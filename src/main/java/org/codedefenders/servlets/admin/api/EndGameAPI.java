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
package org.codedefenders.servlets.admin.api;

import java.io.IOException;
import java.sql.Timestamp;

import javax.inject.Inject;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.codedefenders.auth.CodeDefendersAuth;
import org.codedefenders.beans.admin.AdminCreateGamesBean;
import org.codedefenders.database.EventDAO;
import org.codedefenders.database.GameDAO;
import org.codedefenders.database.KillmapDAO;
import org.codedefenders.dto.api.GameID;
import org.codedefenders.execution.KillMap;
import org.codedefenders.execution.KillMapProcessor;
import org.codedefenders.game.AbstractGame;
import org.codedefenders.game.GameState;
import org.codedefenders.game.Test;
import org.codedefenders.model.Event;
import org.codedefenders.model.EventStatus;
import org.codedefenders.model.EventType;
import org.codedefenders.notification.INotificationService;
import org.codedefenders.notification.events.server.game.GameStoppedEvent;
import org.codedefenders.persistence.database.SettingsRepository;
import org.codedefenders.persistence.database.UserRepository;
import org.codedefenders.service.UserService;
import org.codedefenders.service.game.GameService;
import org.codedefenders.servlets.util.api.Utils;
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
@WebServlet("/admin/api/game/end")
public class EndGameAPI extends HttpServlet {

    private static final Logger logger = LoggerFactory.getLogger(EndGameAPI.class);
    @Inject
    CodeDefendersAuth login;
    @Inject
    GameService gameService;
    @Inject
    SettingsRepository settingsRepository;
    @Inject
    UserRepository userRepository;
    @Inject
    UserService userService;
    @Inject
    AdminCreateGamesBean adminCreateGamesBean;
    @Inject
    EventDAO eventDAO;
    @Inject
    private INotificationService notificationService;

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        final GameID gameId;
        try {
            gameId = (GameID) Utils.parsePostOrRespondJsonError(request, response, GameID.class);
        } catch (JsonParseException e) {
            return;
        }
        AbstractGame game = GameDAO.getGame(gameId.getGameId());
        if (game == null) {
            Utils.respondJsonError(response, "Game with ID " + gameId.getGameId() + " not found", HttpServletResponse.SC_NOT_FOUND);
        } else if (login.getUserId() != game.getCreatorId()) {
            Utils.respondJsonError(response, "Only the game's creator can end the game", HttpServletResponse.SC_BAD_REQUEST);
        } else if (game.getState() != GameState.ACTIVE && game.getState() != GameState.GRACE_ONE && game.getState() != GameState.GRACE_TWO) {
            Utils.respondJsonError(response, "Game cannot be ended since it has state " + game.getState(), HttpServletResponse.SC_BAD_REQUEST);
        } else {
            logger.info("Ending game {} (Setting state to FINISHED)", gameId);
            game.setState(GameState.FINISHED);
            eventDAO.insert(new Event(-1, game.getId(), login.getUserId(), "", EventType.GAME_FINISHED, EventStatus.GAME, new Timestamp(System.currentTimeMillis())));
            if (game.update()) {
                KillmapDAO.enqueueJob(new KillMapProcessor.KillMapJob(KillMap.KillMapType.GAME, gameId.getGameId()));
            }
            GameStoppedEvent gse = new GameStoppedEvent();
            gse.setGameId(game.getId());
            notificationService.post(gse);
        }
    }
}
