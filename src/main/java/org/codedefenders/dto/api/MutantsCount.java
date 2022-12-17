package org.codedefenders.dto.api;

import java.util.Objects;

public class MutantsCount {
    private Integer alive;
    private Integer killed;
    private Integer equivalent;
    private Integer total;

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        MutantsCount that = (MutantsCount) o;
        return Objects.equals(alive, that.alive) && Objects.equals(killed, that.killed) && Objects.equals(equivalent, that.equivalent);
    }

    @Override
    public int hashCode() {
        return Objects.hash(alive, killed, equivalent);
    }

    public MutantsCount(Integer alive, Integer killed, Integer equivalent) {
        this.alive = alive;
        this.killed = killed;
        this.equivalent = equivalent;
        total = alive + killed + equivalent;
    }
}
