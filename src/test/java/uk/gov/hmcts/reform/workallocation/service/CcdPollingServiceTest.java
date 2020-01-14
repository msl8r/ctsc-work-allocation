package uk.gov.hmcts.reform.workallocation.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.applicationinsights.TelemetryClient;
import org.junit.Before;
import org.junit.Test;
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
        MockitoAnnotations.initMocks(this);
        ccdPollingService = new CcdPollingService(idamService, ccdConnectorService, lastRunTimeService,
            30, 5, queueProducer,
            queueConsumer, deadQueueConsumer, telemetryClient);

        when(deadQueueConsumer.runConsumer(any(), any())).thenReturn(CompletableFuture.completedFuture(null));
        when(queueConsumer.runConsumer(any(), any())).thenReturn(CompletableFuture.completedFuture(null));

        Map<String, Object> divorceResponse = divorceSearchResult();
        divorceResponse.put("case_type_id", "DIVORCE");
        when(ccdConnectorService.searchDivorceCases(anyString(), anyString(), anyString(), anyString()))
            .thenReturn(divorceResponse);

        Map<String, Object> probateResponse = probateSearchResult();
        probateResponse.put("case_type_id", "GrantOfRepresentation");
        when(ccdConnectorService.searchProbateCases(anyString(), anyString(), anyString(), anyString()))
            .thenReturn(probateResponse);

        Map<String, Object> cmcResponse = cmcSearchResult();
        probateResponse.put("case_type_id", "GrantOfRepresentation");
        when(ccdConnectorService.searchCmcCases(anyString(), anyString(), anyString(), anyString()))
            .thenReturn(cmcResponse);

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
        Task task2 = getProbateTask();
        verify(ccdConnectorService, times(1))
            .searchDivorceCases(eq("idam_token"), eq("service_token"), eq(queryFromDate), anyString());
        verify(queueProducer, times(1)).placeItemsInQueue(eq(Arrays.asList(task1, task2)), any());
        verify(lastRunTimeService, times(1)).updateLastRuntime(any(LocalDateTime.class));
    }

    @Test
    public void testPollccdEndpointFirstTime() throws CcdConnectionException, IdamConnectionException {
        when(lastRunTimeService.getLastRunTime()).thenReturn(Optional.empty());

        ccdPollingService.pollCcdEndpoint();
        String queryDate = "2019-09-20T11:55";
        String queryToDate = LocalDateTime.now().toString();
        Task task1 = getDivorceTask();
        Task task2 = getProbateTask();
        verify(ccdConnectorService, times(1))
            .searchDivorceCases(eq("idam_token"), eq("service_token"), eq(queryDate), anyString());
        verify(queueProducer, times(1)).placeItemsInQueue(eq(Arrays.asList(task1, task2)), any());
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
        Task task2 = getProbateTask();
        verify(ccdConnectorService, times(1))
            .searchDivorceCases(eq("idam_token"), eq("service_token"), eq("2019-09-25T11:55"), anyString());
        verify(queueProducer, times(1)).placeItemsInQueue(eq(Arrays.asList(task1, task2)), any());
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
        Task task2 = getProbateTask();
        verify(ccdConnectorService, times(1))
            .searchDivorceCases(eq("idam_token"), eq("service_token"), eq("2019-09-25T11:55"), anyString());
        verify(queueProducer, times(1)).placeItemsInQueue(eq(Arrays.asList(task1, task2)), any());
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
        when(ccdConnectorService.searchDivorceCases(anyString(), anyString(), anyString(), anyString()))
            .thenReturn(searchResult);
        ccdPollingService.pollCcdEndpoint();
        Task task2 = getProbateTask();
        verify(ccdConnectorService, times(1))
            .searchDivorceCases(eq("idam_token"), eq("service_token"), eq("2019-09-20T11:55"), anyString());
        verify(queueProducer, times(1)).placeItemsInQueue(eq(Arrays.asList(task2)), any());
        verify(lastRunTimeService, times(1)).updateLastRuntime(any(LocalDateTime.class));
    }

    @Test
    public void testWhenLastRunLessThanThirtyMinutes() throws CcdConnectionException, IdamConnectionException {
        when(lastRunTimeService.getMinDate()).thenReturn(LocalDateTime.now().minusMinutes(25L));
        ccdPollingService.pollCcdEndpoint();
        verify(ccdConnectorService, times(0)).searchDivorceCases(any(), any(), any(), any());
        verify(queueProducer, times(0)).placeItemsInQueue(any(), any());
        verify(lastRunTimeService, times(0)).updateLastRuntime(any(LocalDateTime.class));
    }

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
    private Map<String, Object> cmcSearchResult() throws IOException {
        String json = "{\n"
            + "\"total\": 1,\n"
            + "  \"cases\": [\n"
            + "  {\n"
            +       "\"id\": 1567636489967327,\n"
            +       "\"jurisdiction\": \"CMC\",\n"
            +       "\"state\": \"orderDrawn\",\n"
            +       "\"version\": null,\n"
            +       "\"case_type_id\": null,\n"
            +       "\"created_date\": null,\n"
            +       "\"last_modified\": \"2019-09-04T23:33:39.574\",\n"
            +       "\"security_classification\": null,\n"
            +       "\"case_data\": null,\n"
            +       "\"data_classification\": null,\n"
            +       "\"after_submit_callback_response\": null,\n"
            +       "\"callback_response_status_code\": null,\n"
            +       "\"callback_response_status\": null,\n"
            +       "\"delete_draft_response_status_code\": null,\n"
            +       "\"delete_draft_response_status\": null,\n"
            +       "\"security_classifications\": null\n"
            +   "}"
            + "]\n"
            + "}";
        return new ObjectMapper().readValue(json, Map.class);
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
            .state("ReadyforExamination-Personal")
            .lastModifiedDate(LocalDateTime.of(2019,10,25,21,24,18, 143000000))
            .build();
    }

    private String composeQuery(String date) {
        return "{\"query\":{\"bool\":{\"must\":[{\"range\":{\"last_modified\":{\"gte\":\""
            + date + "\"}}},{\"match\":{\"state\":{\"query\": \"Submitted AwaitingHWFDecision DARequested\","
            + "\"operator\": \"or\"}}}]}},\"size\": 500}";
    }
}
