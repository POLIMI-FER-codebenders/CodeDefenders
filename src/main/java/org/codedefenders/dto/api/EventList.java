package org.codedefenders.dto.api;

import java.util.List;
import java.util.Map;

public class EventList {
    private Map<Integer, List<Event>> events;
    private boolean hasMore;

    public EventList(Map<Integer, List<Event>> events, boolean hasMore) {
        this.events = events;
        this.hasMore = hasMore;
    }
}
