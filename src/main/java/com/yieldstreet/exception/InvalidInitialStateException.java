package com.yieldstreet.exception;

public class InvalidInitialStateException extends RuntimeException {

    private String id;

    public InvalidInitialStateException(String existingAccreditationId){
        this.id = existingAccreditationId;
    }

    public String getAccreditationId(){
        return "Attempt to initiate a pending transaction when user already has one: " + id;
    }
}
