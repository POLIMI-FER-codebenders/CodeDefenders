package org.codedefenders.dto.api;

import org.codedefenders.servlets.util.api.Utils;

public class TestUpload {
    private Integer gameId;
    private String source;
    @Utils.JsonOptional
    private Integer selectedLine;

    public Integer getGameId() {
        return gameId;
    }

    public String getSource() {
        return source;
    }

    public Integer getSelectedLine() {
        return selectedLine;
    }
}
