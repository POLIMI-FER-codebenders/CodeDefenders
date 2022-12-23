package org.codedefenders.dto.api;

import java.util.List;

import org.codedefenders.dto.MutantDTO;
import org.codedefenders.game.Mutant;

public class MutantInfo {
    Integer id;
    Integer playerId;
    Mutant.State state;
    List<Integer> mutatedLines;
    Integer points;
    Integer killingTestId;
    Boolean canMarkEquivalent;

    public MutantInfo(Integer id, Integer playerId, Mutant.State state, List<Integer> mutatedLines, Integer points, Integer killingTestId, Boolean canMarkEquivalent) {
        this.id = id;
        this.playerId = playerId;
        this.state = state;
        this.mutatedLines = mutatedLines;
        this.points = points;
        this.killingTestId = killingTestId;
        this.canMarkEquivalent = canMarkEquivalent;
    }

    public static MutantInfo fromMutantDTO(MutantDTO mutant) {
        return new MutantInfo(mutant.getId(), mutant.getPlayerId(), mutant.getState(), mutant.getLines(), mutant.getPoints(), mutant.getKilledByTestId(),
                mutant.isCanMarkEquivalent());
    }
}
