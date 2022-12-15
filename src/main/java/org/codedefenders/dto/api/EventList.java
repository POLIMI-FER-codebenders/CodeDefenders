package org.codedefenders.dto.api;

import java.util.List;
import java.util.Map;

public class EventList {
    private Map<Integer, List<Event>> events;
    private Map<Integer, MultiplayerScoreboard> multiplayerScores;
    private Map<Integer, MeleeScoreboard> meleeScores;
    private boolean hasMore;

    public EventList(Map<Integer, List<Event>> events, Map<Integer, MultiplayerScoreboard> multiplayerScores, Map<Integer, MeleeScoreboard> meleeScores, boolean hasMore) {
        this.events = events;
        this.multiplayerScores = multiplayerScores;
        this.meleeScores = meleeScores;
        this.hasMore = hasMore;
    }
}
