package org.codedefenders.dto.api;

import org.codedefenders.execution.KillMap;

public class KillMapInfo {
    private final int mutantId;
    private final int testId;
    private final KillMap.KillMapEntry.Status status;

    public KillMapInfo(int mutantId, int testId, KillMap.KillMapEntry.Status status) {
        this.mutantId = mutantId;
        this.testId = testId;
        this.status = status;
    }
}
