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
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import javax.inject.Inject;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.http.HttpStatus;
import org.codedefenders.auth.CodeDefendersAuth;
import org.codedefenders.database.EventDAO;
import org.codedefenders.database.GameDAO;
import org.codedefenders.database.IntentionDAO;
import org.codedefenders.database.PlayerDAO;
import org.codedefenders.database.TargetExecutionDAO;
import org.codedefenders.database.TestSmellsDAO;
import org.codedefenders.dto.SimpleUser;
import org.codedefenders.dto.TestDTO;
import org.codedefenders.dto.api.TestUpload;
import org.codedefenders.execution.CompileException;
import org.codedefenders.execution.IMutationTester;
import org.codedefenders.execution.TargetExecution;
import org.codedefenders.game.AbstractGame;
import org.codedefenders.game.GameState;
import org.codedefenders.game.Role;
import org.codedefenders.game.Test;
import org.codedefenders.game.multiplayer.MeleeGame;
import org.codedefenders.game.multiplayer.MultiplayerGame;
import org.codedefenders.model.DefenderIntention;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;

import static org.codedefenders.execution.TargetExecution.Target.COMPILE_TEST;
import static org.codedefenders.execution.TargetExecution.Target.TEST_ORIGINAL;
import static org.codedefenders.util.Constants.GRACE_PERIOD_MESSAGE;
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
@WebServlet({org.codedefenders.util.Paths.API_TEST,"/api/game/test"})
public class TestAPI extends HttpServlet {
    private static final Logger logger = LoggerFactory.getLogger(TestAPI.class);
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
    TestSmellsDAO testSmellsDAO;
    @Inject
    EventDAO eventDAO;
    @Inject
    IMutationTester mutationTester;

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        final Optional<TestDTO> test = ServletUtils.getIntParameter(request, "testId").map(id -> gameService.getTest(login.getUserId(), id));
        if (!test.isPresent()) {
            response.setStatus(HttpStatus.SC_NOT_FOUND);
            return;
        }
        PrintWriter out = response.getWriter();
        String json = generateJsonForTest(test.get());
        response.setContentType("application/json");
        out.print(json);
        out.flush();
    }

    private String generateJsonForTest(TestDTO test) {
        Gson gson = new GsonBuilder().disableHtmlEscaping().create();
        JsonObject root = new JsonObject();
        root.add("id", gson.toJsonTree(test.getId(), Integer.class));
        root.add("playerId", gson.toJsonTree(test.getPlayerId(), Integer.class));
        root.add("gameId", gson.toJsonTree(test.getGameId(), Integer.class));
        root.add("source", gson.toJsonTree(test.getSource(), String.class));
        return gson.toJson(root);
    }

    @Override
    public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        final TestUpload test;
        try {
            test = (TestUpload) Utils.parsePostOrRespondJsonError(request, response, TestUpload.class);
        } catch (JsonParseException e) {
            return;
        }
        final Integer gameId = test.getGameId();
        AbstractGame abstractGame = GameDAO.getGame(gameId);
        if (abstractGame == null) {
            Utils.respondJsonError(response, "Game with ID " + gameId + " not found", HttpServletResponse.SC_NOT_FOUND);
        } else if (PlayerDAO.getPlayerIdForUserAndGame(login.getUserId(), gameId) == -1) {
            Utils.respondJsonError(response, "You are not part of this game");
        } else if (abstractGame.isCapturePlayersIntention() && test.getSelectedLine() == null) {
            Utils.respondJsonError(response, "This game requires player intentions. You cannot submit a test without specifying the lines to cover");
        } else {
            try {
                CompilationResult compilationResult;
                if (abstractGame instanceof MultiplayerGame || abstractGame instanceof MeleeGame) {
                    compilationResult = createTest(abstractGame, test.getSource(), login.getSimpleUser(), Collections.singleton(test.getSelectedLine()));
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
                out.print(gson.toJsonTree(compilationResult));
                out.flush();
            } catch (CompileException e) {
                Utils.respondJsonError(response, e.getMessage());
            }
        }
    }

    private CompilationResult createTest(AbstractGame game, String testText, SimpleUser user, Set<Integer> selectedLines) throws CompileException {
        boolean isMultiplayer = game instanceof MultiplayerGame;
        if (isMultiplayer && ((MultiplayerGame) game).getRole(login.getUserId()) != Role.DEFENDER) {
            throw new CompileException("You can only submit tests if you are a Defender");
        }
        if (game.getState() != GameState.ACTIVE) {
            throw new CompileException(GRACE_PERIOD_MESSAGE);
        }
        List<String> validationMessages = CodeValidator.validateTestCodeGetMessage(testText, game.getMaxAssertionsPerTest(), game.getCUT().getAssertionLibrary());
        boolean validationSuccess = validationMessages.isEmpty();
        if (!validationSuccess) {
            throw new CompileException(String.join("\n", validationMessages));
        }
        Test newTest;
        try {
            newTest = gameManagingUtils.createTest(game.getId(), game.getClassId(), testText, user.getId(), MODE_BATTLEGROUND_DIR);
        } catch (IOException io) {
            throw new CompileException(TEST_GENERIC_ERROR_MESSAGE);
        }
        Set<Integer> selectedMutants = new HashSet<>();
        logger.debug("New Test {} by user {}", newTest.getId(), user.getId());
        TargetExecution compileTestTarget = TargetExecutionDAO.getTargetExecutionForTest(newTest, COMPILE_TEST);
        if (game.isCapturePlayersIntention()) {
            collectDefenderIntentions(newTest, selectedLines, selectedMutants);
        }
        if (compileTestTarget.status != TargetExecution.Status.SUCCESS) {
            List<String> messages = new ArrayList<>();
            messages.add(TEST_DID_NOT_COMPILE_MESSAGE);
            messages.add(compileTestTarget.message);
            throw new CompileException(String.join("\n", messages));
        }
        TargetExecution testOriginalTarget = TargetExecutionDAO.getTargetExecutionForTest(newTest, TEST_ORIGINAL);
        if (testOriginalTarget.status != TargetExecution.Status.SUCCESS) {
            List<String> messages = new ArrayList<>();
            messages.add(TEST_DID_NOT_PASS_ON_CUT_MESSAGE);
            messages.add(testOriginalTarget.message);
            throw new CompileException(String.join("\n", messages));
        }
        List<String> detectedTestSmells = testSmellsDAO.getDetectedTestSmellsForTest(newTest);
        final String message = user.getName() + " created a test";
        final Timestamp timestamp = new Timestamp(System.currentTimeMillis());
        if (isMultiplayer) {
            final Event notif = new Event(-1, game.getId(), user.getId(), message, EventType.DEFENDER_TEST_CREATED, EventStatus.GAME, timestamp);
            eventDAO.insert(notif);
            mutationTester.runTestOnAllMultiplayerMutants((MultiplayerGame) game, newTest);
        } else {
            final Event notif = new Event(-1, game.getId(), user.getId(), message, EventType.PLAYER_TEST_CREATED, EventStatus.GAME, timestamp);
            eventDAO.insert(notif);
            mutationTester.runTestOnAllMeleeMutants((MeleeGame) game, newTest);
        }
        game.update();
        logger.info("Successfully created test {} ", newTest.getId());
        return new CompilationResult(newTest.getId(), detectedTestSmells);
    }

    private void collectDefenderIntentions(Test newTest, Set<Integer> selectedLines, Set<Integer> selectedMutants) {
        try {
            DefenderIntention intention = new DefenderIntention(selectedLines, selectedMutants);
            intentionDAO.storeIntentionForTest(newTest, intention);
        } catch (Exception e) {
            logger.error("Cannot store intention to database.", e);
        }
    }

    private class CompilationResult {
        private final int id;
        private final List<String> testSmells;

        public CompilationResult(int id, List<String> testSmells) {
            this.id = id;
            this.testSmells = testSmells;
        }
    }
}
