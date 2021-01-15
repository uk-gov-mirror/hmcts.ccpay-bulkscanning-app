package uk.gov.hmcts.reform.bulkscanning.model.request;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.*;
import java.util.Arrays;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

@JsonNaming(PropertyNamingStrategy.SnakeCaseStrategy.class)
@Builder(builderMethodName = "createBSPaymentRequestWith")
@AllArgsConstructor
@NoArgsConstructor
@Data
@JsonInclude(NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class BulkScanPaymentRequest {

    @JsonProperty("ccd_case_number")
    @NotBlank(message = "ccd_case_number can't be Blank")
    @Pattern(regexp="-?\\d+(\\.\\d+)?", message = "ccd_case_number should be numeric")
    @Size(min = 16, max = 16, message = "ccd_case_number length must be 16 digits")
    private String ccdCaseNumber;

    @JsonProperty("is_exception_record")
    @NotNull(message = "is_exception_record flag can't be Blank")
    private Boolean isExceptionRecord;

    @JsonProperty("document_control_numbers")
    @NotEmpty(message = "document_control_numbers can't be Blank")
    private String[] documentControlNumbers;

    @JsonIgnore
    @AssertFalse(message = "document_control_number must be 21 digit numeric")
    public boolean isValidDocumentControlNumbers() {
        return documentControlNumbers != null
            && (Arrays.asList(documentControlNumbers).stream().anyMatch(dcn -> dcn.length() != 21)
            || Arrays.asList(documentControlNumbers).stream().anyMatch(dcn -> ! dcn.matches("-?\\d+(\\.\\d+)?")));
    }
}
