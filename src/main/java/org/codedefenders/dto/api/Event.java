package org.codedefenders.dto.api;

public class Event {
    private int userId;
    private String message;
    private String type;
    private long timestamp;
    private Scoreboard scoreboard;

    public Event(int userId, String message, String type, long timestamp, Scoreboard scoreboard) {
        this.userId = userId;
        this.message = message;
        this.type = type;
        this.timestamp = timestamp;
        this.scoreboard = scoreboard;
    }
}
