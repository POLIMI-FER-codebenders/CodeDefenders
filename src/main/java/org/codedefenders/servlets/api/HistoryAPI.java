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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.codedefenders.beans.game.ScoreboardCacheBean;
import org.codedefenders.database.GameClassDAO;
import org.codedefenders.database.KillmapDAO;
import org.codedefenders.database.MeleeGameDAO;
import org.codedefenders.database.MultiplayerGameDAO;
import org.codedefenders.dto.api.ClassInfo;
import org.codedefenders.dto.api.GameInfo;
import org.codedefenders.dto.api.KillMapInfo;
import org.codedefenders.dto.api.MutantInfo;
import org.codedefenders.dto.api.Scoreboard;
import org.codedefenders.dto.api.TestInfo;
import org.codedefenders.execution.KillMap;
import org.codedefenders.game.AbstractGame;
import org.codedefenders.game.GameClass;
import org.codedefenders.game.Mutant;
import org.codedefenders.game.Test;
import org.codedefenders.game.multiplayer.MeleeGame;
import org.codedefenders.game.multiplayer.MultiplayerGame;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
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
@WebServlet("/api/history")
public class HistoryAPI extends HttpServlet {
    @Inject
    ScoreboardCacheBean scoreboardCacheBean;

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        List<GameClass> classes = GameClassDAO.getAllPlayableClasses();
        List<KillMap.KillMapEntry> killMaps = classes.stream().flatMap(c -> KillmapDAO.getKillMapEntriesForClass(c.getId()).stream()).collect(Collectors.toList());
        Map<Integer, String> mutantCodes = new HashMap<>(killMaps.stream().collect(Collectors.toMap(k -> k.mutant.getId(), k -> k.mutant.getPatchString(), (dup1, dup2) -> dup1)));
        Map<Integer, String> testCodes = new HashMap<>(killMaps.stream().collect(Collectors.toMap(k -> k.test.getId(), k -> k.test.getAsString(), (dup1, dup2) -> dup1)));
        List<AbstractGame> abstractGames = MultiplayerGameDAO.getFinishedMultiplayerGames().stream().map(g -> (AbstractGame) g).collect(Collectors.toList());
        abstractGames.addAll(MeleeGameDAO.getFinishedMeleeGames().stream().map(g -> (AbstractGame) g).collect(Collectors.toList()));
        List<GameInfo> gameInfos = new ArrayList<>();
        for (AbstractGame abstractGame : abstractGames) {
            Scoreboard scoreboard;
            if (abstractGame instanceof MultiplayerGame) {
                scoreboard = scoreboardCacheBean.getMultiplayerScoreboard((MultiplayerGame) abstractGame);
            } else if (abstractGame instanceof MeleeGame) {
                scoreboard = scoreboardCacheBean.getMeleeScoreboard((MeleeGame) abstractGame);
            } else {
                continue;
            }
            List<MutantInfo> mutantInfos = abstractGame.getMutants().stream().map(MutantInfo::fromMutant).collect(Collectors.toList());
            mutantCodes.putAll(abstractGame.getMutants().stream().collect(Collectors.toMap(Mutant::getId, Mutant::getPatchString)));
            List<TestInfo> testInfos = abstractGame.getTests().stream().map(t -> TestInfo.fromTest(t, abstractGame.getMutants())).collect(Collectors.toList());
            testCodes.putAll(abstractGame.getTests().stream().collect(Collectors.toMap(Test::getId, Test::getAsString)));
            gameInfos.add(new GameInfo(abstractGame.getId(), abstractGame.getClassId(), abstractGame.getState(), mutantInfos, testInfos, scoreboard,
                    abstractGame.isCapturePlayersIntention()));
        }
        List<ClassInfo> classInfos = classes.stream().map(c -> new ClassInfo(c.getId(), c.getName(), c.getAlias(), c.getSourceCode(), c.getTestingFramework(),
                c.getAssertionLibrary())).collect(Collectors.toList());
        List<KillMapInfo> killMapInfos = killMaps.stream().map(c -> new KillMapInfo(c.mutant.getId(), c.test.getId(), c.status)).collect(Collectors.toList());
        Gson gson = new GsonBuilder().disableHtmlEscaping().create();
        PrintWriter out = response.getWriter();
        response.setContentType("application/json");
        JsonObject root = new JsonObject();
        root.add("classes", gson.toJsonTree(classInfos));
        root.add("mutantSources", gson.toJsonTree(mutantCodes));
        root.add("testSources", gson.toJsonTree(testCodes));
        root.add("games", gson.toJsonTree(gameInfos));
        root.add("killMaps", gson.toJsonTree(killMapInfos));
        out.print(gson.toJson(root));
        out.flush();
    }
}
