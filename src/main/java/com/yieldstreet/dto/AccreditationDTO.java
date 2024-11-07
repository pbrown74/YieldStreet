package com.yieldstreet.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.yieldstreet.entity.AccreditationStatus;
import com.yieldstreet.entity.AccreditationType;

public class AccreditationDTO implements Cloneable {

    private String accreditationId;
    private String userId;
    private AccreditationStatus status;
    private AccreditationType type;
    private DocumentDTO document;
    private long lastUpdateTime;

    @JsonProperty("accreditation_id")
    public String getAccreditationId() {
        return accreditationId;
    }

    @JsonProperty("accreditation_id")
    public void setAccreditationId(String accreditationId) {
        this.accreditationId = accreditationId;
    }

    @JsonProperty("user_id")
    public String getUserId() {
        return userId;
    }

    @JsonProperty("user_id")
    public void setUserId(String userId) {
        this.userId = userId;
    }

    @JsonProperty("status")
    public AccreditationStatus getStatus() {
        return status;
    }

    @JsonProperty("status")
    public void setStatus(AccreditationStatus status) {
        this.status = status;
    }

    @JsonProperty("accreditation_type")
    public AccreditationType getType() {
        return type;
    }

    @JsonProperty("accreditation_type")
    public void setType(AccreditationType type) {
        this.type = type;
    }

    @JsonProperty("document")
    public DocumentDTO getDocument() {
        return document;
    }

    @JsonProperty("document")
    public void setDocument(DocumentDTO document) {
        this.document = document;
    }

    @JsonProperty("last_update_time")
    public long getLastUpdateTime() {
        return lastUpdateTime;
    }

    @JsonProperty("last_update_time")
    public void setLastUpdateTime(long lastUpdateTime) {
        this.lastUpdateTime = lastUpdateTime;
    }

    public Object clone() throws CloneNotSupportedException {
        return super.clone();
    }

}
