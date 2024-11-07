package com.yieldstreet.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class AccreditationIdDTO {

    private String accreditationId;

    @JsonProperty("accreditation_id")
    public String getAccreditationId() {
        return accreditationId;
    }

    @JsonCreator()
    public void setAccreditationId(String accreditationId) {
        this.accreditationId = accreditationId;
    }

}
