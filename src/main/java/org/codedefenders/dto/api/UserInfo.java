package org.codedefenders.dto.api;

import java.util.HashMap;
import java.util.Map;

import org.codedefenders.dto.UserStats;

public class UserInfo {
    String username;
    Map<String, Integer> mutants;
    Map<String, Integer> tests;
    Map<String, Double> points;
    Map<String, Integer> games;

    public UserInfo(String username, Map<String, Integer> mutants, Map<String, Integer> tests, Map<String, Double> points, Map<String, Integer> games) {
        this.username = username;
        this.mutants = mutants;
        this.tests = tests;
        this.points = points;
        this.games = games;
    }

    public static UserInfo fromUserStats(String username, UserStats stats) {
        Map<String, Integer> mutants = new HashMap<>();
        mutants.put("alive", stats.getAliveMutants());
        mutants.put("dead", stats.getKilledMutants());
        mutants.put("equivalent", stats.getEquivalentMutants());
        mutants.put("total", stats.getAliveMutants() + stats.getKilledMutants());
        Map<String, Integer> tests = new HashMap<>();
        tests.put("killing", stats.getKillingTests());
        tests.put("nonkilling", stats.getNonKillingTests());
        tests.put("killCount", stats.getTestKillCount());
        tests.put("total", stats.getKillingTests() + stats.getNonKillingTests());
        Map<String, Double> points = new HashMap<>();
        points.put("fromMutants", (double) stats.getTotalPointsMutants());
        points.put("fromTests", (double) stats.getTotalPointsTests());
        points.put("avgPerMutant", stats.getAvgPointsMutants());
        points.put("avgPerTest", stats.getAvgPointsTests());
        points.put("total", (double) stats.getTotalPoints());
        Map<String, Integer> games = new HashMap<>();
        games.put("asAttacker", stats.getAttackerGames());
        games.put("asDefender", stats.getDefenderGames());
        games.put("total", stats.getTotalGames());
        return new UserInfo(username, mutants, tests, points, games);
    }
}
