package com.yieldstreet.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.yieldstreet.entity.Accreditation;

import java.util.List;
import java.util.Map;
import java.util.Vector;

public class UserAccreditationsDTO {

    private String userId;
    private Map<String, AccreditationTypeAndStatusDTO> accreditationStatuses;

    @JsonCreator()
    public UserAccreditationsDTO(@JsonProperty("user_id")String userId,
                                 @JsonProperty("accreditation_statuses")Map<String, AccreditationTypeAndStatusDTO> accreditationStatuses){
        this.userId = userId;
        this.accreditationStatuses = accreditationStatuses;
    }

    @JsonProperty("user_id")
    public String getUserId() {
        return userId;
    }

    @JsonProperty("accreditation_statuses")
    public Map<String, AccreditationTypeAndStatusDTO> getAccreditationStatuses(){
        return accreditationStatuses;
    }

}
