package com.yieldstreet.exception;

public class InvalidMimeTypeException extends RuntimeException {

    private String mimeType;

    public InvalidMimeTypeException(String mimeType){
        this.mimeType = mimeType;
    }

    public String getMimeType(){
        return mimeType;
    }
}
