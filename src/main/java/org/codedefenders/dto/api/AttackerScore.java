package org.codedefenders.dto.api;

import java.util.Objects;

public class AttackerScore extends MultiplayerScore {
    private MutantsCount mutants;

    public AttackerScore(String username, Integer userId, Integer playerId, Integer points, DuelsCount duels, MutantsCount mutants) {
        super(username, userId, playerId, points, duels);
        this.mutants = mutants;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        AttackerScore that = (AttackerScore) o;
        return Objects.equals(mutants, that.mutants) && super.equals(that);
    }

    @Override
    public int hashCode() {
        return Objects.hash(mutants);
    }
}
