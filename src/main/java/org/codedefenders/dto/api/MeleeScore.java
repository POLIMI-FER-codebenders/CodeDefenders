package org.codedefenders.dto.api;

import java.util.Objects;

public class MeleeScore extends Score {
    private Integer attackPoints;
    private Integer defensePoints;
    private Integer duelPoints;

    public MeleeScore(String username, Integer userId, Integer playerId, Integer points, Integer attackPoints, Integer defensePoints, Integer duelPoints) {
        super(username, userId, playerId, points);
        this.attackPoints = attackPoints;
        this.defensePoints = defensePoints;
        this.duelPoints = duelPoints;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        MeleeScore that = (MeleeScore) o;
        return Objects.equals(attackPoints, that.attackPoints) && Objects.equals(defensePoints, that.defensePoints) && Objects.equals(duelPoints, that.duelPoints) && super.equals(that);
    }

    @Override
    public int hashCode() {
        return Objects.hash(attackPoints, defensePoints, duelPoints);
    }
}
