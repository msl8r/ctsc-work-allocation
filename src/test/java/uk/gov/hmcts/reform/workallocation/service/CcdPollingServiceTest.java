package uk.gov.hmcts.reform.workallocation.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.applicationinsights.TelemetryClient;
import net.serenitybdd.junit.runners.SerenityRunner;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import uk.gov.hmcts.reform.workallocation.exception.CcdConnectionException;
import uk.gov.hmcts.reform.workallocation.idam.IdamConnectionException;
import uk.gov.hmcts.reform.workallocation.idam.IdamService;
import uk.gov.hmcts.reform.workallocation.model.Task;
import uk.gov.hmcts.reform.workallocation.queue.DeadQueueConsumer;
import uk.gov.hmcts.reform.workallocation.queue.QueueConsumer;
import uk.gov.hmcts.reform.workallocation.queue.QueueProducer;
import uk.gov.hmcts.reform.workallocation.services.CcdConnectorService;
import uk.gov.hmcts.reform.workallocation.services.CcdPollingService;
import uk.gov.hmcts.reform.workallocation.services.LastRunTimeService;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(SerenityRunner.class)
public class CcdPollingServiceTest {

    private CcdPollingService ccdPollingService;

    @Mock
    private IdamService idamService;

    @Mock
    private LastRunTimeService lastRunTimeService;

    @Mock
    private QueueProducer<Task> queueProducer;

    @Mock
    private QueueConsumer<Task> queueConsumer;

    @Mock
    private DeadQueueConsumer deadQueueConsumer;

    @Mock
    private TelemetryClient telemetryClient;

    @Mock
    private CcdConnectorService ccdConnectorService;

    @Before
    public void setup() throws IOException, IdamConnectionException, CcdConnectionException {
        MockitoAnnotations.openMocks(this);
        ccdPollingService = new CcdPollingService(idamService, ccdConnectorService, lastRunTimeService,
            30, 5, queueProducer,
            queueConsumer, deadQueueConsumer, telemetryClient);

        when(deadQueueConsumer.runConsumer(any(), any())).thenReturn(CompletableFuture.completedFuture(null));
        when(queueConsumer.runConsumer(any(), any())).thenReturn(CompletableFuture.completedFuture(null));

        Map<String, Object> divorceResponse = divorceSearchResult();
        divorceResponse.put("case_type_id", "DIVORCE");
        when(ccdConnectorService.searchDivorceCases(anyString(), anyString(), anyString(), anyString(), eq("DIVORCE")))
            .thenReturn(divorceResponse);

        Map<String, Object> divorceExceptionResponse = divorceExceptionSearchResult();
        divorceExceptionResponse.put("case_type_id", "DIVORCE_ExceptionRecord");
        when(ccdConnectorService.searchDivorceCases(anyString(), anyString(), anyString(), anyString(),
                eq("DIVORCE_ExceptionRecord"))).thenReturn(divorceExceptionResponse);

        Map<String, Object> divorceEvidenceHandResponse = divorceEvidenceHandSearchResult();
        divorceEvidenceHandResponse.put("case_type_id", "DIVORCE");
        divorceEvidenceHandResponse.put("EVIDENCE_FLOW", "evidenceHandled");
        when(ccdConnectorService.searchDivorceEvidenceHandledCases(anyString(), anyString(), anyString(), anyString(),
                eq("DIVORCE"))).thenReturn(divorceEvidenceHandResponse);

        Map<String, Object> probateResponse = probateSearchResult();
        probateResponse.put("case_type_id", "GrantOfRepresentation");
        when(ccdConnectorService.findProbateCases(anyString(), anyString(), anyString(), anyString(),
                eq("GrantOfRepresentation"))).thenReturn(probateResponse);

        Map<String, Object> probateCaveatResponse = probateCaveatSearchResult();
        probateCaveatResponse.put("case_type_id", "Caveat");
        when(ccdConnectorService.findProbateCases(anyString(), anyString(), anyString(), anyString(),
                eq("Caveat"))).thenReturn(probateCaveatResponse);

        Map<String, Object> probateExpResponse = probateExpSearchResult();
        probateExpResponse.put("case_type_id", "PROBATE_ExceptionRecord");
        when(ccdConnectorService.findProbateCases(anyString(), anyString(), anyString(), anyString(),
                eq("PROBATE_ExceptionRecord"))).thenReturn(probateExpResponse);

        Map<String, Object> frResponse = frSearchResult();
        frResponse.put("case_type_id", "FinancialRemedyMVP2");
        when(ccdConnectorService.findFinancialRemedyCases(anyString(), anyString(), anyString(), anyString(),
                eq("FinancialRemedyMVP2"))).thenReturn(frResponse);

        Map<String, Object> frExpResponse = frExpSearchResult();
        frExpResponse.put("case_type_id", "FINREM_ExceptionRecord");
        when(ccdConnectorService.findFinancialRemedyCases(anyString(), anyString(), anyString(), anyString(),
                eq("FINREM_ExceptionRecord"))).thenReturn(frExpResponse);

        when(idamService.generateServiceAuthorization()).thenReturn("service_token");
        when(idamService.getIdamOauth2Token()).thenReturn("idam_token");
        when(lastRunTimeService.getMinDate()).thenReturn(LocalDateTime.of(2019, 9, 20, 12, 0, 0, 0));
    }

    @Test
    public void testPollccdEndpoint() throws CcdConnectionException, IdamConnectionException {
        when(lastRunTimeService.getLastRunTime()).thenReturn(Optional.of(LocalDateTime.of(2019, 9, 25, 12, 0, 0, 0)));

        ccdPollingService.pollCcdEndpoint();
        String queryFromDate = "2019-09-25T11:55";
        Task task1 = getDivorceTask();
        Task task2 = getDivorceExceptionTask();
        Task task3 = getDivorceEvidenceHandledTask();
        Task task4 = getProbateTask();
        Task task5 = getProbateCaveatTask();
        Task task6 = getProbateExceptionTask();
        Task task7 = getFrTask();
        Task task8 = getFrExceptionTask();
        verify(ccdConnectorService, times(2))
            .searchDivorceCases(eq("idam_token"), eq("service_token"), eq(queryFromDate), anyString(), anyString());
        verify(ccdConnectorService, times(1)).searchDivorceEvidenceHandledCases(eq("idam_token"),
                eq("service_token"), eq(queryFromDate), anyString(), anyString());
        verify(ccdConnectorService, times(3)).findProbateCases(eq("idam_token"),
                eq("service_token"), eq(queryFromDate), anyString(), anyString());
        verify(ccdConnectorService, times(2)).findFinancialRemedyCases(eq("idam_token"),
                eq("service_token"), eq(queryFromDate), anyString(), anyString());
        verify(queueProducer, times(1)).placeItemsInQueue(eq(Arrays.asList(task1, task2, task3, task4,
                task5, task6, task7, task8)), any());
        verify(lastRunTimeService, times(1)).updateLastRuntime(any(LocalDateTime.class));
    }

    @Test
    public void testPollccdEndpointFirstTime() throws CcdConnectionException, IdamConnectionException {
        when(lastRunTimeService.getLastRunTime()).thenReturn(Optional.empty());

        ccdPollingService.pollCcdEndpoint();
        String queryDate = "2019-09-20T11:55";
        Task task1 = getDivorceTask();
        Task task2 = getDivorceExceptionTask();
        Task task3 = getDivorceEvidenceHandledTask();
        Task task4 = getProbateTask();
        Task task5 = getProbateCaveatTask();
        Task task6 = getProbateExceptionTask();
        Task task7 = getFrTask();
        Task task8 = getFrExceptionTask();
        verify(ccdConnectorService, times(2))
            .searchDivorceCases(eq("idam_token"), eq("service_token"), eq(queryDate), anyString(), anyString());
        verify(ccdConnectorService, times(1)).searchDivorceEvidenceHandledCases(eq("idam_token"),
                eq("service_token"), eq(queryDate), anyString(), anyString());
        verify(ccdConnectorService, times(3)).findProbateCases(eq("idam_token"),
                eq("service_token"), eq(queryDate), anyString(), anyString());
        verify(ccdConnectorService, times(2)).findFinancialRemedyCases(eq("idam_token"),
                eq("service_token"), eq(queryDate), anyString(), anyString());
        verify(queueProducer, times(1))
                .placeItemsInQueue(eq(Arrays.asList(task1, task2, task3, task4, task5, task6, task7, task8)), any());
        verify(lastRunTimeService, times(1)).updateLastRuntime(any(LocalDateTime.class));
    }

    @Test
    public void testPollccdEndpointWhenQueueConsumerThrowsAnError()
            throws CcdConnectionException, IdamConnectionException {
        CompletableFuture<Void> consumerResponse = new CompletableFuture<>();
        consumerResponse.completeExceptionally(new RuntimeException("Something went wrong"));
        when(lastRunTimeService.getLastRunTime()).thenReturn(Optional.of(LocalDateTime.of(2019, 9, 25, 12, 0, 0, 0)));
        when(queueConsumer.runConsumer(any(), any())).thenReturn(consumerResponse);
        ccdPollingService.pollCcdEndpoint();
        Task task1 = getDivorceTask();
        Task task2 = getDivorceExceptionTask();
        Task task3 = getDivorceEvidenceHandledTask();
        Task task4 = getProbateTask();
        Task task5 = getProbateCaveatTask();
        Task task6 = getProbateExceptionTask();
        Task task7 = getFrTask();
        Task task8 = getFrExceptionTask();
        verify(ccdConnectorService, times(2))
            .searchDivorceCases(eq("idam_token"), eq("service_token"), eq("2019-09-25T11:55"),
                    anyString(), anyString());
        verify(ccdConnectorService, times(1)).searchDivorceEvidenceHandledCases(eq("idam_token"),
                eq("service_token"), eq("2019-09-25T11:55"), anyString(), anyString());
        verify(ccdConnectorService, times(3)).findProbateCases(eq("idam_token"),
                eq("service_token"), eq("2019-09-25T11:55"), anyString(), anyString());
        verify(ccdConnectorService, times(2)).findFinancialRemedyCases(eq("idam_token"),
                eq("service_token"), eq("2019-09-25T11:55"), anyString(), anyString());
        verify(queueProducer, times(1))
                .placeItemsInQueue(eq(Arrays.asList(task1, task2, task3, task4, task5, task6, task7, task8)), any());
        verify(lastRunTimeService, times(1)).updateLastRuntime(any(LocalDateTime.class));
    }

    @Test
    public void testPollccdEndpointWhenDeadQueueConsumerThrowsAnError()
            throws CcdConnectionException, IdamConnectionException {
        CompletableFuture<Void> consumerResponse = new CompletableFuture<>();
        consumerResponse.completeExceptionally(new RuntimeException("Something went wrong"));
        when(lastRunTimeService.getLastRunTime()).thenReturn(Optional.of(LocalDateTime.of(2019, 9, 25, 12, 0, 0, 0)));
        when(deadQueueConsumer.runConsumer(any(), any())).thenReturn(consumerResponse);
        ccdPollingService.pollCcdEndpoint();
        Task task1 = getDivorceTask();
        Task task2 = getDivorceExceptionTask();
        Task task3 = getDivorceEvidenceHandledTask();
        Task task4 = getProbateTask();
        Task task5 = getProbateCaveatTask();
        Task task6 = getProbateExceptionTask();
        Task task7 = getFrTask();
        Task task8 = getFrExceptionTask();
        verify(ccdConnectorService, times(2))
            .searchDivorceCases(eq("idam_token"), eq("service_token"), eq("2019-09-25T11:55"),
                    anyString(), anyString());
        verify(ccdConnectorService, times(1)).searchDivorceEvidenceHandledCases(eq("idam_token"),
                eq("service_token"), eq("2019-09-25T11:55"), anyString(), anyString());
        verify(ccdConnectorService, times(3)).findProbateCases(eq("idam_token"),
                eq("service_token"), eq("2019-09-25T11:55"), anyString(), anyString());
        verify(ccdConnectorService, times(2)).findFinancialRemedyCases(eq("idam_token"),
                eq("service_token"), eq("2019-09-25T11:55"), anyString(), anyString());
        verify(queueProducer, times(1))
                .placeItemsInQueue(eq(Arrays.asList(task1, task2, task3, task4, task5, task6, task7, task8)), any());
        verify(lastRunTimeService, times(1)).updateLastRuntime(any(LocalDateTime.class));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testPollCcdWhenTheResponseIsNotCorrect()
            throws IOException, CcdConnectionException, IdamConnectionException {
        Map<String, Object> searchResult = divorceSearchResult();
        List<Object> cases = (List<Object>) searchResult.get("cases");
        Map<String, Object> ccdCase = (Map<String, Object>) cases.get(0);
        ccdCase.remove("id");
        when(ccdConnectorService.searchDivorceCases(anyString(), anyString(), anyString(), anyString(), anyString()))
            .thenReturn(searchResult);
        searchResult.put("EVIDENCE_FLOW", "evidenceHandled");
        when(ccdConnectorService.searchDivorceEvidenceHandledCases(anyString(), anyString(), anyString(),
                anyString(), anyString())).thenReturn(searchResult);
        ccdPollingService.pollCcdEndpoint();
        Task task1 = getProbateTask();
        Task task2 = getProbateCaveatTask();
        Task task3 = getProbateExceptionTask();
        Task task4 = getFrTask();
        Task task5 = getFrExceptionTask();
        verify(ccdConnectorService, times(2))
            .searchDivorceCases(eq("idam_token"), eq("service_token"), eq("2019-09-20T11:55"),
                    anyString(), anyString());
        verify(ccdConnectorService, times(3)).findProbateCases(eq("idam_token"),
                eq("service_token"), eq("2019-09-20T11:55"), anyString(), anyString());
        verify(ccdConnectorService, times(2)).findFinancialRemedyCases(eq("idam_token"),
                eq("service_token"), eq("2019-09-20T11:55"), anyString(), anyString());
        verify(queueProducer, times(1)).placeItemsInQueue(eq(Arrays.asList(task1, task2, task3, task4, task5)), any());
        verify(lastRunTimeService, times(1)).updateLastRuntime(any(LocalDateTime.class));
    }

    @Test
    public void testWhenLastRunLessThanThirtyMinutes() throws CcdConnectionException, IdamConnectionException {
        when(lastRunTimeService.getMinDate()).thenReturn(LocalDateTime.now().minusMinutes(25L));
        ccdPollingService.pollCcdEndpoint();
        verify(ccdConnectorService, times(0)).searchDivorceCases(any(), any(), any(), any(), any());
        verify(ccdConnectorService, times(0)).findProbateCases(any(), any(), any(), any(), any());
        verify(ccdConnectorService, times(0)).findFinancialRemedyCases(any(), any(), any(), any(), any());
        verify(queueProducer, times(0)).placeItemsInQueue(any(), any());
        verify(lastRunTimeService, times(0)).updateLastRuntime(any(LocalDateTime.class));
    }

    //CHECKSTYLE:OFF
    @SuppressWarnings("unchecked")
    private Map<String, Object> divorceExceptionSearchResult() throws IOException {
        String json = "{\n"
                + "\"total\": 1,\n"
                + "  \"cases\": [\n"
                + "  {\n"
                + "    \"id\": 1563460551495377,\n"
                + "    \"jurisdiction\": \"DIVORCE\",\n"
                + "    \"state\": \"ScannedRecordReceived\",\n"
                + "    \"version\": null,\n"
                + "    \"case_type_id\": null,\n"
                + "    \"created_date\": \"2019-07-18T14:35:51.473\",\n"
                + "    \"last_modified\": \"2019-07-18T14:36:25.862\",\n"
                + "    \"security_classification\": null\n"
                + "  }\n"
                + "]\n"
                + "}";
        return new ObjectMapper().readValue(json, Map.class);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> divorceEvidenceHandSearchResult() throws IOException {
        String json = "{\n"
                + "\"total\": 1,\n"
                + "  \"cases\": [\n"
                + "  {\n"
                + "    \"id\": 1563460551495399,\n"
                + "    \"jurisdiction\": \"DIVORCE\",\n"
                + "    \"state\": null,\n"
                + "    \"version\": null,\n"
                + "    \"case_type_id\": null,\n"
                + "    \"created_date\": \"2019-07-18T14:35:51.473\",\n"
                + "    \"last_modified\": \"2019-07-18T14:36:25.862\",\n"
                + "    \"security_classification\": null\n"
                + "  }\n"
                + "]\n"
                + "}";
        return new ObjectMapper().readValue(json, Map.class);
    }
    //CHECKSTYLE:ON

    @SuppressWarnings("unchecked")
    private Map<String, Object> divorceSearchResult() throws IOException {
        String json = "{\n"
            + "\"total\": 1,\n"
            + "  \"cases\": [\n"
            + "  {\n"
            + "    \"id\": 1563460551495313,\n"
            + "    \"jurisdiction\": \"DIVORCE\",\n"
            + "    \"state\": \"Submitted\",\n"
            + "    \"version\": null,\n"
            + "    \"case_type_id\": null,\n"
            + "    \"created_date\": \"2019-07-18T14:35:51.473\",\n"
            + "    \"last_modified\": \"2019-07-18T14:36:25.862\",\n"
            + "    \"security_classification\": null\n"
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
            + "     \"state\": \"ReadyForExaminationPersonal\",\n"
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

    private Task getDivorceExceptionTask() {
        return Task.builder()
            .caseTypeId("DIVORCE_ExceptionRecord")
            .id("1563460551495377")
            .jurisdiction("DIVORCE")
            .state("ScannedRecordReceived")
            .lastModifiedDate(LocalDateTime.of(2019, 7, 18, 14, 36, 25, 862000000))
            .build();
    }

    private Task getDivorceTask() {
        return Task.builder()
            .caseTypeId("DIVORCE")
            .id("1563460551495313")
            .jurisdiction("DIVORCE")
            .state("Submitted")
            .lastModifiedDate(LocalDateTime.of(2019,7,18,14,36,25, 862000000))
            .build();
    }

    private Task getProbateTask() {
        return Task.builder()
            .caseTypeId("GrantOfRepresentation")
            .id("1572038226693576")
            .jurisdiction("PROBATE")
            .state("ReadyForExaminationPersonal")
            .lastModifiedDate(LocalDateTime.of(2019,10,25,21,24,18, 143000000))
            .build();
    }

    private Task getProbateCaveatTask() {
        return Task.builder()
            .caseTypeId("Caveat")
            .id("1572044446693576")
            .jurisdiction("PROBATE")
            .state("CaveatPersonal")
            .lastModifiedDate(LocalDateTime.of(2019, 10, 25, 21, 24, 18, 143000000))
            .build();
    }

    private Task getProbateExceptionTask() {
        return Task.builder()
            .caseTypeId("PROBATE_ExceptionRecord")
            .id("1572038555593576")
            .jurisdiction("PROBATE")
            .state("ScannedRecordReceived")
            .lastModifiedDate(LocalDateTime.of(2019, 10, 25, 21, 24, 18, 143000000))
            .build();
    }

    private Task getFrTask() {
        return Task.builder()
            .caseTypeId("FinancialRemedyMVP2")
            .id("1544448226693576")
            .jurisdiction("DIVORCE")
            .state("ConsentAppCreated")
            .lastModifiedDate(LocalDateTime.of(2019,10,25,21,24,18, 143000000))
            .build();
    }

    private Task getFrExceptionTask() {
        return Task.builder()
            .caseTypeId("FINREM_ExceptionRecord")
            .id("1555558555593576")
            .jurisdiction("DIVORCE")
            .state("ScannedRecordReceivedFormA")
            .lastModifiedDate(LocalDateTime.of(2019,10,25,21,24,18, 143000000))
            .build();
    }

    private Task getDivorceEvidenceHandledTask() {
        return Task.builder()
            .caseTypeId("DIVORCE")
            .id("1563460551495399")
            .jurisdiction("DIVORCE")
            .state("SupplementaryEvidence")
            .lastModifiedDate(LocalDateTime.of(2019, 7, 18, 14, 36, 25, 862000000))
            .build();
    }
}
