package org.codedefenders.dto.api;

import java.util.Objects;

public abstract class Score {
    private String username;
    private Integer userId;
    private Integer playerId;
    private Integer points;

    public Score(String username, Integer userId, Integer playerId, Integer points) {
        this.username = username;
        this.userId = userId;
        this.playerId = playerId;
        this.points = points;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Score score = (Score) o;
        return Objects.equals(username, score.username) && Objects.equals(userId, score.userId) && Objects.equals(playerId, score.playerId) && Objects.equals(points, score.points);
    }

    @Override
    public int hashCode() {
        return Objects.hash(username, userId, playerId, points);
    }
}
