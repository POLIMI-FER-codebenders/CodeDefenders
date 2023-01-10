package org.codedefenders.dto.api;

import java.util.Objects;

public class DefenderScore extends MultiplayerScore {
    private TestsCount tests;

    public DefenderScore(String username, Integer userId, Integer playerId, Integer points, DuelsCount duels, TestsCount tests) {
        super(username, userId, playerId, points, duels);
        this.tests = tests;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        DefenderScore that = (DefenderScore) o;
        return Objects.equals(tests, that.tests) && super.equals(that);
    }

    @Override
    public int hashCode() {
        return Objects.hash(tests);
    }
}
