package com.yieldstreet.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A springboot class which allows us to decouple the throwing of exceptions in the service layers
 * to the mapping to HTTP response codes here.
 */
@ControllerAdvice
class GlobalControllerExceptionHandler {
    private static Logger logger = LoggerFactory.getLogger(GlobalControllerExceptionHandler.class);

    @ResponseStatus(HttpStatus.NOT_FOUND)
    @ExceptionHandler(AccreditationNotFoundException.class)
    public ResponseEntity<String> handleAccreditationNotFound(AccreditationNotFoundException ex) {
        logger.error("Accreditation not found: "+ ex.getId());
        return new ResponseEntity<>(ex.getId(), HttpStatus.NOT_FOUND);
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(InvalidIdFormatException.class)
    public ResponseEntity<String> handleInvalidIdFormatException(InvalidIdFormatException ex) {
        logger.error("Accreditation bad format: "+ ex.getId());
        return new ResponseEntity<>(ex.getId(), HttpStatus.BAD_REQUEST);
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(InvalidMimeTypeException.class)
    public ResponseEntity<String> handleInvalidMimeTypeException(InvalidMimeTypeException ex) {
        logger.error("Invalid MIME type: "+ ex.getMimeType());
        return new ResponseEntity<>(ex.getMimeType(), HttpStatus.BAD_REQUEST);
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(InvalidOutcomeException.class)
    public ResponseEntity<String> handleInvalidOutcomeException(InvalidOutcomeException ex) {
        logger.error("Invalid outcome: "+ ex.getOutcome());
        return new ResponseEntity<>(ex.getOutcome(), HttpStatus.BAD_REQUEST);
    }

    @ResponseStatus(HttpStatus.METHOD_NOT_ALLOWED)
    @ExceptionHandler(InvalidStateChangeException.class)
    public ResponseEntity<String> handleInvalidStateChangeException(InvalidStateChangeException ex) {
        logger.error("Invalid status change: "+ ex.getChange());
        return new ResponseEntity<>(ex.getChange(), HttpStatus.METHOD_NOT_ALLOWED);
    }

    @ResponseStatus(HttpStatus.METHOD_NOT_ALLOWED)
    @ExceptionHandler(InvalidInitialStateException.class)
    public ResponseEntity<String> handleInvalidInitialStateException(InvalidInitialStateException ex) {
        logger.error("Invalid state for accreditation: "+ ex.getAccreditationId());
        return new ResponseEntity<>(ex.getAccreditationId(), HttpStatus.METHOD_NOT_ALLOWED);
    }

}