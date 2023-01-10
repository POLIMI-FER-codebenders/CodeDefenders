package org.codedefenders.dto.api;

import java.util.Objects;

public class TestsCount {
    private Integer killing;
    private Integer nonkilling;
    private Integer total;

    public TestsCount(Integer killing, Integer nonkilling) {
        this.killing = killing;
        this.nonkilling = nonkilling;
        total = killing + nonkilling;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        TestsCount that = (TestsCount) o;
        return Objects.equals(killing, that.killing) && Objects.equals(nonkilling, that.nonkilling);
    }

    @Override
    public int hashCode() {
        return Objects.hash(killing, nonkilling);
    }
}
