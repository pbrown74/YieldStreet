package com.yieldstreet.entity;

public enum AccreditationType {
    BY_INCOME(1), BY_NET_WORTH(2);

    private int code;

    AccreditationType(int code) {
        this.code = code;
    }

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public static AccreditationType valueOf(int i){
        for (AccreditationType s : values()){
            if (s.code == i){
                return s;
            }
        }
        throw new IllegalArgumentException("No matching constant for " + i);
    }
}