package com.yieldstreet.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class AccreditationTypeAndStatusDTO {

    private String status;
    private String type;

    @JsonCreator()
    public AccreditationTypeAndStatusDTO(@JsonProperty("status")String status, @JsonProperty("accreditation_type")String type){
        this.status = status;
        this.type = type;
    }

    @JsonProperty("status")
    public String getAccreditationStatus(){
        return this.status;
    }

    @JsonProperty("status")
    public void setAccreditationStatus(String status){
        this.status = status;
    }

    @JsonProperty("accreditation_type")
    public String getAccreditationType() {
        return this.type;
    }

    @JsonProperty("accreditation_type")
    public void setAccreditationType(String type){
        this.type = type;
    }

}
