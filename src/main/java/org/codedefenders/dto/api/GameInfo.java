package org.codedefenders.dto.api;

import java.util.List;

import org.codedefenders.game.GameState;

public class GameInfo {
    Integer gameId;
    Integer classId;
    GameState state;
    List<MutantInfo> mutants;
    List<TestInfo> tests;
    Scoreboard scoreboard;
    Boolean capturePlayersIntention;

    public GameInfo(Integer gameId, Integer classId, GameState state, List<MutantInfo> mutants, List<TestInfo> tests, Scoreboard scoreboard, Boolean capturePlayersIntention) {
        this.gameId = gameId;
        this.classId = classId;
        this.state = state;
        this.mutants = mutants;
        this.tests = tests;
        this.scoreboard = scoreboard;
        this.capturePlayersIntention = capturePlayersIntention;
    }
}
