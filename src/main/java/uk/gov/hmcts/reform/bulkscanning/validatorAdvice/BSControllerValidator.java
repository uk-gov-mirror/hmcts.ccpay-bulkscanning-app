package uk.gov.hmcts.reform.bulkscanning.validatorAdvice;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
import uk.gov.hmcts.reform.bulkscanning.exception.BSCaseAlreadyExistsException;

@ControllerAdvice(basePackages = "uk.gov.hmcts.reform.bulkscanning.controller")
public class BSControllerValidator extends
    ResponseEntityExceptionHandler {

    @ExceptionHandler(BSCaseAlreadyExistsException.class)
    public ResponseEntity bsPaymentAlreadyExists(BSCaseAlreadyExistsException ex) {
        return new ResponseEntity(ex.getMessage(), HttpStatus.ALREADY_REPORTED);
    }
}
