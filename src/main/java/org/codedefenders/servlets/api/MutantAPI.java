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
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.http.HttpStatus;
import org.codedefenders.auth.CodeDefendersAuth;
import org.codedefenders.configuration.Configuration;
import org.codedefenders.database.EventDAO;
import org.codedefenders.database.GameDAO;
import org.codedefenders.database.IntentionDAO;
import org.codedefenders.database.PlayerDAO;
import org.codedefenders.database.TargetExecutionDAO;
import org.codedefenders.dto.MutantDTO;
import org.codedefenders.dto.SimpleUser;
import org.codedefenders.dto.api.MutantUpload;
import org.codedefenders.execution.CompileException;
import org.codedefenders.execution.IMutationTester;
import org.codedefenders.execution.TargetExecution;
import org.codedefenders.game.AbstractGame;
import org.codedefenders.game.GameState;
import org.codedefenders.game.Mutant;
import org.codedefenders.game.Role;
import org.codedefenders.game.multiplayer.MeleeGame;
import org.codedefenders.game.multiplayer.MultiplayerGame;
import org.codedefenders.model.AttackerIntention;
import org.codedefenders.model.Event;
import org.codedefenders.model.EventStatus;
import org.codedefenders.model.EventType;
import org.codedefenders.service.game.GameService;
import org.codedefenders.servlets.games.GameManagingUtils;
import org.codedefenders.servlets.games.battleground.MultiplayerGameManager;
import org.codedefenders.servlets.games.melee.MeleeGameManager;
import org.codedefenders.servlets.util.ServletUtils;
import org.codedefenders.servlets.util.api.Utils;
import org.codedefenders.validation.code.CodeValidator;
import org.codedefenders.validation.code.CodeValidatorLevel;
import org.codedefenders.validation.code.ValidationMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;

import static org.codedefenders.execution.TargetExecution.Target.COMPILE_MUTANT;
import static org.codedefenders.util.Constants.ATTACKER_HAS_PENDING_DUELS;
import static org.codedefenders.util.Constants.GRACE_PERIOD_MESSAGE;
import static org.codedefenders.util.Constants.MODE_BATTLEGROUND_DIR;
import static org.codedefenders.util.Constants.MUTANT_CREATION_ERROR_MESSAGE;
import static org.codedefenders.util.Constants.MUTANT_DUPLICATED_MESSAGE;
import static org.codedefenders.util.Constants.MUTANT_UNCOMPILABLE_MESSAGE;

/**
 * This {@link HttpServlet} offers an API for {@link Mutant mutants}.
 *
 * <p>A {@code GET} request with the {@code mutantId} parameter results in a JSON string containing
 * mutant information, including the source code diff.
 *
 * <p>Serves on path: {@code /api/mutant}.
 *
 * @author <a href="https://github.com/werli">Phil Werli</a>
 */
@WebServlet({org.codedefenders.util.Paths.API_MUTANT,"/api/game/mutant"})
public class MutantAPI extends HttpServlet {

    private static final Logger logger = LoggerFactory.getLogger(MutantAPI.class);
    @Inject
    CodeDefendersAuth login;
    @Inject
    GameService gameService;
    @Inject
    GameManagingUtils gameManagingUtils;
    @Inject
    IntentionDAO intentionDAO;
    @Inject
    MeleeGameManager meleeGameManager;
    @Inject
    MultiplayerGameManager multiplayerGameManager;
    @Inject
    EventDAO eventDAO;
    @Inject
    IMutationTester mutationTester;
    @Inject
    Configuration config;

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        final Optional<MutantDTO> mutant = ServletUtils.getIntParameter(request, "mutantId").map(id -> gameService.getMutant(login.getUserId(), id));

        if (!mutant.isPresent()) {
            response.setStatus(HttpStatus.SC_BAD_REQUEST);
            return;
        }

        PrintWriter out = response.getWriter();

        String json = generateJsonForMutant(mutant.get());

        response.setContentType("application/json");
        out.print(json);
        out.flush();
    }

    private String generateJsonForMutant(MutantDTO mutant) {
        Gson gson = new Gson();

        JsonObject root = new JsonObject();
        root.add("id", gson.toJsonTree(mutant.getId(), Integer.class));
        root.add("playerId", gson.toJsonTree(mutant.getPlayerId(), Integer.class));
        root.add("gameId", gson.toJsonTree(mutant.getGameId(), Integer.class));
        root.add("diff", gson.toJsonTree(mutant.getPatchString(), String.class));

        return gson.toJson(root);

    }

    @Override
    public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        final MutantUpload mutant;
        try {
            mutant = (MutantUpload) Utils.parsePostOrRespondJsonError(request, response, MutantUpload.class);
        } catch (JsonParseException e) {
            return;
        }
        final Integer gameId = mutant.getGameId();
        AbstractGame abstractGame = GameDAO.getGame(gameId);
        if (abstractGame == null) {
            Utils.respondJsonError(response, "Game with ID " + gameId + " not found", HttpServletResponse.SC_NOT_FOUND);
        } else {
            int playerId = PlayerDAO.getPlayerIdForUserAndGame(login.getUserId(), gameId);
            if (playerId == -1) {
                Utils.respondJsonError(response, "You are not part of this game");
            } else if (abstractGame.isCapturePlayersIntention() && mutant.getAttackerIntention() == null) {
                Utils.respondJsonError(response, "This game requires player intentions. You cannot submit a mutant without specifying your intention");
            } else {
                try {
                    int generatedId;
                    if (abstractGame instanceof MultiplayerGame || abstractGame instanceof MeleeGame) {
                        generatedId = createMutant(abstractGame, mutant.getSource(), login.getSimpleUser(), playerId, mutant.getAttackerIntention());
                        if (abstractGame instanceof MultiplayerGame) {
                            multiplayerGameManager.triggerAutomaticMutantEquivalenceForGame((MultiplayerGame) abstractGame);
                        } else {
                            meleeGameManager.triggerAutomaticMutantEquivalenceForGame((MeleeGame) abstractGame);
                        }
                    } else {
                        throw new CompileException("Specified game is neither battleground nor melee");
                    }
                    Gson gson = new Gson();
                    PrintWriter out = response.getWriter();
                    response.setContentType("application/json");
                    JsonObject root = new JsonObject();
                    root.add("id", gson.toJsonTree(generatedId, Integer.class));
                    out.print(gson.toJson(root));
                    out.flush();
                } catch (CompileException e) {
                    Utils.respondJsonError(response, e.getMessage());
                }
            }
        }
    }

    private int createMutant(AbstractGame game, String mutantText, SimpleUser user, int playerId, AttackerIntention intention) throws IOException, CompileException {
        boolean isMultiplayer = game instanceof MultiplayerGame;
        if (isMultiplayer && ((MultiplayerGame) game).getRole(login.getUserId()) != Role.ATTACKER) {
            throw new CompileException("You can only submit tests if you are an Attacker");
        }
        if (game.getState() != GameState.ACTIVE) {
            throw new CompileException(GRACE_PERIOD_MESSAGE);
        }
        if (gameManagingUtils.hasAttackerPendingMutantsInGame(game.getId(), playerId) && config.isBlockAttacker()) {
            throw new CompileException(ATTACKER_HAS_PENDING_DUELS);
        }
        CodeValidatorLevel codeValidatorLevel = game.getMutantValidatorLevel();
        ValidationMessage validationMessage = CodeValidator.validateMutantGetMessage(game.getCUT().getSourceCode(), mutantText, codeValidatorLevel);
        boolean validationSuccess = validationMessage == ValidationMessage.MUTANT_VALIDATION_SUCCESS;
        if (!validationSuccess) {
            throw new CompileException(validationMessage.get());
        }
        Mutant existingMutant = gameManagingUtils.existingMutant(game.getId(), mutantText);
        boolean duplicateCheckSuccess = existingMutant == null;
        if (!duplicateCheckSuccess) {
            List<String> messages = new ArrayList<>();
            messages.add(MUTANT_DUPLICATED_MESSAGE);
            TargetExecution existingMutantTarget = TargetExecutionDAO.getTargetExecutionForMutant(existingMutant, COMPILE_MUTANT);
            if (existingMutantTarget != null && existingMutantTarget.status != TargetExecution.Status.SUCCESS && existingMutantTarget.message != null && !existingMutantTarget.message.isEmpty()) {
                messages.add(existingMutantTarget.message);
            }
            throw new CompileException(String.join("\n", messages));
        }
        Mutant newMutant = gameManagingUtils.createMutant(game.getId(), game.getClassId(), mutantText, user.getId(), MODE_BATTLEGROUND_DIR);
        if (newMutant == null) {
            logger.debug("Error creating mutant. Game: {}, Class: {}, User: {}, Mutant: {}", game.getId(), game.getClassId(), user.getId(), mutantText);
            throw new CompileException(MUTANT_CREATION_ERROR_MESSAGE);
        }
        TargetExecution compileMutantTarget = TargetExecutionDAO.getTargetExecutionForMutant(newMutant, COMPILE_MUTANT);
        if (compileMutantTarget == null || compileMutantTarget.status != TargetExecution.Status.SUCCESS) {
            List<String> messages = new ArrayList<>();
            messages.add(MUTANT_UNCOMPILABLE_MESSAGE);
            if (compileMutantTarget != null && compileMutantTarget.message != null && !compileMutantTarget.message.isEmpty()) {
                messages.add(compileMutantTarget.message);

            }
            throw new CompileException(String.join("\n", messages));
        }
        final String notificationMsg = user.getName() + " created a mutant.";
        if (isMultiplayer) {
            Event notif = new Event(-1, game.getId(), user.getId(), notificationMsg, EventType.ATTACKER_MUTANT_CREATED, EventStatus.GAME,
                    new Timestamp(System.currentTimeMillis() - 1000));
            eventDAO.insert(notif);
            mutationTester.runAllTestsOnMutant((MultiplayerGame) game, newMutant);

        } else {
            Event notif = new Event(-1, game.getId(), user.getId(), notificationMsg, EventType.PLAYER_MUTANT_CREATED, EventStatus.GAME,
                    new Timestamp(System.currentTimeMillis() - 1000));
            eventDAO.insert(notif);
            mutationTester.runAllTestsOnMeleeMutant((MeleeGame) game, newMutant);
        }
        game.update();
        if (game.isCapturePlayersIntention()) {
            collectAttackerIntentions(newMutant, intention);
        }
        logger.info("Successfully created mutant {} ", newMutant.getId());
        return newMutant.getId();
    }

    private void collectAttackerIntentions(Mutant newMutant, AttackerIntention intention) {
        try {
            intentionDAO.storeIntentionForMutant(newMutant, intention);
        } catch (Exception e) {
            logger.error("Cannot store intention to database.", e);
        }
    }
}