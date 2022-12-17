package org.codedefenders.dto.api;

import java.util.Objects;

public class MultiplayerScore extends Score {
    protected DuelsCount duels;

    public MultiplayerScore(String username, Integer userId, Integer playerId, Integer points, DuelsCount duels) {
        super(username, userId, playerId, points);
        this.duels = duels;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        if (!super.equals(o)) {
            return false;
        }
        MultiplayerScore that = (MultiplayerScore) o;
        return Objects.equals(duels, that.duels) && super.equals(that);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), duels);
    }
}
