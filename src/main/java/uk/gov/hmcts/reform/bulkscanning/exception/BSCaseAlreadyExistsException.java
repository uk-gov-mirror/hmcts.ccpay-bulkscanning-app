package uk.gov.hmcts.reform.bulkscanning.exception;

public class BSCaseAlreadyExistsException extends RuntimeException{
    public BSCaseAlreadyExistsException(String message) {
        super(message);
    }
}
