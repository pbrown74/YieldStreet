package com.yieldstreet.exception;

import com.yieldstreet.dto.AccreditationOutcomeDTO;

public class InvalidOutcomeException extends RuntimeException {

    private String outcome;

    public InvalidOutcomeException(AccreditationOutcomeDTO outcomeDTO){
        this.outcome = outcomeDTO.getOutcome();
    }

    public String getOutcome(){
        return outcome;
    }
}
