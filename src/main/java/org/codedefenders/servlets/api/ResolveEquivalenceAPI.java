/*
 * Copyright (C) 2016-2019 Code Defenders contributors
 *
 * This file is part of Code Defenders.
 *
 * Code Defenders is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or (at
 * your option) any later version.
 *
 * Code Defenders is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Code Defenders. If not, see <http://www.gnu.org/licenses/>.
 */
package org.codedefenders.servlets.api;

import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import javax.inject.Inject;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.codedefenders.auth.CodeDefendersAuth;
import org.codedefenders.database.EventDAO;
import org.codedefenders.database.GameDAO;
import org.codedefenders.database.MutantDAO;
import org.codedefenders.database.PlayerDAO;
import org.codedefenders.database.TargetExecutionDAO;
import org.codedefenders.dto.SimpleUser;
import org.codedefenders.dto.api.ClaimResolution;
import org.codedefenders.execution.CompileException;
import org.codedefenders.execution.IMutationTester;
import org.codedefenders.execution.TargetExecution;
import org.codedefenders.game.AbstractGame;
import org.codedefenders.game.GameState;
import org.codedefenders.game.Mutant;
import org.codedefenders.game.Role;
import org.codedefenders.game.Test;
import org.codedefenders.game.multiplayer.MeleeGame;
import org.codedefenders.game.multiplayer.MultiplayerGame;
import org.codedefenders.model.Event;
import org.codedefenders.model.EventStatus;
import org.codedefenders.model.EventType;
import org.codedefenders.persistence.database.UserRepository;
import org.codedefenders.service.UserService;
import org.codedefenders.servlets.games.GameManagingUtils;
import org.codedefenders.servlets.games.battleground.MultiplayerGameManager;
import org.codedefenders.servlets.util.api.Utils;
import org.codedefenders.util.Constants;
import org.codedefenders.validation.code.CodeValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.JsonParseException;

import static org.codedefenders.execution.TargetExecution.Target.COMPILE_TEST;
import static org.codedefenders.execution.TargetExecution.Target.TEST_ORIGINAL;
import static org.codedefenders.game.Mutant.Equivalence.ASSUMED_YES;
import static org.codedefenders.util.Constants.MODE_BATTLEGROUND_DIR;
import static org.codedefenders.util.Constants.TEST_DID_NOT_COMPILE_MESSAGE;
import static org.codedefenders.util.Constants.TEST_DID_NOT_PASS_ON_CUT_MESSAGE;
import static org.codedefenders.util.Constants.TEST_GENERIC_ERROR_MESSAGE;

/**
 * This {@link HttpServlet} offers an API for {@link Test tests}.
 *
 * <p>A {@code GET} request with the {@code testId} parameter results in a JSON string containing
 * test information, including the source code.
 *
 * <p>Serves on path: {@code /api/test}.
 *
 * @author <a href="https://github.com/werli">Phil Werli</a>
 */
@WebServlet("/api/game/mutant/equivalences/resolve")
public class ResolveEquivalenceAPI extends HttpServlet {
    private static final Logger logger = LoggerFactory.getLogger(ResolveEquivalenceAPI.class);
    @Inject
    CodeDefendersAuth login;
    @Inject
    GameManagingUtils gameManagingUtils;
    @Inject
    MultiplayerGameManager multiplayerGameManager;
    @Inject
    EventDAO eventDAO;
    @Inject
    IMutationTester mutationTester;
    @Inject
    UserService userService;
    @Inject
    UserRepository userRepo;

    @Override
    public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        final ClaimResolution resolution;
        try {
            resolution = (ClaimResolution) Utils.parsePostOrRespondJsonError(request, response, ClaimResolution.class);
        } catch (JsonParseException e) {
            return;
        }
        final Integer mutantId = resolution.getMutantId();
        Mutant mutant = MutantDAO.getMutantById(mutantId);
        if (mutant == null) {
            Utils.respondJsonError(response, "Mutant with ID " + mutantId + " not found", HttpServletResponse.SC_NOT_FOUND);
        } else {
            int gameId = mutant.getGameId();
            AbstractGame abstractGame = GameDAO.getGame(gameId);
            int playerId = PlayerDAO.getPlayerIdForUserAndGame(login.getUserId(), gameId);
            if (playerId == -1) {
                Utils.respondJsonError(response, "You are not part of this game");
            } else if (!(abstractGame instanceof MeleeGame) && !(abstractGame instanceof MultiplayerGame)) {
                Utils.respondJsonError(response, "Specified game is neither battleground nor melee");
            } else if (abstractGame instanceof MultiplayerGame && ((MultiplayerGame) abstractGame).getRole(login.getUserId()) != Role.ATTACKER) {
                Utils.respondJsonError(response, "You are not an Attacker");
            } else if (mutant.getState() != Mutant.State.FLAGGED) {
                //This doesn't happen in the frontend but it should, otherwise an attacker can keep spamming tests on a dead mutant and kill other claimed mutants without the risk of losing
                logger.info("User {} tried to accept equivalence for mutant {}, but mutant has no pending equivalences.", login.getSimpleUser().getId(), mutantId);
                Utils.respondJsonError(response, "Mutant has no pending equivalences");
            } else if (!resolution.isAccepted() && (resolution.getSource() == null || resolution.getSource().isEmpty())) {
                Utils.respondJsonError(response, "You have to provide a killing test if you want to counter a claim");
            } else {
                try {
                    ResolutionResult resolutionResult;
                    resolutionResult = resolveEquivalence(abstractGame, mutantId, resolution.isAccepted(), resolution.getSource(), login.getSimpleUser(), playerId);
                    if (resolutionResult != null) {
                        Gson gson = new Gson();
                        PrintWriter out = response.getWriter();
                        response.setContentType("application/json");
                        out.print(gson.toJsonTree(resolutionResult));
                        out.flush();
                    }
                } catch (CompileException e) {
                    Utils.respondJsonError(response, e.getMessage());
                }
            }
        }
    }

    private ResolutionResult resolveEquivalence(AbstractGame game, int mutantId, boolean accept, String testText, SimpleUser user, int playerId) throws CompileException {
        boolean isMultiplayer = game instanceof MultiplayerGame;
        if (game.getState() == GameState.FINISHED) {
            throw new CompileException("Game has finished.");
        }
        if (accept) {
            List<Mutant> mutantsPending = game.getMutantsMarkedEquivalentPending();
            for (Mutant m : mutantsPending) {
                if (m.getId() == mutantId && m.getPlayerId() == playerId) {
                    if (isMultiplayer) {
                        boolean isMutantKillable = multiplayerGameManager.isMutantKillableByOtherTests(m);
                        String notification = String.format("%s accepts that their mutant %d is equivalent", user.getName(), m.getId());
                        if (isMutantKillable) {
                            logger.warn("Mutant {} was accepted as equivalence but it is killable", m);
                            notification = notification + " " + " However, the mutant was killable!";
                        }
                        m.kill(Mutant.Equivalence.DECLARED_YES);
                        PlayerDAO.increasePlayerPoints(1, MutantDAO.getEquivalentDefenderId(m));
                        Event notifEquiv = new Event(-1, game.getId(), user.getId(), notification, EventType.DEFENDER_MUTANT_EQUIVALENT, EventStatus.GAME,
                                new Timestamp(System.currentTimeMillis()));
                        eventDAO.insert(notifEquiv);
                        if (isMutantKillable) {
                            int defenderId = MutantDAO.getEquivalentDefenderId(m);
                            Optional<Integer> userId = userRepo.getUserIdForPlayerId(defenderId);
                            notification = user.getName() + " accepts that the mutant " + m.getId() + "that you claimed equivalent is equivalent, but that mutant " + "was " +
                                    "killable.";
                            Event notifDefenderEquiv = new Event(-1, game.getId(), userId.orElse(0), notification, EventType.GAME_MESSAGE_DEFENDER, EventStatus.GAME,
                                    new Timestamp(System.currentTimeMillis()));
                            eventDAO.insert(notifDefenderEquiv);
                        }
                        return null;
                    } else {
                        m.kill(Mutant.Equivalence.DECLARED_YES);
                        Optional<SimpleUser> eventUser = userService.getSimpleUserById(user.getId());
                        Event notifEquiv = new Event(-1, game.getId(), user.getId(), eventUser.map(SimpleUser::getName).orElse("") + " accepts that their mutant " + m.getId() +
                                " is equivalent.", EventType.PLAYER_LOST_EQUIVALENT_DUEL, EventStatus.GAME, new Timestamp(System.currentTimeMillis()));
                        eventDAO.insert(notifEquiv);
                        Event scoreEvent = new Event(-1, game.getId(), Constants.DUMMY_CREATOR_USER_ID, "-1" + ":" + m.getId(), EventType.PLAYER_LOST_EQUIVALENT_DUEL,
                                EventStatus.GAME, new Timestamp(System.currentTimeMillis()));
                        eventDAO.insert(scoreEvent);
                        return null;
                    }
                }
            }
            logger.info("User {} tried to accept equivalence for mutant {}, but mutant has no pending equivalences.", user.getId(), mutantId);
            throw new CompileException("Mutant has no pending equivalences");
        } else {
            List<String> validationMessage = CodeValidator.validateTestCodeGetMessage(testText, game.getMaxAssertionsPerTest(), game.getCUT().getAssertionLibrary());
            boolean validationSuccess = validationMessage.isEmpty();
            if (!validationSuccess) {
                throw new CompileException(String.join("\n", validationMessage));
            }
            Test newTest;
            try {
                newTest = gameManagingUtils.createTest(game.getId(), game.getClassId(), testText, user.getId(), MODE_BATTLEGROUND_DIR);
            } catch (IOException io) {
                throw new CompileException(TEST_GENERIC_ERROR_MESSAGE);
            }
            logger.debug("Executing Action resolveEquivalence for mutant {} and test {}", mutantId, newTest.getId());
            TargetExecution compileTestTarget = TargetExecutionDAO.getTargetExecutionForTest(newTest, COMPILE_TEST);
            if (compileTestTarget == null || compileTestTarget.status != TargetExecution.Status.SUCCESS) {
                logger.debug("compileTestTarget: " + compileTestTarget);
                List<String> messages = new ArrayList<>();
                messages.add(TEST_DID_NOT_COMPILE_MESSAGE);
                if (compileTestTarget != null) {
                    messages.add(compileTestTarget.message);
                }
                throw new CompileException(String.join("\n", messages));
            }
            TargetExecution testOriginalTarget = TargetExecutionDAO.getTargetExecutionForTest(newTest, TEST_ORIGINAL);
            if (testOriginalTarget.status != TargetExecution.Status.SUCCESS) {
                List<String> messages = new ArrayList<>();
                logger.debug("testOriginalTarget: " + testOriginalTarget);
                messages.add(TEST_DID_NOT_PASS_ON_CUT_MESSAGE);
                messages.add(testOriginalTarget.message);
                throw new CompileException(String.join("\n", messages));
            }
            logger.debug("Test {} passed on the CUT", newTest.getId());
            List<Mutant> mutantsPendingTests = game.getMutantsMarkedEquivalentPending();
            boolean killedClaimed = false;
            int killedOthers = 0;
            boolean isMutantKillable;
            if (isMultiplayer) {
                for (Mutant mutPending : mutantsPendingTests) {
                    mutationTester.runEquivalenceTest(newTest, mutPending);
                    if (mutPending.getEquivalent() == Mutant.Equivalence.PROVEN_NO) {
                        logger.debug("Test {} killed mutant {} and proved it non-equivalent", newTest.getId(), mutPending.getId());
                        final String message = user.getName() + " killed mutant " + mutPending.getId() + " in an equivalence duel.";
                        Event notif = new Event(-1, game.getId(), user.getId(), message, EventType.ATTACKER_MUTANT_KILLED_EQUIVALENT, EventStatus.GAME,
                                new Timestamp(System.currentTimeMillis()));
                        eventDAO.insert(notif);
                        if (mutPending.getId() == mutantId) {
                            killedClaimed = true;
                        } else {
                            killedOthers++;
                        }
                    } else {
                        if (mutPending.getId() == mutantId) {
                            isMutantKillable = multiplayerGameManager.isMutantKillableByOtherTests(mutPending);
                            String notification = user.getName() + " lost an equivalence duel. Mutant " + mutPending.getId() + " is assumed equivalent.";
                            if (isMutantKillable) {
                                notification = notification + " " + "However, the mutant was killable!";
                            }
                            mutPending.kill(ASSUMED_YES);
                            Event notif = new Event(-1, game.getId(), user.getId(), notification, EventType.DEFENDER_MUTANT_EQUIVALENT, EventStatus.GAME,
                                    new Timestamp(System.currentTimeMillis()));
                            eventDAO.insert(notif);
                        }
                        logger.debug("Test {} failed to kill mutant {}, hence mutant is assumed equivalent", newTest.getId(), mutPending.getId());
                    }
                }
            } else {
                for (Mutant pendingMutant : mutantsPendingTests) {
                    //This behaves differently from the multiplayer case, I'm deleting this for consistency
                    /*if (pendingMutant.getId() != mutantId) {
                        logger.info("Skip pending mutant {} as it is not the one to deal with in this request ({})", pendingMutant, mutantId);
                        continue;
                    }*/
                    mutationTester.runEquivalenceTest(newTest, pendingMutant);
                    if (pendingMutant.getEquivalent() == Mutant.Equivalence.PROVEN_NO) {
                        logger.debug("Test {} killed mutant {} and proved it non-equivalent", newTest.getId(), pendingMutant.getId());
                        newTest.updateScore(0);
                        final String message =
                                userService.getSimpleUserById(user.getId()).map(SimpleUser::getName).orElse("") + " killed mutant " + pendingMutant.getId() + " " + "in an " +
                                        "equivalence duel.";
                        Event notif = new Event(-1, game.getId(), user.getId(), message, EventType.PLAYER_WON_EQUIVALENT_DUEL, EventStatus.GAME,
                                new Timestamp(System.currentTimeMillis()));
                        eventDAO.insert(notif);
                        Event scoreEvent = new Event(-1, game.getId(), Constants.DUMMY_CREATOR_USER_ID, newTest.getId() + ":" + pendingMutant.getId(),
                                EventType.PLAYER_WON_EQUIVALENT_DUEL, EventStatus.GAME, new Timestamp(System.currentTimeMillis()));
                        eventDAO.insert(scoreEvent);
                        if (pendingMutant.getId() == mutantId) {
                            killedClaimed = true;
                        } else {
                            killedOthers++;
                        }
                    } else {
                        if (pendingMutant.getId() == mutantId) {
                            logger.debug("Test {} did not kill mutant {} and so did not prov it non-equivalent", newTest.getId(), pendingMutant.getId());
                            pendingMutant.kill(ASSUMED_YES);
                            final String message =
                                    userService.getSimpleUserById(user.getId()).map(SimpleUser::getName).orElse("") + " lost an equivalence duel. Mutant " + pendingMutant.getId() + " is assumed equivalent.";
                            Event notif = new Event(-1, game.getId(), user.getId(), message, EventType.PLAYER_LOST_EQUIVALENT_DUEL, EventStatus.GAME,
                                    new Timestamp(System.currentTimeMillis()));
                            eventDAO.insert(notif);
                            Event scoreEvent = new Event(-1, game.getId(), Constants.DUMMY_CREATOR_USER_ID, newTest.getId() + ":" + pendingMutant.getId(),
                                    EventType.PLAYER_LOST_EQUIVALENT_DUEL, EventStatus.GAME, new Timestamp(System.currentTimeMillis()));
                            eventDAO.insert(scoreEvent);
                        }
                        logger.debug("Test {} failed to kill mutant {}, hence mutant is assumed equivalent", newTest.getId(), pendingMutant.getId());
                    }
                }
            }
            ResolutionResult resolutionResult = new ResolutionResult(killedClaimed, killedOthers);
            newTest.update();
            game.update();
            logger.info("Resolving equivalence was handled successfully");
            return resolutionResult;
        }
    }

    private class ResolutionResult {
        private final boolean killedClaimed;
        private final int killedOthers;

        public ResolutionResult(boolean killedClaimed, int killedOthers) {
            this.killedClaimed = killedClaimed;
            this.killedOthers = killedOthers;
        }
    }
}
