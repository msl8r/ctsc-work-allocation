package uk.gov.hmcts.reform.workallocation.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import net.serenitybdd.junit.runners.SerenityRunner;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.test.util.ReflectionTestUtils;
import uk.gov.hmcts.reform.workallocation.ccd.CcdClient;
import uk.gov.hmcts.reform.workallocation.exception.CcdConnectionException;
import uk.gov.hmcts.reform.workallocation.services.CcdConnectorService;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@RunWith(SerenityRunner.class)
public class CcdConnectorServiceTest {

    @Mock
    private CcdClient ccdClient;

    private CcdConnectorService ccdConnectorService;

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        ccdConnectorService = new CcdConnectorService(ccdClient);
        ReflectionTestUtils.setField(ccdConnectorService, "ctids", "DIVORCE");
        ReflectionTestUtils.setField(ccdConnectorService, "dryRun", true);
    }

    @Test
    public void testDryDivorceRun() throws CcdConnectionException {
        Map<String, Object> result = ccdConnectorService.searchDivorceCases("", "", "", "",
            CcdConnectorService.CASE_TYPE_ID_DIVORCE);
        assertEquals(0, result.get("total"));
        assertTrue(((List)result.get("cases")).isEmpty());

        ReflectionTestUtils.setField(ccdConnectorService, "ctids", "DIVORCE_ExceptionRecord");
        result = ccdConnectorService.searchDivorceCases("", "", "", "",
            CcdConnectorService.CASE_TYPE_ID_DIVORCE_EXCEPTION);
        assertEquals(0, result.get("total"));
        assertTrue(((List)result.get("cases")).isEmpty());

        ReflectionTestUtils.setField(ccdConnectorService, "ctids", "DIVORCE");
        result = ccdConnectorService.searchDivorceEvidenceHandledCases("", "", "", "",
            CcdConnectorService.CASE_TYPE_ID_DIVORCE);
        assertEquals(0, result.get("total"));
        assertTrue(((List)result.get("cases")).isEmpty());
    }

    @Test
    public void testDryProbateGoPRun() throws CcdConnectionException {
        Map<String, Object> result = ccdConnectorService.findProbateCases("", "", "", "",
            CcdConnectorService.PROBATE_CASE_TYPE_ID_GOP);
        assertEquals(0, result.get("total"));
        assertTrue(((List)result.get("cases")).isEmpty());

        result = ccdConnectorService.findProbateCases("", "", "", "",
                CcdConnectorService.PROBATE_CASE_TYPE_ID_CAVEAT);
        assertEquals(0, result.get("total"));
        assertTrue(((List)result.get("cases")).isEmpty());

        result = ccdConnectorService.findProbateCases("", "", "", "",
                CcdConnectorService.PROBATE_CASE_TYPE_ID_BSP_EXCEPTION);
        assertEquals(0, result.get("total"));
        assertTrue(((List)result.get("cases")).isEmpty());
    }

    @Test
    public void testDryFrRun() throws CcdConnectionException {
        Map<String, Object> result = ccdConnectorService.findFinancialRemedyCases("", "", "", "",
            CcdConnectorService.FR_CASE_TYPE);
        assertEquals(0, result.get("total"));
        assertTrue(((List)result.get("cases")).isEmpty());

        result = ccdConnectorService.findFinancialRemedyCases("", "", "", "",
                CcdConnectorService.FR_EXCEPTION_CASE_TYPE);
        assertEquals(0, result.get("total"));
        assertTrue(((List)result.get("cases")).isEmpty());
    }

    @Test
    public void divorceNormalRun() throws CcdConnectionException, IOException {
        ReflectionTestUtils.setField(ccdConnectorService, "dryRun", false);
        when(ccdClient.searchCases(any(), any(), any(), any())).thenReturn(caseSearchResult());
        Map<String, Object> result = ccdConnectorService.searchDivorceCases("", "", "", "",
            CcdConnectorService.CASE_TYPE_ID_DIVORCE);
        assertEquals(1, result.get("total"));
        Assert.assertFalse(((List)result.get("cases")).isEmpty());

        ReflectionTestUtils.setField(ccdConnectorService, "ctids", "DIVORCE_ExceptionRecord");
        when(ccdClient.searchCases(any(), any(), any(), any())).thenReturn(exceptionCaseSearchResult());
        result = ccdConnectorService.searchDivorceCases("", "", "", "",
            CcdConnectorService.CASE_TYPE_ID_DIVORCE_EXCEPTION);
        assertEquals(1, result.get("total"));
        Assert.assertFalse(((List)result.get("cases")).isEmpty());

        ReflectionTestUtils.setField(ccdConnectorService, "ctids", "DIVORCE");
        when(ccdClient.searchCases(any(), any(), any(), any())).thenReturn(evidenceHandledCaseSearchResult());
        result = ccdConnectorService.searchDivorceEvidenceHandledCases("", "", "", "",
            CcdConnectorService.CASE_TYPE_ID_DIVORCE);
        assertEquals(1, result.get("total"));
        Assert.assertFalse(((List)result.get("cases")).isEmpty());
    }

    @Test
    public void probateNormalRun() throws CcdConnectionException, IOException {
        ReflectionTestUtils.setField(ccdConnectorService, "ctids", "GrantOfRepresentation");
        ReflectionTestUtils.setField(ccdConnectorService, "dryRun", false);
        ReflectionTestUtils.setField(ccdConnectorService, "enableProbate", true);
        when(ccdClient.searchCases(any(), any(), any(), any())).thenReturn(probateSearchResult());
        Map<String, Object> result = ccdConnectorService.findProbateCases("", "", "", "",
            CcdConnectorService.PROBATE_CASE_TYPE_ID_GOP);
        assertEquals(1, result.get("total"));
        Assert.assertFalse(((List)result.get("cases")).isEmpty());

        ReflectionTestUtils.setField(ccdConnectorService, "ctids", "Caveat");
        when(ccdClient.searchCases(any(), any(), any(), any())).thenReturn(probateCaveatSearchResult());
        result = ccdConnectorService.findProbateCases("", "", "", "",
            CcdConnectorService.PROBATE_CASE_TYPE_ID_CAVEAT);
        assertEquals(1, result.get("total"));
        Assert.assertFalse(((List)result.get("cases")).isEmpty());

        ReflectionTestUtils.setField(ccdConnectorService, "ctids", "PROBATE_ExceptionRecord");
        when(ccdClient.searchCases(any(), any(), any(), any())).thenReturn(probateExpSearchResult());
        result = ccdConnectorService.findProbateCases("", "", "", "",
            CcdConnectorService.PROBATE_CASE_TYPE_ID_BSP_EXCEPTION);
        assertEquals(1, result.get("total"));
        Assert.assertFalse(((List)result.get("cases")).isEmpty());
    }

    @Test
    public void frNormalRun() throws CcdConnectionException, IOException {
        ReflectionTestUtils.setField(ccdConnectorService, "ctids", "FinancialRemedyMVP2");
        ReflectionTestUtils.setField(ccdConnectorService, "dryRun", false);
        ReflectionTestUtils.setField(ccdConnectorService, "enableProbate", true);
        when(ccdClient.searchCases(any(), any(), any(), any())).thenReturn(frSearchResult());
        Map<String, Object> result = ccdConnectorService.findFinancialRemedyCases("", "", "", "",
            CcdConnectorService.FR_CASE_TYPE);
        assertEquals(1, result.get("total"));
        Assert.assertFalse(((List)result.get("cases")).isEmpty());

        ReflectionTestUtils.setField(ccdConnectorService, "ctids", "FINREM_ExceptionRecord");
        when(ccdClient.searchCases(any(), any(), any(), any())).thenReturn(frExpSearchResult());
        result = ccdConnectorService.findProbateCases("", "", "", "",
            CcdConnectorService.FR_EXCEPTION_CASE_TYPE);
        assertEquals(1, result.get("total"));
        Assert.assertFalse(((List)result.get("cases")).isEmpty());
    }

    //CHECKSTYLE:OFF
    @SuppressWarnings("unchecked")
    private Map<String, Object> caseSearchResult() throws IOException {
        String json = "{\n"
            + "\"total\": 1,\n"
            + "  \"cases\": [\n"
            + "  {\n"
            + "    \"id\": 1563460551495313,\n"
            + "    \"jurisdiction\": \"DIVORCE\",\n"
            + "    \"state\": \"Submitted\",\n"
            + "    \"version\": null,\n"
            + "    \"case_type_id\": \"DIVORCE\",\n"
            + "    \"created_date\": \"2019-07-18T14:35:51.473\",\n"
            + "    \"last_modified\": \"2019-07-18T14:36:25.862\",\n"
            + "    \"security_classification\": \"PUBLIC\"\n"
            + "  }\n"
            + "]\n"
            + "}";
        return new ObjectMapper().readValue(json, Map.class);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> exceptionCaseSearchResult() throws IOException {
        String json = "{\n"
                + "\"total\": 1,\n"
                + "  \"cases\": [\n"
                + "  {\n"
                + "    \"id\": 1563460551495313,\n"
                + "    \"jurisdiction\": \"DIVORCE\",\n"
                + "    \"state\": \"ScannedRecordReceived\",\n"
                + "    \"version\": null,\n"
                + "    \"case_type_id\": \"DIVORCE_ExceptionRecord\",\n"
                + "    \"created_date\": \"2019-07-18T14:35:51.473\",\n"
                + "    \"last_modified\": \"2019-07-18T14:36:25.862\",\n"
                + "    \"security_classification\": \"PUBLIC\"\n"
                + "  }\n"
                + "]\n"
                + "}";
        return new ObjectMapper().readValue(json, Map.class);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> evidenceHandledCaseSearchResult() throws IOException {
        String json = "{\n"
                + "\"total\": 1,\n"
                + "  \"cases\": [\n"
                + "  {\n"
                + "    \"id\": 1563460551494444,\n"
                + "    \"jurisdiction\": \"DIVORCE\",\n"
                + "    \"state\": null,\n"
                + "    \"version\": null,\n"
                + "    \"case_type_id\": \"DIVORCE\",\n"
                + "    \"created_date\": \"2019-07-18T14:35:51.473\",\n"
                + "    \"last_modified\": \"2019-07-18T14:36:25.862\",\n"
                + "    \"security_classification\": \"PUBLIC\"\n"
                + "  }\n"
                + "]\n"
                + "}";
        return new ObjectMapper().readValue(json, Map.class);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> probateSearchResult() throws IOException {
        String json = "{\n"
            + "\"total\": 1,\n"
            + "  \"cases\": [\n"
            + "  {\n"
            + "     \"id\": 1572038226693576,\n"
            + "     \"jurisdiction\": \"PROBATE\",\n"
            + "     \"state\": \"BOReadyForExamination\",\n"
            + "     \"version\": null,\n"
            + "     \"case_type_id\": null,\n"
            + "     \"created_date\": null,\n"
            + "     \"last_modified\": \"2019-10-25T21:24:18.143\",\n"
            + "     \"security_classification\": null,\n"
            + "     \"case_data\":{\n"
            + "         \"evidenceHandled\": \"Yes\",\n"
            + "         \"applicationType\": \"Personal\"\n"
            + "     },\n"
            + "     \"data_classification\": null,\n"
            + "     \"after_submit_callback_response\": null,\n"
            + "     \"callback_response_status_code\": null,\n"
            + "     \"callback_response_status\": null,\n"
            + "     \"delete_draft_response_status_code\": null,\n"
            + "     \"delete_draft_response_status\": null,\n"
            + "     \"security_classifications\": null\n"
            + "     }\n"
            + "]\n"
            + "}";
        return new ObjectMapper().readValue(json, Map.class);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> probateCaveatSearchResult() throws IOException {
        String json = "{\n"
            + "\"total\": 1,\n"
            + "  \"cases\": [\n"
            + "  {\n"
            + "     \"id\": 1572044446693576,\n"
            + "     \"jurisdiction\": \"PROBATE\",\n"
            + "     \"state\": \"CaveatRaised\",\n"
            + "     \"version\": null,\n"
            + "     \"case_type_id\": null,\n"
            + "     \"created_date\": null,\n"
            + "     \"last_modified\": \"2019-10-25T21:24:18.143\",\n"
            + "     \"security_classification\": null,\n"
            + "     \"case_data\":{\n"
            + "         \"evidenceHandled\": \"Yes\",\n"
            + "         \"applicationType\": \"Personal\"\n"
            + "     },\n"
            + "     \"data_classification\": null,\n"
            + "     \"after_submit_callback_response\": null,\n"
            + "     \"callback_response_status_code\": null,\n"
            + "     \"callback_response_status\": null,\n"
            + "     \"delete_draft_response_status_code\": null,\n"
            + "     \"delete_draft_response_status\": null,\n"
            + "     \"security_classifications\": null\n"
            + "     }\n"
            + "]\n"
            + "}";
        return new ObjectMapper().readValue(json, Map.class);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> probateExpSearchResult() throws IOException {
        String json = "{\n"
            + "\"total\": 1,\n"
            + "  \"cases\": [\n"
            + "  {\n"
            + "     \"id\": 1572038555593576,\n"
            + "     \"jurisdiction\": \"PROBATE\",\n"
            + "     \"state\": \"ScannedRecordReceived\",\n"
            + "     \"version\": null,\n"
            + "     \"case_type_id\": null,\n"
            + "     \"created_date\": null,\n"
            + "     \"last_modified\": \"2019-10-25T21:24:18.143\",\n"
            + "     \"security_classification\": null,\n"
            + "     \"case_data\":{\n"
            + "         \"containsPayments\": \"No\",\n"
            + "         \"applicationType\": \"Personal\"\n"
            + "     },\n"
            + "     \"data_classification\": null,\n"
            + "     \"after_submit_callback_response\": null,\n"
            + "     \"callback_response_status_code\": null,\n"
            + "     \"callback_response_status\": null,\n"
            + "     \"delete_draft_response_status_code\": null,\n"
            + "     \"delete_draft_response_status\": null,\n"
            + "     \"security_classifications\": null\n"
            + "     }\n"
            + "]\n"
            + "}";
        return new ObjectMapper().readValue(json, Map.class);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> frSearchResult() throws IOException {
        String json = "{\n"
            + "\"total\": 1,\n"
            + "  \"cases\": [\n"
            + "  {\n"
            + "     \"id\": 1544448226693576,\n"
            + "     \"jurisdiction\": \"DIVORCE\",\n"
            + "     \"state\": \"ConsentAppCreated\",\n"
            + "     \"version\": null,\n"
            + "     \"case_type_id\": null,\n"
            + "     \"created_date\": null,\n"
            + "     \"last_modified\": \"2019-10-25T21:24:18.143\",\n"
            + "     \"security_classification\": null,\n"
            + "     \"case_data\":{\n"
            + "         \"evidenceHandled\": \"Yes\",\n"
            + "         \"applicationType\": \"Personal\"\n"
            + "     },\n"
            + "     \"data_classification\": null,\n"
            + "     \"after_submit_callback_response\": null,\n"
            + "     \"callback_response_status_code\": null,\n"
            + "     \"callback_response_status\": null,\n"
            + "     \"delete_draft_response_status_code\": null,\n"
            + "     \"delete_draft_response_status\": null,\n"
            + "     \"security_classifications\": null\n"
            + "     }\n"
            + "]\n"
            + "}";
        return new ObjectMapper().readValue(json, Map.class);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> frExpSearchResult() throws IOException {
        String json = "{\n"
            + "\"total\": 1,\n"
            + "  \"cases\": [\n"
            + "  {\n"
            + "     \"id\": 1555558555593576,\n"
            + "     \"jurisdiction\": \"DIVORCE\",\n"
            + "     \"state\": \"ScannedRecordReceived\",\n"
            + "     \"version\": null,\n"
            + "     \"case_type_id\": null,\n"
            + "     \"created_date\": null,\n"
            + "     \"last_modified\": \"2019-10-25T21:24:18.143\",\n"
            + "     \"security_classification\": null,\n"
            + "     \"case_data\":{\n"
            + "         \"containsPayments\": \"No\",\n"
            + "         \"applicationType\": \"Personal\"\n"
            + "     },\n"
            + "     \"data_classification\": null,\n"
            + "     \"after_submit_callback_response\": null,\n"
            + "     \"callback_response_status_code\": null,\n"
            + "     \"callback_response_status\": null,\n"
            + "     \"delete_draft_response_status_code\": null,\n"
            + "     \"delete_draft_response_status\": null,\n"
            + "     \"security_classifications\": null\n"
            + "     }\n"
            + "]\n"
            + "}";
        return new ObjectMapper().readValue(json, Map.class);
    }
    //CHECKSTYLE:ON
}
