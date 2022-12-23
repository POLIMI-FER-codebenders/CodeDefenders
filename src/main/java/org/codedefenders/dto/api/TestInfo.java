package org.codedefenders.dto.api;

import java.util.List;

import org.codedefenders.dto.TestDTO;

public class TestInfo {
    Integer id;
    private Integer playerId;
    private Integer points;
    private List<Integer> coveredLines;
    private List<Integer> coveredMutantIds;
    private List<Integer> killedMutantIds;

    public TestInfo(Integer id, Integer playerId, Integer points, List<Integer> coveredLines, List<Integer> coveredMutantIds, List<Integer> killedMutantIds) {
        this.id = id;
        this.playerId = playerId;
        this.points = points;
        this.coveredLines = coveredLines;
        this.coveredMutantIds = coveredMutantIds;
        this.killedMutantIds = killedMutantIds;
    }

    public static TestInfo fromTestDTO(TestDTO test) {
        return new TestInfo(test.getId(), test.getPlayerId(), test.getPoints(), test.getLinesCovered(), test.getCoveredMutantIds(), test.getKilledMutantIds());
    }
}
