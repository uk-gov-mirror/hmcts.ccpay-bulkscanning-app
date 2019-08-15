package uk.gov.hmcts.reform.bulkscanning.controller;

import io.swagger.annotations.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import uk.gov.hmcts.reform.bulkscanning.exception.BSCaseAlreadyExistsException;
import uk.gov.hmcts.reform.bulkscanning.model.dto.BSPaymentRequest;
import uk.gov.hmcts.reform.bulkscanning.service.BSConsumerService;

import javax.validation.Valid;

@RestController
@Api(tags = {"BulkScanning"})
@SwaggerDefinition(tags = {@Tag(name = "BSConsumerController", description = "Bulk Scanning Consumer API")})
public class BSConsumerController {

    @Autowired
    BSConsumerService bsConsumerService;

    @ApiOperation(value = "Get the initial meta data from bulk Scanning",
    notes = "Get the initial meta data from bulk Scanning")
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Bulk Scanning Data retrieved"),
        @ApiResponse(code = 400, message = "Bad request"),
        @ApiResponse(code = 401, message = "Failed authentication"),
        @ApiResponse(code = 403, message = "Failed authorization")
    })
    @PostMapping("/bulk-scan-payment")
    public ResponseEntity consumeInitialMetaDataBulkScanning (@Valid @RequestBody BSPaymentRequest bsPaymentRequest) {
        bsConsumerService.saveInitialMetadataFromBS(bsPaymentRequest);
        return new ResponseEntity(HttpStatus.CREATED);
    }


}
