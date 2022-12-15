package org.codedefenders.servlets.admin.api;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.codedefenders.auth.CodeDefendersAuth;
import org.codedefenders.database.EventDAO;
import org.codedefenders.database.GameDAO;
import org.codedefenders.dto.api.EventList;
import org.codedefenders.dto.api.MeleeScoreboard;
import org.codedefenders.dto.api.MultiplayerScoreboard;
import org.codedefenders.game.AbstractGame;
import org.codedefenders.game.multiplayer.MeleeGame;
import org.codedefenders.game.multiplayer.MultiplayerGame;
import org.codedefenders.model.Event;
import org.codedefenders.persistence.database.SettingsRepository;
import org.codedefenders.persistence.database.UserRepository;
import org.codedefenders.service.AuthService;
import org.codedefenders.service.game.GameService;
import org.codedefenders.servlets.api.GameAPI;
import org.codedefenders.servlets.util.APIUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.MissingRequiredPropertiesException;

import com.google.gson.Gson;

@WebServlet("/admin/api/events")
public class AllEventsAPI extends HttpServlet {
    private static final Logger logger = LoggerFactory.getLogger(CheckTokenAPI.class);
    final Map<String, Class<?>> parameterTypes = new HashMap<String, Class<?>>() {
        {
            put("fromTimestamp", Long.class);
        }
    };
    @Inject
    CodeDefendersAuth login;
    @Inject
    GameService gameService;
    @Inject
    SettingsRepository settingsRepository;
    @Inject
    UserRepository userRepository;
    @Inject
    AuthService authService;
    @Inject
    GameAPI gameAPI;

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        final Map<String, Object> params;
        try {
            params = APIUtils.getParametersOrRespondJsonError(request, response, parameterTypes);
        } catch (MissingRequiredPropertiesException e) {
            return;
        }
        Long fromTimestamp = (Long) params.get("fromTimestamp");
        boolean hasMore = false;
        List<Event> events = EventDAO.getEventsForAllGamesWithCreatorAndAfter(authService.getUser().getId(), fromTimestamp);
        if (events.size() == 501) {
            events.remove(500);
            hasMore = true;
        }
        Map<Integer, List<Event>> eventsByGameId = events.stream().collect(Collectors.groupingBy(Event::gameId));
        Map<Integer, List<org.codedefenders.dto.api.Event>> newEventsByGameId = eventsByGameId.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey,
                es -> es.getValue().stream().map(e -> new org.codedefenders.dto.api.Event(e.getUserId(), "", e.getEventType().toString(), e.getTimestamp() / 1000))
                        .collect(Collectors.toList())));
        Map<Integer, MultiplayerScoreboard> multiplayerScores = new HashMap<>();
        Map<Integer, MeleeScoreboard> meleeScores = new HashMap<>();
        newEventsByGameId.keySet().forEach(gameId -> {
            AbstractGame game = GameDAO.getGame(gameId);
            if (game instanceof MultiplayerGame) {
                multiplayerScores.put(gameId, gameAPI.getMultiplayerScoreboard(game));
            } else if (game instanceof MeleeGame) {
                meleeScores.put(gameId, gameAPI.getMeleeScoreboard(game));
            }
        });
        EventList eventList = new EventList(newEventsByGameId, multiplayerScores, meleeScores, hasMore);
        PrintWriter out = response.getWriter();
        response.setContentType("application/json");
        Gson gson = new Gson();
        out.print(gson.toJson(eventList));
        out.flush();
    }
}
