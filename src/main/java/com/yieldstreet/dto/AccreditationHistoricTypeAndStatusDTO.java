package com.yieldstreet.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class AccreditationHistoricTypeAndStatusDTO extends AccreditationTypeAndStatusDTO {

    private String lastUpdateTime;

    @JsonCreator()
    public AccreditationHistoricTypeAndStatusDTO(@JsonProperty("status")String status,
                                                 @JsonProperty("accreditation_type")String type,
                                                 @JsonProperty("last_update_time")long lastUpdateTime){
        super(status, type);
        this.lastUpdateTime = Long.toString(lastUpdateTime);
    }

    @JsonProperty("last_update_time")
    public String getLastUpdateTime() {
        return this.lastUpdateTime;
    }

    @JsonProperty("last_update_time")
    public void setLastUpdateTime(String lastUpdateTime) {
        this.lastUpdateTime = lastUpdateTime;
    }

}
