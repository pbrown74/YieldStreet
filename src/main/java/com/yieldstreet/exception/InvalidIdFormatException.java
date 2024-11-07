package com.yieldstreet.exception;

public class InvalidIdFormatException extends RuntimeException {

    private String id;

    public InvalidIdFormatException(String id){
        this.id = id;
    }

    public String getId(){
        return id;
    }
}
