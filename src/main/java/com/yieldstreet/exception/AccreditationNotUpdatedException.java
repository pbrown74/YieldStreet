package com.yieldstreet.exception;

public class AccreditationNotUpdatedException extends RuntimeException {

    private String id;

    public AccreditationNotUpdatedException(String id){
        this.id = id;
    }

    public String getId(){
        return id;
    }
}
