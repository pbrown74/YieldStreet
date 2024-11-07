package com.yieldstreet.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;

@Entity
public class AccreditationHistory {

    @Id
    @GeneratedValue(strategy=GenerationType.UUID)
    private String accreditationHistoryId;

    private String accreditationId;
    private long lastUpdateTime;

    @Transient
    private transient AccreditationStatus oldStatus;
    @Column(name="oldStatus")
    private int oldStatusCode;

    @PrePersist
    void populateDBFields(){
        oldStatusCode = oldStatus.getCode();
    }

    @PostLoad
    void populateTransientFields(){
        oldStatus = AccreditationStatus.valueOf(oldStatusCode);
    }

    @JsonProperty("accreditation_history_id")
    public String getAccreditationHistoryId() {
        return accreditationHistoryId;
    }

    @JsonProperty("accreditation_history_id")
    public void setAccreditationHistoryId(String accreditationHistoryId) {
        this.accreditationHistoryId = accreditationHistoryId;
    }

    @JsonProperty("accreditation_id")
    public String getAccreditationId() {
        return accreditationId;
    }

    @JsonProperty("accreditation_id")
    public void setAccreditationId(String accreditationId) {
        this.accreditationId = accreditationId;
    }

    @JsonProperty("accreditation_old_status")
    public AccreditationStatus getOldStatus() {
        return oldStatus;
    }

    @JsonProperty("accreditation_old_status")
    public void setOldStatus(AccreditationStatus oldStatus) {
        this.oldStatus = oldStatus;
        this.oldStatusCode = oldStatus.getCode();
    }

    @JsonProperty("lastUpdateTime")
    public long getLastUpdateTime() {
        return lastUpdateTime;
    }

    @JsonProperty("lastUpdateTime")
    public void setLastUpdateTime(long lastUpdateTime) {
        this.lastUpdateTime = lastUpdateTime;
    }

}
