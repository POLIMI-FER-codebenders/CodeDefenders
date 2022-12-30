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
import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.codedefenders.auth.CodeDefendersAuth;
import org.codedefenders.database.GameDAO;
import org.codedefenders.game.AbstractGame;
import org.codedefenders.game.Role;
import org.codedefenders.game.Test;
import org.codedefenders.servlets.util.api.Utils;
import org.springframework.core.env.MissingRequiredPropertiesException;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

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
@WebServlet("/api/game/role")
public class GameRoleAPI extends HttpServlet {

    final Map<String, Class<?>> parameterTypes = new HashMap<String, Class<?>>() {
        {
            put("gameId", Integer.class);
        }
    };
    @Inject
    CodeDefendersAuth login;

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

        final Map<String, Object> params;
        try {
            params = Utils.getParametersOrRespondJsonError(request, response, parameterTypes);
        } catch (MissingRequiredPropertiesException e) {
            return;
        }
        final Integer gameId = (Integer) params.get("gameId");
        AbstractGame game = GameDAO.getGame(gameId);
        if (game == null) {
            Utils.respondJsonError(response, "Game with ID " + gameId + " not found", HttpServletResponse.SC_NOT_FOUND);
        } else {
            Role role = GameDAO.getRole(login.getUserId(), gameId);
            PrintWriter out = response.getWriter();
            response.setContentType("application/json");
            Gson gson = new Gson();
            JsonObject root = new JsonObject();
            root.add("role", gson.toJsonTree(role));
            out.print(new Gson().toJson(root));
            out.flush();
        }
    }
}
