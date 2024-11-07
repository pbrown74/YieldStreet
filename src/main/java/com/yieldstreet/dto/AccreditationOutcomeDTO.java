package com.yieldstreet.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class AccreditationOutcomeDTO {

    private String outcome;

    @JsonCreator
    public AccreditationOutcomeDTO(String outcome){
        this.outcome = outcome;
    }

    @JsonProperty("outcome")
    public String getOutcome() {
        return outcome;
    }

}
