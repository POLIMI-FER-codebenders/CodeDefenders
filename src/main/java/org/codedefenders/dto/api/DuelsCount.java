package org.codedefenders.dto.api;

import java.util.Objects;

public class DuelsCount {
    private Integer won;
    private Integer lost;
    private Integer ongoing;

    public DuelsCount(Integer won, Integer lost, Integer ongoing) {
        this.won = won;
        this.lost = lost;
        this.ongoing = ongoing;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        DuelsCount that = (DuelsCount) o;
        return Objects.equals(won, that.won) && Objects.equals(lost, that.lost) && Objects.equals(ongoing, that.ongoing);
    }

    @Override
    public int hashCode() {
        return Objects.hash(won, lost, ongoing);
    }
}
