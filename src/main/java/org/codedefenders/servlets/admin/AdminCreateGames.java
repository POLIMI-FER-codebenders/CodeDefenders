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
package org.codedefenders.servlets.admin;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.codedefenders.beans.admin.AdminCreateGamesBean;
import org.codedefenders.beans.admin.AdminCreateGamesBean.RoleAssignmentMethod;
import org.codedefenders.beans.admin.AdminCreateGamesBean.TeamAssignmentMethod;
import org.codedefenders.beans.admin.StagedGameList;
import org.codedefenders.beans.admin.StagedGameList.GameSettings;
import org.codedefenders.beans.admin.StagedGameList.StagedGame;
import org.codedefenders.beans.message.MessagesBean;
import org.codedefenders.database.GameClassDAO;
import org.codedefenders.database.GameDAO;
import org.codedefenders.game.AbstractGame;
import org.codedefenders.game.GameLevel;
import org.codedefenders.game.Role;
import org.codedefenders.model.UserEntity;
import org.codedefenders.model.UserInfo;
import org.codedefenders.servlets.util.Redirect;
import org.codedefenders.util.Constants;
import org.codedefenders.util.Paths;
import org.codedefenders.validation.code.CodeValidatorLevel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.text.MessageFormat.format;
import static org.codedefenders.beans.admin.StagedGameList.GameSettings.GameType.MELEE;
import static org.codedefenders.servlets.util.ServletUtils.getIntParameter;

/**
 * Handles extraction and verification of parameters for the admin create games page.
 * @see AdminCreateGamesBean
 */
@WebServlet(urlPatterns = {Paths.ADMIN_PAGE, Paths.ADMIN_GAMES})
public class AdminCreateGames extends HttpServlet {
    private static final Logger logger = LoggerFactory.getLogger(AdminCreateGames.class);

    @Inject
    private MessagesBean messages;

    @Inject
    private AdminCreateGamesBean adminCreateGamesBean;

    private StagedGameList stagedGameList;

    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        synchronized (adminCreateGamesBean.getSynchronizer()) {
            adminCreateGamesBean.update();
            stagedGameList = adminCreateGamesBean.getStagedGameList();

            request.getRequestDispatcher(Constants.ADMIN_GAMES_JSP).forward(request, response);
        }
    }

    @Override
    public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        synchronized (adminCreateGamesBean.getSynchronizer()) {
            adminCreateGamesBean.update();
            stagedGameList = adminCreateGamesBean.getStagedGameList();

            final String action = request.getParameter("formType");
            if (action == null) {
                logger.error("Missing parameter: formType.");
                Redirect.redirectBack(request, response);
                return;
            }

            switch (action) {
                case "stageGamesWithUsers":
                    stageGamesWithUsers(request);
                    break;
                case "stageEmptyGames":
                    stageEmptyGames(request);
                    break;
                case "deleteStagedGames":
                    deleteStagedGames(request);
                    break;
                case "createStagedGames":
                    createStagedGames(request);
                    break;
                case "removePlayerFromStagedGame":
                    removePlayerFromStagedGame(request);
                case "removeCreatorFromStagedGame":
                    removeCreatorFromStagedGame(request);
                    break;
                case "switchRole":
                    switchRole(request);
                    break;
                case "switchCreatorRole":
                    switchCreatorRole(request);
                    break;
                case "movePlayerBetweenStagedGames":
                    movePlayerBetweenStagedGames(request);
                    break;
                case "addPlayerToGame":
                    addPlayerToGame(request);
                    break;
                default:
                    logger.error("Unknown form type: {}", action);
                    Redirect.redirectBack(request, response);
                    break;
            }

            response.sendRedirect(request.getContextPath() + Paths.ADMIN_GAMES);
        }
    }

    /**
     * Extract game settings from a request.
     * @param request The HTTP request.
     * @return The extracted game settings.
     */
    private GameSettings extractGameSettings(HttpServletRequest request) {
        GameSettings gameSettings = new GameSettings();
        try {
            gameSettings.setGameType(GameSettings.GameType.valueOf(request.getParameter("gameType")));
            gameSettings.setCut(GameClassDAO.getClassForId(getIntParameter(request, "cut").get()));
            gameSettings.setWithMutants(request.getParameter("withMutants") != null);
            gameSettings.setWithTests(request.getParameter("withTests") != null);

            gameSettings.setMaxAssertionsPerTest(getIntParameter(request, "maxAssertionsPerTest").get());
            gameSettings.setMutantValidatorLevel(
                    CodeValidatorLevel.valueOf(request.getParameter("mutantValidatorLevel")));
            gameSettings.setCreatorRole(Role.valueOf(request.getParameter("creatorRole")));
            gameSettings.setChatEnabled(request.getParameter("chatEnabled") != null);
            gameSettings.setCaptureIntentions(request.getParameter("captureIntentions") != null);
            gameSettings.setEquivalenceThreshold(
                    getIntParameter(request, "automaticEquivalenceTrigger").get());
            gameSettings.setLevel(GameLevel.valueOf(request.getParameter("level")));

            gameSettings.setStartGame(request.getParameter("startGame") != null);
        } catch (NullPointerException | NoSuchElementException e) {
            messages.add("ERROR: Missing game settings parameter.");
            return null;
        } catch (IllegalArgumentException e) {
            messages.add("ERROR: Invalid game settings parameter.");
            return null;
        }
        return gameSettings;
    }

    /**
     * Extract and validate POST parameters and forward them to {@link AdminCreateGamesBean#stageGamesWithUsers(Set,
     * GameSettings, RoleAssignmentMethod, TeamAssignmentMethod, int, int, int) AdminCreateGamesBean#stageGames()}.
     * @param request The HTTP request.
     */
    private void stageGamesWithUsers(HttpServletRequest request) {
        /* Extract user IDs from the table. */
        String userIdsStr = request.getParameter("userIds");
        Set<Integer> userIdsFromTable;
        try {
            userIdsFromTable = Arrays.stream(userIdsStr.split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .map(Integer::valueOf)
                    .collect(Collectors.toSet());
        } catch (NullPointerException e) {
            messages.add("ERROR: Missing parameter: userIds.");
            return;
        } catch (NumberFormatException e) {
            messages.add("ERROR: Invalid parameter: userIds.");
            return;
        }

        /* Extract user names/emails from the text field. */
        String userNamesStr = request.getParameter("userNames");
        Set<String> userNames;
        try {
            userNames = Arrays.stream(userNamesStr.split("\\R"))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .collect(Collectors.toSet());
        } catch (NullPointerException e) {
            messages.add("ERROR: Missing parameter: userNames.");
            return;
        } catch (NumberFormatException e) {
            messages.add("ERROR: Invalid parameter: userNames.");
            return;
        }

        /* Extract game settings. */
        GameSettings gameSettings = extractGameSettings(request);
        if (gameSettings == null) {
            return;
        }

        /* Extract game management settings settings. */
        RoleAssignmentMethod roleAssignmentMethod;
        TeamAssignmentMethod teamAssignmentMethod;
        int attackersPerGame;
        int defendersPerGame;
        int playersPerGame;
        try {
            roleAssignmentMethod = RoleAssignmentMethod.valueOf(request.getParameter("roleAssignmentMethod"));
            teamAssignmentMethod = TeamAssignmentMethod.valueOf(request.getParameter("teamAssignmentMethod"));
            attackersPerGame = getIntParameter(request, "attackersPerGame").get();
            defendersPerGame = getIntParameter(request, "defendersPerGame").get();
            playersPerGame = getIntParameter(request, "playersPerGame").get();
        } catch (NullPointerException | NoSuchElementException e) {
            messages.add("ERROR: Missing game management settings parameter.");
            return;
        } catch (IllegalArgumentException e) {
            messages.add("ERROR: Invalid game management settings parameter.");
            return;
        }

        /* Map given user IDs to users and validate that all exist. */
        Optional<Set<UserInfo>> usersFromTable = adminCreateGamesBean.getUserInfosForIds(userIdsFromTable);
        if (!usersFromTable.isPresent()) {
            return;
        }

        /* Map given user names/emails to users and validate that all exist. */
        Optional<Set<UserInfo>> usersFromTextArea = adminCreateGamesBean.getUserInfosForNamesAndEmails(userNames);
        if (!usersFromTextArea.isPresent()) {
            return;
        }

        Set<UserInfo> users = new HashSet<>();
        users.addAll(usersFromTable.get());
        users.addAll(usersFromTextArea.get());

        /* Validate that no users are already assigned to other staged games. */
        Set<Integer> assignedUsers = stagedGameList.getAssignedUsers();
        for (UserInfo user : users) {
            if (assignedUsers.contains(user.getUser().getId())) {
                messages.add(format("ERROR: Cannot create staged games with user {0}. "
                        + "User is already assigned to a staged game.",
                        user.getUser().getId()));
                return;
            }
        }

        /* Validate team sizes. */
        if (gameSettings.getGameType() == MELEE) {
            if (playersPerGame < 0) {
                messages.add(format("Invalid team sizes. Players per game: {0}.", playersPerGame));
            }
        } else {
            if (attackersPerGame < 0 || defendersPerGame < 0 || attackersPerGame + defendersPerGame == 0) {
                messages.add(format("Invalid team sizes. Attackers per game: {0}, defenders per game: {1}.",
                        attackersPerGame, defendersPerGame));
                return;
            }
        }


        adminCreateGamesBean.stageGamesWithUsers(users, gameSettings, roleAssignmentMethod,
                teamAssignmentMethod, attackersPerGame, defendersPerGame, playersPerGame);
    }

    /**
     * Extract and validate POST parameters and forward them to {@link AdminCreateGamesBean#stageEmptyGames(
     * GameSettings, int) AdminCreateGamesBean#stageEmptyGames()}.
     * @param request The HTTP request.
     */
    private void stageEmptyGames(HttpServletRequest request) {
        int numGames;
        try {
            numGames = getIntParameter(request, "numGames").get();
        } catch (NullPointerException e) {
            messages.add("ERROR: Missing parameter: numGames.");
            return;
        } catch (NoSuchElementException e) {
            messages.add("ERROR: Invalid parameter: numGames.");
            return;
        }

        GameSettings gameSettings = extractGameSettings(request);
        if (gameSettings == null) {
            return;
        }

        if (numGames > 100) {
            messages.add("ERROR: Won't create more than 100 staged games.");
            return;
        }

        adminCreateGamesBean.stageEmptyGames(gameSettings, numGames);
    }

    /**
     * Extract and validate POST parameters and forward them to {@link AdminCreateGamesBean#deleteStagedGames(List)
     * AdminCreateGamesBean#deleteStagedGames()}.
     * @param request The HTTP request.
     */
    private void deleteStagedGames(HttpServletRequest request) {
        /* Convert game IDs to ints. */
        String stagedGameIdsStr = request.getParameter("stagedGameIds");
        List<Integer> stagedGameIds;
        try {
            stagedGameIds = Arrays.stream(stagedGameIdsStr.split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .map(stagedGameList::formattedToNumericGameId)
                    .map(Optional::get)
                    .distinct()
                    .collect(Collectors.toList());
        } catch (NullPointerException e) {
            messages.add("ERROR: Missing parameter: stagedGameIds.");
            return;
        } catch (NoSuchElementException e) {
            messages.add("ERROR: Invalid parameter: stagedGameIds.");
            return;
        }

        Map<Integer, StagedGame> existingStagedGames = stagedGameList.getStagedGames();

        /* Verify that all staged games exist. */
        if (!existingStagedGames.keySet().containsAll(stagedGameIds)) {
            messages.add("ERROR: Cannot delete staged games. Not all selected staged games exist.");
            return;
        }

        List<StagedGame> stagedGames = stagedGameIds.stream()
                .map(existingStagedGames::get)
                .collect(Collectors.toList());

        adminCreateGamesBean.deleteStagedGames(stagedGames);
    }

    /**
     * Extract and validate POST parameters and forward them to {@link AdminCreateGamesBean#createStagedGames(List)
     * AdminCreateGamesBean#createStagedGames()}.
     * @param request The HTTP request.
     */
    private void createStagedGames(HttpServletRequest request) {
        /* Convert game IDs to ints. */
        String stagedGameIdsStr = request.getParameter("stagedGameIds");
        List<Integer> stagedGameIds;
        try {
            stagedGameIds = Arrays.stream(stagedGameIdsStr.split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .map(stagedGameList::formattedToNumericGameId)
                    .map(Optional::get)
                    .distinct()
                    .collect(Collectors.toList());
        } catch (NullPointerException e) {
            messages.add("ERROR: Missing parameter: stagedGameIds.");
            return;
        } catch (NoSuchElementException e) {
            messages.add("ERROR: Invalid parameter: stagedGameIds.");
            return;
        }

        Map<Integer, StagedGame> existingStagedGames = stagedGameList.getStagedGames();

        /* Verify that all staged games exist. */
        if (!existingStagedGames.keySet().containsAll(stagedGameIds)) {
            messages.add("ERROR: Cannot create staged games. Not all selected staged games exist.");
            return;
        }

        List<StagedGame> stagedGames = stagedGameIds.stream()
                .map(existingStagedGames::get)
                .collect(Collectors.toList());

        adminCreateGamesBean.createStagedGames(stagedGames);
    }

    /**
     * Extract and validate POST parameters and forward them to {@link AdminCreateGamesBean#removePlayerFromStagedGame(
     * StagedGame, int) AdminCreateGamesBean#removePlayerFromStagedGame()}.
     * @param request The HTTP request.
     */
    private void removePlayerFromStagedGame(HttpServletRequest request) {
        int userId;
        int gameId;
        try {
            userId = getIntParameter(request, "userId").get();
            gameId = stagedGameList.formattedToNumericGameId(request.getParameter("gameId")).get();
        } catch (NullPointerException e) {
            messages.add("ERROR: Missing parameter.");
            return;
        } catch (NoSuchElementException | IllegalArgumentException e) {
            messages.add("ERROR: Invalid parameter.");
            return;
        }

        StagedGame stagedGame = stagedGameList.getStagedGame(gameId);
        if (stagedGame == null) {
            messages.add(format("ERROR: Cannot remove user {0} from staged game {1}. Staged game does not exist.",
                    userId, stagedGameList.numericToFormattedGameId(gameId)));
            return;
        }

        adminCreateGamesBean.removePlayerFromStagedGame(stagedGame, userId);
    }

    /**
     * Extract and validate POST parameters and forward them to
     * {@link AdminCreateGamesBean#removeCreatorFromStagedGame(StagedGame)}
     * AdminCreateGamesBean#removeCreatorFromStagedGame()}.
     * @param request The HTTP request.
     */
    private void removeCreatorFromStagedGame(HttpServletRequest request) {
        int gameId;
        try {
            gameId = stagedGameList.formattedToNumericGameId(request.getParameter("gameId")).get();
        } catch (NullPointerException e) {
            messages.add("ERROR: Missing parameter.");
            return;
        } catch (NoSuchElementException | IllegalArgumentException e) {
            messages.add("ERROR: Invalid parameter.");
            return;
        }

        StagedGame stagedGame = stagedGameList.getStagedGame(gameId);
        if (stagedGame == null) {
            messages.add(format("ERROR: Cannot remove you from staged game {1}. Staged game does not exist.",
                    stagedGameList.numericToFormattedGameId(gameId)));
            return;
        }

        adminCreateGamesBean.removeCreatorFromStagedGame(stagedGame);
    }

    /**
     * Extract and validate POST parameters for {@link AdminCreateGamesBean#switchRole(StagedGame, UserEntity)
     * AdminCreateGamesBean#switchRole()}.
     * @param request The HTTP request.
     */
    private void switchRole(HttpServletRequest request) {
        int userId;
        int gameId;
        try {
            userId = getIntParameter(request, "userId").get();
            gameId = stagedGameList.formattedToNumericGameId(request.getParameter("gameId")).get();
        } catch (NullPointerException e) {
            messages.add("ERROR: Missing parameter.");
            return;
        } catch (IllegalArgumentException | NoSuchElementException e) {
            messages.add("ERROR: Invalid parameter.");
            return;
        }

        UserInfo user = adminCreateGamesBean.getUserInfos().get(userId);
        if (user == null) {
            messages.add(format("ERROR: Cannot switch role of user {0}. User does not exist.", userId));
            return;
        }

        StagedGame stagedGame = stagedGameList.getStagedGame(gameId);
        if (stagedGame == null) {
            messages.add(format("ERROR: Cannot switch role of user {0} in staged game {1}. Staged game does not exist.",
                    userId, stagedGameList.numericToFormattedGameId(gameId)));
            return;
        }

        adminCreateGamesBean.switchRole(stagedGame, user.getUser());
    }

    /**
     * Extract and validate POST parameters for {@link AdminCreateGamesBean#switchCreatorRole(StagedGame)
     * AdminCreateGamesBean#switchCreatorRole()}.
     * @param request The HTTP request.
     */
    private void switchCreatorRole(HttpServletRequest request) {
        int gameId;
        try {
            gameId = stagedGameList.formattedToNumericGameId(request.getParameter("gameId")).get();
        } catch (NullPointerException e) {
            messages.add("ERROR: Missing parameter.");
            return;
        } catch (IllegalArgumentException | NoSuchElementException e) {
            messages.add("ERROR: Invalid parameter.");
            return;
        }

        StagedGame stagedGame = stagedGameList.getStagedGame(gameId);
        if (stagedGame == null) {
            messages.add(format("ERROR: Cannot switch creator role in staged game {1}. Staged game does not exist.",
                    stagedGameList.numericToFormattedGameId(gameId)));
            return;
        }

        adminCreateGamesBean.switchCreatorRole(stagedGame);
    }

    /**
     * Extract and validate POST parameters for {@link AdminCreateGamesBean#movePlayerBetweenStagedGames(StagedGame,
     * StagedGame, UserEntity, Role) AdminCreateGamesBean#movePlayerBetweenStagedGames()}.
     * @param request The HTTP request.
     */
    private void movePlayerBetweenStagedGames(HttpServletRequest request) {
        int userId;
        int gameIdFrom;
        int gameIdTo;
        Role role;
        try {
            userId = getIntParameter(request, "userId").get();
            role = Role.valueOf(request.getParameter("role"));
            gameIdFrom = stagedGameList.formattedToNumericGameId(request.getParameter("gameIdFrom")).get();
            gameIdTo = stagedGameList.formattedToNumericGameId(request.getParameter("gameIdTo")).get();
        } catch (NullPointerException e) {
            messages.add("ERROR: Missing parameter.");
            return;
        } catch (IllegalArgumentException | NoSuchElementException e) {
            messages.add("ERROR: Invalid parameter.");
            return;
        }

        StagedGame stagedGameFrom = stagedGameList.getStagedGame(gameIdFrom);
        if (stagedGameFrom == null) {
            messages.add(format("ERROR: Cannot move user {0} from staged game {1}. Staged game does not exist.",
                    userId, stagedGameList.numericToFormattedGameId(gameIdFrom)));
            return;
        }

        StagedGame stagedGameTo = stagedGameList.getStagedGame(gameIdTo);
        if (stagedGameTo == null) {
            messages.add(format("ERROR: Cannot move user {0} to staged game {1}. Staged game does not exist.",
                    userId, stagedGameList.numericToFormattedGameId(gameIdTo)));
            return;
        }

        UserInfo user = adminCreateGamesBean.getUserInfos().get(userId);
        if (user == null) {
            messages.add(format("ERROR: Cannot move user {0}. User does not exist.",
                    userId, gameIdTo));
            return;
        }

        adminCreateGamesBean.movePlayerBetweenStagedGames(stagedGameFrom, stagedGameTo, user.getUser(), role);
    }

    /**
     * Extract and validate POST parameters for {@link AdminCreateGamesBean#addPlayerToStagedGame(StagedGame, UserEntity,
     * Role) AdminCreateGamesBean#addPlayerToStagedGame()} or {@link AdminCreateGamesBean#addPlayerToExistingGame(
     * AbstractGame, UserEntity, Role) AdminCreateGamesBean#addPlayerToExistingGame()}.
     * @param request The HTTP request.
     */
    private void addPlayerToGame(HttpServletRequest request) {
        int gameId;
        boolean isStagedGame;
        int userId;
        Role role;
        try {
            String gameIdStr = request.getParameter("gameId");
            Optional<Integer> stagedGameId = stagedGameList.formattedToNumericGameId(gameIdStr);
            if (stagedGameId.isPresent()) {
                isStagedGame = true;
                gameId = stagedGameId.get();
            } else {
                isStagedGame = false;
                gameId = Integer.parseInt(gameIdStr);
            }
            userId = getIntParameter(request, "userId").get();
            role = Role.valueOf(request.getParameter("role"));
        } catch (NullPointerException e) {
            messages.add("ERROR: Missing parameter.");
            return;
        } catch (IllegalArgumentException e) {
            messages.add("ERROR: Invalid parameter.");
            return;
        }

        UserInfo user = adminCreateGamesBean.getUserInfos().get(userId);
        if (user == null) {
            if (isStagedGame) {
                messages.add(format("ERROR: Cannot add user {0} to staged game {1}. User does not exist.",
                        userId, stagedGameList.numericToFormattedGameId(gameId)));
            } else {
                messages.add(format("ERROR: Cannot add user {0} to existing game {1}. User does not exist.",
                        userId, gameId));
            }
            return;
        }

        if (isStagedGame) {
            StagedGame stagedGame = stagedGameList.getStagedGame(gameId);
            if (stagedGame == null) {
                messages.add(format("ERROR: Cannot add user {0} to staged game {1}. Staged game does not exist.",
                        userId, stagedGameList.numericToFormattedGameId(gameId)));
                return;
            }
            adminCreateGamesBean.addPlayerToStagedGame(stagedGame, user.getUser(), role);
        } else {
            AbstractGame game = GameDAO.getGame(gameId);
            if (game == null) {
                messages.add(format("ERROR: Cannot add user {0} to existing game {1}. Game does not exist.",
                        userId, gameId));
                return;
            }
            adminCreateGamesBean.addPlayerToExistingGame(game, user.getUser(), role);
        }
    }
}
