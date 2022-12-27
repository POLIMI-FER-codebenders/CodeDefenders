package org.codedefenders.dto.api;

import org.codedefenders.servlets.util.api.Utils;

public class ClaimResolution {
    private Boolean accept;
    private Integer mutantId;
    @Utils.JsonOptional
    private String source;

    public Boolean isAccepted() {
        return accept;
    }

    public String getSource() {
        return source;
    }

    public Integer getMutantId() {
        return mutantId;
    }
}
