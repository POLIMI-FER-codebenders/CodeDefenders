package org.codedefenders.dto.api;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class MultiplayerScoreboard extends Scoreboard {
    List<AttackerScore> attackers = new ArrayList<>();
    AttackerScore attackersTotal;
    List<DefenderScore> defenders = new ArrayList<>();
    DefenderScore defendersTotal;

    public void addAttacker(AttackerScore attacker) {
        attackers.add(attacker);
    }

    public List<AttackerScore> getAttackers() {
        return attackers;
    }

    public void addDefender(DefenderScore defender) {
        defenders.add(defender);
    }

    public List<DefenderScore> getDefenders() {
        return defenders;
    }

    public void setAttackersTotal(AttackerScore attackersTotal) {
        this.attackersTotal = attackersTotal;
    }

    public AttackerScore getAttackersTotal() {
        return attackersTotal;
    }

    public void setDefendersTotal(DefenderScore defendersTotal) {
        this.defendersTotal = defendersTotal;
    }

    public DefenderScore getDefendersTotal() {
        return defendersTotal;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        MultiplayerScoreboard that = (MultiplayerScoreboard) o;
        return Objects.equals(attackers, that.attackers) && Objects.equals(attackersTotal, that.attackersTotal) && Objects.equals(defenders, that.defenders) && Objects.equals(defendersTotal, that.defendersTotal);
    }

    @Override
    public int hashCode() {
        return Objects.hash(attackers, attackersTotal, defenders, defendersTotal);
    }
}
