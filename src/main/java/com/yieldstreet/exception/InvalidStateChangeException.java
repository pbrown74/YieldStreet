package com.yieldstreet.exception;

import com.yieldstreet.entity.AccreditationStatus;

public class InvalidStateChangeException extends RuntimeException {

    private String id;
    private AccreditationStatus from;
    private AccreditationStatus to;

    public InvalidStateChangeException(String id, AccreditationStatus from, AccreditationStatus to){
        this.id = id;
        this.from = from;
        this.to = to;
    }

    public String getChange(){
        return "Attempt to change state of accreditation: " + id + " from " + from.name() + " to " + to.name();
    }
}
