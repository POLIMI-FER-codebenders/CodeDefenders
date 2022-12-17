package org.codedefenders.dto.api;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class MeleeScoreboard extends Scoreboard {
    List<MeleeScore> players = new ArrayList<>();
    public void addPlayer(MeleeScore player) {
        players.add(player);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        MeleeScoreboard that = (MeleeScoreboard) o;
        return Objects.equals(players, that.players);
    }

    @Override
    public int hashCode() {
        return Objects.hash(players);
    }
}
