package com.yieldstreet.exception;

public class AccreditationNotFoundException extends RuntimeException {

    private String id;

    public AccreditationNotFoundException(String id){
        this.id = id;
    }

    public String getId(){
        return id;
    }
}
