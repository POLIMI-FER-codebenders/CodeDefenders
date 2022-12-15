package org.codedefenders.dto.api;

public class Event {
    private int userId;
    private String message;
    private String type;
    private long timestamp;

    public Event(int userId, String message, String type, long timestamp) {
        this.userId = userId;
        this.message = message;
        this.type = type;
        this.timestamp = timestamp;
    }

    public int getUserId() {
        return userId;
    }

    public String getMessage() {
        return message;
    }

    public String getType() {
        return type;
    }

    public long getTimestamp() {
        return timestamp;
    }
}
