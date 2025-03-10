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
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.codedefenders.auth.CodeDefendersAuth;
import org.codedefenders.beans.message.MessagesBean;
import org.codedefenders.database.AdminDAO;
import org.codedefenders.database.EventDAO;
import org.codedefenders.database.GameDAO;
import org.codedefenders.database.KillmapDAO;
import org.codedefenders.database.MeleeGameDAO;
import org.codedefenders.database.MultiplayerGameDAO;
import org.codedefenders.database.MutantDAO;
import org.codedefenders.database.TestDAO;
import org.codedefenders.dto.SimpleUser;
import org.codedefenders.execution.KillMap.KillMapType;
import org.codedefenders.execution.KillMapProcessor.KillMapJob;
import org.codedefenders.game.AbstractGame;
import org.codedefenders.game.GameState;
import org.codedefenders.game.Mutant;
import org.codedefenders.game.Role;
import org.codedefenders.game.Test;
import org.codedefenders.game.multiplayer.MeleeGame;
import org.codedefenders.game.multiplayer.MultiplayerGame;
import org.codedefenders.game.multiplayer.PlayerScore;
import org.codedefenders.persistence.database.UserRepository;
import org.codedefenders.service.UserService;
import org.codedefenders.servlets.util.Redirect;
import org.codedefenders.util.Constants;
import org.codedefenders.util.Paths;

@WebServlet(Paths.ADMIN_MONITOR)
public class AdminMonitorGames extends HttpServlet {

    @Inject
    private MessagesBean messages;

    @Inject
    private EventDAO eventDAO;

    @Inject
    private CodeDefendersAuth login;

    @Inject
    private UserRepository userRepo;

    @Inject
    private UserService userService;

    public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {

        List<MultiplayerGame> multiplayerGames = MultiplayerGameDAO.getAvailableMultiplayerGames();

        Map<Integer, String> multiplayerGameCreatorNames = multiplayerGames.stream()
                .collect(Collectors.toMap(AbstractGame::getId,
                        game -> userService.getSimpleUserById(game.getCreatorId())
                                .map(SimpleUser::getName)
                                .orElse("Unknown user")));

        Map<Integer, List<List<String>>> multiplayerPlayersInfoForGame = multiplayerGames.stream()
                .map(AbstractGame::getId)
                .collect(Collectors.toMap(id -> id, AdminDAO::getPlayersInfo));

        Map<Integer, Integer> multiplayerUserIdForPlayerIds = multiplayerPlayersInfoForGame.values().stream()
                .flatMap(Collection::stream)
                .map(list -> list.get(0))
                .map(Integer::parseInt)
                .distinct()
                .collect(Collectors.toMap(pid -> pid, pid -> userRepo.getUserIdForPlayerId(pid).orElse(0)));

        request.setAttribute("multiplayerGames", multiplayerGames);
        request.setAttribute("multiplayerGameCreatorNames", multiplayerGameCreatorNames);
        request.setAttribute("multiplayerPlayersInfoForGame", multiplayerPlayersInfoForGame);
        request.setAttribute("multiplayerUserIdForPlayerIds", multiplayerUserIdForPlayerIds);

        List<MeleeGame> meleeGames = MeleeGameDAO.getAvailableMeleeGames();
        Map<Integer, String> meleeGameCreatorNames = meleeGames.stream()
                .collect(Collectors.toMap(AbstractGame::getId,
                        game -> userService.getSimpleUserById(game.getCreatorId())
                                .map(SimpleUser::getName)
                                .orElse("Unknown user")));

        Map<Integer, List<List<String>>> meleePlayersInfoForGame = meleeGames.stream()
                .map(AbstractGame::getId)
                .collect(Collectors.toMap(id -> id, AdminDAO::getPlayersInfo));

        Map<Integer, Integer> meleeUserIdForPlayerIds = meleePlayersInfoForGame.values().stream()
                .flatMap(Collection::stream)
                .map(list -> list.get(0))
                .map(Integer::parseInt)
                .distinct()
                .collect(Collectors.toMap(pid -> pid, pid -> userRepo.getUserIdForPlayerId(pid).orElse(0)));

        request.setAttribute("meleeGames", meleeGames);
        request.setAttribute("meleeGameCreatorNames", meleeGameCreatorNames);
        request.setAttribute("meleePlayersInfoForGame", meleePlayersInfoForGame);
        request.setAttribute("meleeUserIdForPlayerIds", meleeUserIdForPlayerIds);

        request.getRequestDispatcher(Constants.ADMIN_MONITOR_JSP).forward(request, response);
    }

    public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        switch (request.getParameter("formType")) {

            case "startStopGame":
                startStopGame(request, response);
                break;
            default:
                System.err.println("Action not recognised");
                Redirect.redirectBack(request, response);
                break;
        }
    }


    private void startStopGame(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String playerToRemoveIdGameIdString = request.getParameter("activeGameUserRemoveButton");
        String playerToSwitchIdGameIdString = request.getParameter("activeGameUserSwitchButton");
        boolean switchUser = playerToSwitchIdGameIdString != null;
        AbstractGame game;
        if (playerToRemoveIdGameIdString != null || playerToSwitchIdGameIdString != null) { // admin is removing user from temp game
            int playerToRemoveId = Integer.parseInt((switchUser ? playerToSwitchIdGameIdString : playerToRemoveIdGameIdString).split("-")[0]);
            int gameToRemoveFromId = Integer.parseInt((switchUser ? playerToSwitchIdGameIdString : playerToRemoveIdGameIdString).split("-")[1]);
            Optional<Integer> userId = userRepo.getUserIdForPlayerId(playerToRemoveId);
            if (userId.isPresent() && !deletePlayer(playerToRemoveId, gameToRemoveFromId, userId.get())) {
                messages.add("Deleting player " + playerToRemoveId + " failed! \n Please check the logs!");
            } else if (switchUser && userId.isPresent()) {
                Role newRole = Role.valueOf(playerToSwitchIdGameIdString.split("-")[2]).equals(Role.ATTACKER)
                        ? Role.DEFENDER : Role.ATTACKER;
                game = GameDAO.getGame(gameToRemoveFromId);
                if (game != null) {
                    game.setEventDAO(eventDAO);
                    game.setUserRepository(userRepo);
                    if (!game.addPlayer(userId.get(), newRole)) {
                        messages.add("Inserting user " + userId.get() + " failed! \n Please check the logs!");
                    } else {
                        messages.add("The game with id " + gameToRemoveFromId + " doesn't exist! Could not switch user"
                                + " role. \n Please check the logs!");
                    }
                }
            }

        } else {  // admin is starting or stopping selected games

            String[] selectedGames = request.getParameterValues("selectedGames");
            String gameSelectedViaPlayButton = request.getParameter("start_stop_btn");

            if (selectedGames == null || gameSelectedViaPlayButton != null) {
                // admin is starting or stopping a single game
                int gameId = -1;
                // Get the identifying information required to create a game from the submitted form.

                try {
                    gameId = Integer.parseInt(gameSelectedViaPlayButton);
                } catch (Exception e) {
                    messages.add("There was a problem with the form.");
                    response.sendRedirect(request.getContextPath() + "/admin");
                    return;
                }


                String errorMessage = "ERROR trying to start or stop game " + gameId
                        + ".\nIf this problem persists, contact your administrator.";

                game = GameDAO.getGame(gameId);

                if (game == null) {
                    messages.add(errorMessage);
                } else {
                    GameState newState = game.getState() == GameState.ACTIVE ? GameState.FINISHED : GameState.ACTIVE;
                    game.setState(newState);
                    if (!game.update()) {
                        messages.add(errorMessage);
                    } else {
                        // Schedule the killmap
                        if (GameState.FINISHED.equals(newState)) {
                            KillmapDAO.enqueueJob(new KillMapJob(KillMapType.GAME, gameId));
                        }
                    }
                }
            } else {
                GameState newState = request.getParameter("games_btn").equals("Start Games")
                        ? GameState.ACTIVE : GameState.FINISHED;
                for (String gameId : selectedGames) {
                    game = GameDAO.getGame(Integer.parseInt(gameId));
                    game.setState(newState);
                    if (!game.update()) {
                        messages.add("ERROR trying to start or stop game " + String.valueOf(gameId));
                    } else {
                        // Schedule the killmap
                        if (GameState.FINISHED.equals(newState)) {
                            KillmapDAO.enqueueJob(new KillMapJob(KillMapType.GAME, Integer.parseInt(gameId)));
                        }
                    }
                }
            }
        }
        response.sendRedirect(request.getContextPath() + Paths.ADMIN_MONITOR);
    }


    private boolean deletePlayer(int playerId, int gameId, int userId) {
        for (Test t : TestDAO.getTestsForGame(gameId)) {
            if (t.getPlayerId() == playerId) {
                AdminDAO.deleteTestTargetExecutions(t.getId());
            }
        }
        for (Mutant m : MutantDAO.getValidMutantsForGame(gameId)) {
            if (m.getPlayerId() == playerId) {
                AdminDAO.deleteMutantTargetExecutions(m.getId());
            }
        }
        eventDAO.removePlayerEventsForGame(gameId, userId);
        AdminDAO.deleteAttackerEquivalences(playerId);
        AdminDAO.deleteDefenderEquivalences(playerId);
        AdminDAO.deletePlayerTest(playerId);
        AdminDAO.deletePlayerMutants(playerId);
        return AdminDAO.deletePlayer(playerId);
    }

    public static int getPlayerScore(MultiplayerGame mg, int pid) {
        HashMap<Integer, PlayerScore> mutantScores = mg.getMutantScores();
        HashMap<Integer, PlayerScore> testScores = mg.getTestScores();
        if (mutantScores.containsKey(pid) && mutantScores.get(pid) != null) {
            return (mutantScores.get(pid)).getTotalScore();
        } else if (testScores.containsKey(pid) && testScores.get(pid) != null) {
            return (testScores.get(pid)).getTotalScore();
        }
        return 0;
    }

    public static int getPlayerScore(MeleeGame mg, int pid) {
        Map<Integer, PlayerScore> mutantScores = mg.getMutantScores();
        Map<Integer, PlayerScore> testScores = mg.getTestScores();
        if (mutantScores.containsKey(pid) && mutantScores.get(pid) != null) {
            return (mutantScores.get(pid)).getTotalScore();
        } else if (testScores.containsKey(pid) && testScores.get(pid) != null) {
            return (testScores.get(pid)).getTotalScore();
        }
        return 0;
    }
}
