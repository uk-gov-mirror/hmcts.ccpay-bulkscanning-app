package uk.gov.hmcts.reform.bulkscanning.functionaltest;

import org.apache.commons.lang.StringUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.reform.bulkscanning.config.IdamService;
import uk.gov.hmcts.reform.bulkscanning.config.TestConfigProperties;
import uk.gov.hmcts.reform.bulkscanning.config.TestContextConfiguration;
import uk.gov.hmcts.reform.bulkscanning.config.S2sTokenService;
import uk.gov.hmcts.reform.bulkscanning.model.entity.Envelope;
import uk.gov.hmcts.reform.bulkscanning.model.repository.EnvelopeRepository;
import uk.gov.hmcts.reform.bulkscanning.model.repository.PaymentRepository;
import uk.gov.hmcts.reform.bulkscanning.model.request.BulkScanPaymentRequest;
import uk.gov.hmcts.reform.bulkscanning.model.request.CaseReferenceRequest;
import uk.gov.hmcts.reform.bulkscanning.service.PaymentService;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;

import static org.springframework.http.HttpStatus.OK;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static uk.gov.hmcts.reform.bulkscanning.controller.PaymentControllerTest.createPaymentRequest;
import static uk.gov.hmcts.reform.bulkscanning.config.IdamService.CMC_CITIZEN_GROUP;
import static uk.gov.hmcts.reform.bulkscanning.model.enums.PaymentStatus.COMPLETE;
import static uk.gov.hmcts.reform.bulkscanning.model.enums.PaymentStatus.INCOMPLETE;
import static uk.gov.hmcts.reform.bulkscanning.utils.BulkScanningConstants.*;
import static uk.gov.hmcts.reform.bulkscanning.utils.BulkScanningUtils.asJsonString;


@RunWith(SpringRunner.class)
@SpringBootTest
@EnableFeignClients
@AutoConfigureMockMvc
@ContextConfiguration(classes = TestContextConfiguration.class)
@ActiveProfiles("local")
@TestPropertySource(locations="classpath:application-local.yaml")
public class PaymentControllerFunctionalTest {

    @Autowired
    MockMvc mvc;

    @Autowired
    PaymentService bulkScanConsumerService;

    BulkScanPaymentRequest bulkScanPaymentRequest;

    CaseReferenceRequest caseReferenceRequest;

    @Autowired
    PaymentRepository paymentRepository;

    @Autowired
    EnvelopeRepository envelopeRepository;

    @Autowired
    private TestConfigProperties testProps;

    @Autowired
    private IdamService idamService;

    @Autowired
    private S2sTokenService s2sTokenService;

    private static String USER_TOKEN;
    private static String SERVICE_TOKEN;
    private static boolean TOKENS_INITIALIZED;

    @Before
    public void setUp() {
        caseReferenceRequest = CaseReferenceRequest
            .createCaseReferenceRequest()
            .ccdCaseNumber("CCN2")
            .build();

        if (!TOKENS_INITIALIZED) {
            USER_TOKEN = idamService.createUserWith(CMC_CITIZEN_GROUP, "citizen").getAuthorisationToken();
            SERVICE_TOKEN = s2sTokenService.getS2sToken(testProps.s2sServiceName, testProps.s2sServiceSecret);
            TOKENS_INITIALIZED = true;
        }
    }

    @Test
    public void testBulkScanningPaymentRequestFirst() throws Exception{
        String dcn[] = {"DCN2"};
        BulkScanPaymentRequest bulkScanPaymentRequest = createBulkScanPaymentRequest("1111-2222-3333-5555"
            ,dcn,"AA08", true);

        //Post request
        ResultActions resultActions = mvc.perform(post("/bulk-scan-payments")
            .header("ServiceAuthorization", SERVICE_TOKEN)
            .content(asJsonString(bulkScanPaymentRequest))
            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isCreated())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));

        Assert.assertNotNull(resultActions.andReturn().getResponse().getContentAsString());

        //Post Repeat request
        ResultActions repeatRequest = mvc.perform(post("/bulk-scan-payments")
            .header("ServiceAuthorization", SERVICE_TOKEN)
            .content(asJsonString(bulkScanPaymentRequest))
            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isConflict())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));

        Assert.assertTrue(StringUtils.containsIgnoreCase(
            repeatRequest.andReturn().getResponse().getContentAsString(),
            BULK_SCANNING_PAYMENT_DETAILS_ALREADY_EXIST
        ));

        //PATCH Request
        ResultActions patchRequest = mvc.perform(patch("/bulk-scan-payments/DCN2/status/PROCESSED")
             .header("Authorization", USER_TOKEN)
             .header("ServiceAuthorization", SERVICE_TOKEN)
            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(content()
                .contentTypeCompatibleWith(MediaType.APPLICATION_JSON));

        Assert.assertNotNull(patchRequest.andReturn().getResponse().getContentAsString());

        //DCN Not exists Request
        ResultActions patchDCNNotExists = mvc.perform(patch("/bulk-scan-payments/DCN4/status/PROCESSED")
            .header("Authorization", USER_TOKEN)
            .header("ServiceAuthorization", SERVICE_TOKEN)
            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isNotFound())
            .andExpect(content()
                .contentTypeCompatibleWith(MediaType.APPLICATION_JSON));

        Assert.assertTrue(StringUtils.containsIgnoreCase(patchDCNNotExists.andReturn().getResponse().getContentAsString(),
            DCN_NOT_EXISTS));
    }

    @Test
    @Transactional
    public void testUpdateCaseReferenceForExceptionRecord() throws Exception {
        String dcn[] = {"DCN5"};
        String dcn2[] = {"DCN6"};

        //Multiple envelopes with same exception record
        bulkScanPaymentRequest = createBulkScanPaymentRequest("1111-2222-3333-4444"
            , dcn, "AA08", true);
        bulkScanConsumerService.saveInitialMetadataFromBs(bulkScanPaymentRequest);

        bulkScanPaymentRequest = createBulkScanPaymentRequest("1111-2222-3333-4444"
            , dcn2, "AA08", true);
        bulkScanConsumerService.saveInitialMetadataFromBs(bulkScanPaymentRequest);

        ResultActions resultActions = mvc.perform(put("/bulk-scan-payments/?exception_reference=1111-2222-3333-4444")
            .header("Authorization", USER_TOKEN)
            .header("ServiceAuthorization", SERVICE_TOKEN)
            .content(asJsonString(caseReferenceRequest))
            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(content()
                .contentTypeCompatibleWith(MediaType.APPLICATION_JSON));

        Assert.assertNotNull(resultActions.andReturn().getResponse().getContentAsString());

    }

    @Test
    @Transactional
    public void testExceptionRecordNotExists() throws Exception {

        ResultActions resultActions = mvc.perform(put("/bulk-scan-payments/?exception_reference=4444-3333-2222-111")
            .header("Authorization", USER_TOKEN)
            .header("ServiceAuthorization", SERVICE_TOKEN)
            .content(asJsonString(caseReferenceRequest))
            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isNotFound())
            .andExpect(content()
                .contentTypeCompatibleWith(MediaType.APPLICATION_JSON));

        Assert.assertTrue(StringUtils.containsIgnoreCase(resultActions.andReturn().getResponse().getContentAsString(),
            EXCEPTION_RECORD_NOT_EXISTS));
    }

    @Test
    @Transactional
    public void testMarkPaymentAsProcessed() throws Exception {
        String dcn[] = {"DCN1"};
        bulkScanPaymentRequest = createBulkScanPaymentRequest("1111-2222-3333-4444"
            , dcn, "AA08", false);
        bulkScanConsumerService.saveInitialMetadataFromBs(bulkScanPaymentRequest);

        ResultActions resultActions = mvc.perform(patch("/bulk-scan-payments/DCN1/status/PROCESSED")
            .header("Authorization", USER_TOKEN)
            .header("ServiceAuthorization", SERVICE_TOKEN)
            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(content()
                .contentTypeCompatibleWith(MediaType.APPLICATION_JSON));

        Assert.assertEquals(resultActions.andReturn().getResponse().getStatus(), OK.value());
    }

    @Test
    public void testMatchingPaymentsFromExcelaBulkScan() throws Exception {

        //Request from Exela with one DCN
        String dcn[] = {"1111-2222-4444-5555"};
        mvc.perform(post("/bulk-scan-payment")
            .header("ServiceAuthorization", SERVICE_TOKEN)
            .content(asJsonString(createPaymentRequest("1111-2222-4444-5555")))
            .contentType(MediaType.APPLICATION_JSON));

        //Request from bulk scan with one DCN
        BulkScanPaymentRequest bulkScanPaymentRequest = createBulkScanPaymentRequest("1111-2222-3333-4444"
            , dcn, "AA08", true);

        //Post request
        mvc.perform(post("/bulk-scan-payments")
            .header("ServiceAuthorization", SERVICE_TOKEN)
            .content(asJsonString(bulkScanPaymentRequest))
            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isCreated())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));

        //Complete payment
        Assert.assertEquals(COMPLETE.toString(), paymentRepository.findByDcnReference("1111-2222-4444-5555").get().getPaymentStatus());

        //Complete envelope
        Envelope finalEnvelope = envelopeRepository.findAll().iterator().next();
        Assert.assertEquals(COMPLETE.toString(), finalEnvelope.getPaymentStatus());
    }


    @Test
    public void testNonMatchingPaymentsFromExelaThenBulkScan() throws Exception {

        //Request from Exela with one DCN
        String dcn[] = {"1111-2222-3333-6666", "1111-2222-3333-7777"};
        mvc.perform(post("/bulk-scan-payment")
            .header("ServiceAuthorization", SERVICE_TOKEN)
            .content(asJsonString(createPaymentRequest("1111-2222-3333-6666")))
            .contentType(MediaType.APPLICATION_JSON));

        //Request from bulk scan with two DCN
        BulkScanPaymentRequest bulkScanPaymentRequest = createBulkScanPaymentRequest("1111-2222-3333-4444"
            , dcn, "AA08", true);

        //Post request
        mvc.perform(post("/bulk-scan-payments")
            .header("ServiceAuthorization", SERVICE_TOKEN)
            .content(asJsonString(bulkScanPaymentRequest))
            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isCreated())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));

        //Complete payment
        Assert.assertEquals(paymentRepository.findByDcnReference("1111-2222-3333-6666").get().getPaymentStatus()
            , COMPLETE.toString());

        //Non Complete Payment
        Assert.assertEquals(paymentRepository.findByDcnReference("1111-2222-3333-7777").get().getPaymentStatus()
            , INCOMPLETE.toString());
    }


    @Test
    public void testMatchingBulkScanFirstThenExela() throws Exception {
        //Request from Bulk Scan with one DCN
        String dcn[] = {"1111-2222-3333-8888", "1111-2222-3333-9999"};

        //Request from bulk scan with two DCN
        BulkScanPaymentRequest bulkScanPaymentRequest = createBulkScanPaymentRequest("1111-2222-3333-4444"
            , dcn, "AA08", true);

        //Post request
        mvc.perform(post("/bulk-scan-payments")
            .header("ServiceAuthorization", SERVICE_TOKEN)
            .content(asJsonString(bulkScanPaymentRequest))
            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isCreated())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));


        mvc.perform(post("/bulk-scan-payment")
            .header("ServiceAuthorization", SERVICE_TOKEN)
            .content(asJsonString(createPaymentRequest("1111-2222-3333-8888")))
            .contentType(MediaType.APPLICATION_JSON));


        //Complete payment
        Assert.assertEquals(paymentRepository.findByDcnReference("1111-2222-3333-8888").get().getPaymentStatus()
            , COMPLETE.toString());

        //Non Complete Payment
        Assert.assertEquals(paymentRepository.findByDcnReference("1111-2222-3333-9999").get().getPaymentStatus()
            , INCOMPLETE.toString());

    }

    public static BulkScanPaymentRequest createBulkScanPaymentRequest(String ccdCaseNumber, String[] dcn, String responsibleServiceId, boolean isExceptionRecord) {
        return BulkScanPaymentRequest
            .createBSPaymentRequestWith()
            .ccdCaseNumber(ccdCaseNumber)
            .documentControlNumbers(dcn)
            .responsibleServiceId(responsibleServiceId)
            .isExceptionRecord(isExceptionRecord)
            .build();
    }

    @Test
    public void testGeneratePaymentReport_Unprocessed() throws Exception {

        String dcn[] = {"11112222333344441", "11112222333344442"};
        String ccd = "1111222233334444";
        createTestReportData(ccd, dcn);
        ResultActions resultActions = mvc.perform(get("/report/download")
                                                      .header("ServiceAuthorization", "service")
                                                      .param(
                                                          "date_from",
                                                          getReportDate(new Date(System.currentTimeMillis() - 24 * 60 * 60 * 1000L))
                                                      )
                                                      .param(
                                                          "date_to",
                                                          getReportDate(new Date(System.currentTimeMillis() + 24 * 60 * 60 * 1000L))
                                                      )
                                                      .param("report_type", "UNPROCESSED")
                                                      .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk());
        Assert.assertEquals(200, resultActions.andReturn().getResponse().getStatus());
    }

    @Test
    public void testGeneratePaymentReport_DataLoss() throws Exception {
        String dcn[] = {"11112222333355551", "11112222333355552"};
        String ccd = "1111222233335555";
        createTestReportData(ccd, dcn);
        ResultActions resultActions = mvc.perform(get("/report/download")
                                                      .header("ServiceAuthorization", "service")
                                                      .param(
                                                          "date_from",
                                                          getReportDate(new Date(System.currentTimeMillis() - 24 * 60 * 60 * 1000L))
                                                      )
                                                      .param(
                                                          "date_to",
                                                          getReportDate(new Date(System.currentTimeMillis() + 24 * 60 * 60 * 1000L))
                                                      )
                                                      .param("report_type", "DATA_LOSS")
                                                      .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk());
        Assert.assertEquals(200, resultActions.andReturn().getResponse().getStatus());
    }

    private void createTestReportData(String ccd, String... dcns) throws Exception {
        //Request from Exela with one DCN

        mvc.perform(post("/bulk-scan-payment")
                        .header("ServiceAuthorization", "service")
                        .content(asJsonString(createPaymentRequest(dcns[0])))
                        .contentType(MediaType.APPLICATION_JSON));

        //Request from bulk scan with one DCN
        BulkScanPaymentRequest bulkScanPaymentRequest = createBulkScanPaymentRequest(ccd
            , dcns, "AA08", true);

        //Post request
        mvc.perform(post("/bulk-scan-payments")
                        .header("ServiceAuthorization", "service")
                        .content(asJsonString(bulkScanPaymentRequest))
                        .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isCreated())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));
    }

    private String getReportDate(Date date) {
        DateTimeFormatter reportNameDateFormat = DateTimeFormatter.ofPattern("MM/dd/yyyy");
        return dateToLocalDateTime(date).format(reportNameDateFormat);
    }

    private LocalDateTime dateToLocalDateTime(Date date) {
        return date == null ? null : LocalDateTime.ofInstant(date.toInstant(), ZoneId.systemDefault());
    }
}
