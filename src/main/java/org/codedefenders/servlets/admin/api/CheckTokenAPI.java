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
import java.io.PrintWriter;
import java.util.Optional;

import javax.inject.Inject;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.codedefenders.auth.CodeDefendersAuth;
import org.codedefenders.dto.api.TokenPost;
import org.codedefenders.game.Test;
import org.codedefenders.model.UserEntity;
import org.codedefenders.persistence.database.SettingsRepository;
import org.codedefenders.persistence.database.UserRepository;
import org.codedefenders.service.UserService;
import org.codedefenders.service.game.GameService;
import org.codedefenders.servlets.util.APIUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
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
@WebServlet("/admin/api/auth/checkToken")
public class CheckTokenAPI extends HttpServlet {

    private static final Logger logger = LoggerFactory.getLogger(CheckTokenAPI.class);
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

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        final TokenPost tokenPost;
        try {
            tokenPost = (TokenPost) APIUtils.parsePostOrRespondJsonError(request, response, TokenPost.class);
        } catch (JsonParseException e) {
            return;
        }
        String token = tokenPost.getToken();

        Optional<UserEntity> activeUser = userRepository.getUserByToken(token);

        if (!activeUser.isPresent()) {
            APIUtils.respondJsonError(response, "Invalid token", HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        if (settingsRepository.isMailValidationRequired() && !activeUser.get().isValidated()) {
            APIUtils.respondJsonError(response, "Account email is not validated.", HttpServletResponse.SC_FORBIDDEN);
            return;
        }
        if (!activeUser.get().isActive()) {
            APIUtils.respondJsonError(response, "Your account is inactive, login is only possible with an active account.", HttpServletResponse.SC_FORBIDDEN);
            return;
        }
        UserEntity user = activeUser.get();
        PrintWriter out = response.getWriter();
        response.setContentType("application/json");
        Gson gson = new Gson();
        JsonObject root = new JsonObject();
        root.add("userId", gson.toJsonTree(user.getId(), Integer.class));
        root.add("username", gson.toJsonTree(user.getUsername(), String.class));
        root.add("token", gson.toJsonTree(user.getToken(), String.class));
        out.print(new Gson().toJson(root));
        out.flush();
    }
}
