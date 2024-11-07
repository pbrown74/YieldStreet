package com.yieldstreet.entity;

import jakarta.persistence.*;

@Entity
public class Accreditation {

    @Id
    @GeneratedValue(strategy=GenerationType.UUID)
    private String accreditationId;

    private String userId;

    // the typeCode is stored in the database, the type isnt
    @Transient
    private transient AccreditationType type;
    @Column(name="type")
    private int typeCode;

    @Transient
    private transient AccreditationStatus status;
    @Column(name="status")
    private int statusCode;

    @OneToOne
    @JoinColumn(name = "document_id")
    private Document document;

    private long lastUpdateTime;

    /**
     * these two methods are just so we can use enums on the pojos,
     * we store them as integers in the database .. Spring maps to and from
     */

    @PrePersist
    void populateDBFields(){
        statusCode = status.getCode();
        typeCode = type.getCode();
    }

    @PostLoad
    void populateTransientFields(){
        status = AccreditationStatus.valueOf(statusCode);
        type = AccreditationType.valueOf(typeCode);
    }

    public String getAccreditationId() {
        return accreditationId;
    }

    public void setAccreditationId(String accreditationId) {
        this.accreditationId = accreditationId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public AccreditationType getType() {
        return type;
    }

    public void setType(AccreditationType type) {
        this.type = type;
        this.typeCode = type.getCode();
    }

    public AccreditationStatus getStatus() {
        return status;
    }

    public void setStatus(AccreditationStatus status) {
        this.status = status;
        this.statusCode = status.getCode();
    }

    public Document getDocument() {
        return document;
    }

    public void setDocument(Document document) {
        this.document = document;
    }

    public long getLastUpdateTime() {
        return lastUpdateTime;
    }

    public void setLastUpdateTime(long lastUpdateTime) {
        this.lastUpdateTime = lastUpdateTime;
    }

}
