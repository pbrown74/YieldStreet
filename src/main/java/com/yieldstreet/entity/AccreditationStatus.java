package com.yieldstreet.entity;

public enum AccreditationStatus {
    PENDING(1), CONFIRMED(2), EXPIRED(3), FAILED(4);

    private int code;

    AccreditationStatus(int code) {
        this.code = code;
    }

    public int getCode() {
        return code;
    }

    public static AccreditationStatus valueOf(int i){
        for (AccreditationStatus as : values()){
            if (as.code == i){
                return as;
            }
        }
        throw new IllegalArgumentException("No matching constant for " + i);
    }

    public static AccreditationStatus from(String s){
        for (AccreditationStatus as : values()){
            if (as.name().equals(s)){
                return as;
            }
        }
        throw new IllegalArgumentException("No matching constant for " + s);
    }
}