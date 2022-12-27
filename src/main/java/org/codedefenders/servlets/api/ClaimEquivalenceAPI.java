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
import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import javax.inject.Inject;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.codedefenders.auth.CodeDefendersAuth;
import org.codedefenders.database.EventDAO;
import org.codedefenders.database.GameDAO;
import org.codedefenders.database.MutantDAO;
import org.codedefenders.database.PlayerDAO;
import org.codedefenders.dto.SimpleUser;
import org.codedefenders.dto.api.GameIDAndLines;
import org.codedefenders.execution.CompileException;
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
import org.codedefenders.model.Player;
import org.codedefenders.service.UserService;
import org.codedefenders.servlets.util.api.Utils;
import org.codedefenders.util.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonParseException;

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
@WebServlet("/api/game/mutant/equivalences/claim")
public class ClaimEquivalenceAPI extends HttpServlet {

    private static final Logger logger = LoggerFactory.getLogger(ClaimEquivalenceAPI.class);
    @Inject
    CodeDefendersAuth login;
    @Inject
    EventDAO eventDAO;
    @Inject
    UserService userService;

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        final GameIDAndLines gameIDAndLines;
        try {
            gameIDAndLines = (GameIDAndLines) Utils.parsePostOrRespondJsonError(request, response, GameIDAndLines.class);
        } catch (JsonParseException e) {
            return;
        }
        int gameId = gameIDAndLines.getGameId();
        List<Integer> lines = gameIDAndLines.getLines();
        AbstractGame abstractGame = GameDAO.getGame(gameId);
        int playerId = PlayerDAO.getPlayerIdForUserAndGame(login.getUserId(), gameId);
        if (abstractGame == null) {
            Utils.respondJsonError(response, "Game with ID " + gameId + " not found", HttpServletResponse.SC_NOT_FOUND);
        } else if (playerId == -1) {
            Utils.respondJsonError(response, "You are not part of this game");
        } else if (!(abstractGame instanceof MeleeGame) && !(abstractGame instanceof MultiplayerGame)) {
            Utils.respondJsonError(response, "Specified game is neither battleground nor melee");
        } else if (abstractGame instanceof MultiplayerGame && ((MultiplayerGame) abstractGame).getRole(login.getUserId()) != Role.DEFENDER) {
            Utils.respondJsonError(response, "You are not a Defender");
        } else if (lines == null || lines.isEmpty()) {
            Utils.respondJsonError(response, "specify at least one line to claim");
        } else {
            try {
                claimEquivalent(abstractGame, lines, playerId, login.getSimpleUser());
            } catch (CompileException e) {
                Utils.respondJsonError(response, e.getMessage());
            }
        }
    }

    private void claimEquivalent(AbstractGame game, List<Integer> equivLines, int playerId, SimpleUser user) throws CompileException {
        boolean isMultiplayer = game instanceof MultiplayerGame;
        if (game.getState() != GameState.ACTIVE && game.getState() != GameState.GRACE_ONE) {
            logger.info("Mutant claimed for non-active game.");
            throw new CompileException("You cannot claim mutants as equivalent in this game anymore.");
        }
        AtomicInteger claimedMutants = new AtomicInteger();
        AtomicBoolean noneCovered = new AtomicBoolean(true);
        List<Mutant> mutantsAlive = game.getAliveMutants();
        equivLines.stream().filter(lineNumber -> isMultiplayer ? ((MultiplayerGame) game).isLineCovered(lineNumber) : ((MeleeGame) game).isLineCovered(lineNumber)).forEach(line -> {
            noneCovered.set(false);
            mutantsAlive.stream().filter(m -> m.getLines().contains(line) && m.getCreatorId() != Constants.DUMMY_ATTACKER_USER_ID && m.getCreatorId() != user.getId()).forEach(m -> {
                m.setEquivalent(Mutant.Equivalence.PENDING_TEST);
                m.update();
                Optional<SimpleUser> mutantOwner = userService.getSimpleUserByPlayerId(m.getPlayerId());
                if (isMultiplayer) {
                    Event event = new Event(-1, game.getId(), mutantOwner.map(SimpleUser::getId).orElse(0), "One or more of your mutants is flagged equivalent.",
                            EventType.DEFENDER_MUTANT_EQUIVALENT, EventStatus.NEW, new Timestamp(System.currentTimeMillis()));
                    eventDAO.insert(event);
                } else {
                    Event event = new Event(-1, game.getId(), mutantOwner.map(SimpleUser::getId).orElse(0), "One or more of your mutants is flagged equivalent.",
                            EventType.PLAYER_MUTANT_EQUIVALENT, EventStatus.NEW, new Timestamp(System.currentTimeMillis()));
                    eventDAO.insert(event);
                    Player claimingPlayer = PlayerDAO.getPlayerForUserAndGame(user.getId(), game.getId());
                    Event scoreEvent = new Event(-1, game.getId(), Constants.DUMMY_CREATOR_USER_ID, claimingPlayer.getId() + ":" + m.getId(),
                            EventType.PLAYER_MUTANT_CLAIMED_EQUIVALENT, EventStatus.GAME, new Timestamp(System.currentTimeMillis()));
                    eventDAO.insert(scoreEvent);
                }
                MutantDAO.insertEquivalence(m, playerId);
                claimedMutants.incrementAndGet();
            });
        });
        if (noneCovered.get()) {
            throw new CompileException("You cannot claim equivalence on untested lines");
        }
        int numClaimed = claimedMutants.get();
        if (numClaimed > 0) {
            String flaggingChatMessage = user.getName() + " flagged " + numClaimed + " mutant" + (numClaimed == 1 ? "" : "s") + " " + "as equivalent.";
            Event event = new Event(-1, game.getId(), user.getId(), flaggingChatMessage, isMultiplayer ? EventType.DEFENDER_MUTANT_CLAIMED_EQUIVALENT :
                    EventType.PLAYER_MUTANT_CLAIMED_EQUIVALENT, EventStatus.GAME, new Timestamp(System.currentTimeMillis()));
            eventDAO.insert(event);
        } else {
            throw new CompileException("All mutants on the selected lines have already been claimed as equivalent or killed");
        }
    }
}
