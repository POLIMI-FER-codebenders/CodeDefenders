package org.codedefenders.dto.api;

import org.codedefenders.model.AttackerIntention;
import org.codedefenders.servlets.util.api.Utils;

public class MutantUpload {
    private Integer gameId;
    private String source;
    @Utils.JsonOptional
    private AttackerIntention attackerIntention;

    public Integer getGameId() {
        return gameId;
    }

    public String getSource() {
        return source;
    }

    public AttackerIntention getAttackerIntention() {
        return attackerIntention;
    }
}
