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
import java.util.Collections;
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

        when(deadQueueConsumer.runConsumer(any())).thenReturn(CompletableFuture.completedFuture(null));
        when(queueConsumer.runConsumer(any())).thenReturn(CompletableFuture.completedFuture(null));
        Map<String, Object> ccdResponse = caseSearchResult();
        ccdResponse.put("case_type_id", "DIVORCE");
        when(ccdConnectorService.searchCases(anyString(), anyString(), anyString(), anyString()))
            .thenReturn(ccdResponse);
        when(idamService.generateServiceAuthorization()).thenReturn("service_token");
        when(idamService.getIdamOauth2Token()).thenReturn("idam_token");
        when(lastRunTimeService.getMinDate()).thenReturn(LocalDateTime.of(2019, 9, 20, 12, 0, 0, 0));
    }

    @Test
    public void testPollccdEndpoint() throws CcdConnectionException, IdamConnectionException {
        when(lastRunTimeService.getLastRunTime()).thenReturn(Optional.of(LocalDateTime.of(2019, 9, 25, 12, 0, 0, 0)));

        ccdPollingService.pollCcdEndpoint();
        String queryFromDate = "2019-09-25T11:55";
        Task task = getTask();
        verify(ccdConnectorService, times(1))
            .searchCases(eq("idam_token"), eq("service_token"), eq(queryFromDate), anyString());
        verify(queueProducer, times(1)).placeItemsInQueue(eq(Collections.singletonList(task)), any());
        verify(lastRunTimeService, times(1)).updateLastRuntime(any(LocalDateTime.class));
    }

    @Test
    public void testPollccdEndpointFirstTime() throws CcdConnectionException, IdamConnectionException {
        when(lastRunTimeService.getLastRunTime()).thenReturn(Optional.empty());

        ccdPollingService.pollCcdEndpoint();
        String queryDate = "2019-09-20T11:55";
        String queryToDate = LocalDateTime.now().toString();
        Task task = getTask();
        verify(ccdConnectorService, times(1))
            .searchCases(eq("idam_token"), eq("service_token"), eq(queryDate), anyString());
        verify(queueProducer, times(1)).placeItemsInQueue(eq(Collections.singletonList(task)), any());
        verify(lastRunTimeService, times(1)).updateLastRuntime(any(LocalDateTime.class));
    }

    @Test
    public void testPollccdEndpointWhenQueueConsumerThrowsAnError()
            throws CcdConnectionException, IdamConnectionException {
        CompletableFuture<Void> consumerResponse = new CompletableFuture<>();
        consumerResponse.completeExceptionally(new RuntimeException("Something went wrong"));
        when(lastRunTimeService.getLastRunTime()).thenReturn(Optional.of(LocalDateTime.of(2019, 9, 25, 12, 0, 0, 0)));
        when(queueConsumer.runConsumer(any())).thenReturn(consumerResponse);
        ccdPollingService.pollCcdEndpoint();
        Task task = getTask();
        verify(ccdConnectorService, times(1))
            .searchCases(eq("idam_token"), eq("service_token"), eq("2019-09-25T11:55"), anyString());
        verify(queueProducer, times(1)).placeItemsInQueue(eq(Collections.singletonList(task)), any());
        verify(lastRunTimeService, times(1)).updateLastRuntime(any(LocalDateTime.class));
    }

    @Test
    public void testPollccdEndpointWhenDeadQueueConsumerThrowsAnError()
            throws CcdConnectionException, IdamConnectionException {
        CompletableFuture<Void> consumerResponse = new CompletableFuture<>();
        consumerResponse.completeExceptionally(new RuntimeException("Something went wrong"));
        when(lastRunTimeService.getLastRunTime()).thenReturn(Optional.of(LocalDateTime.of(2019, 9, 25, 12, 0, 0, 0)));
        when(deadQueueConsumer.runConsumer(any())).thenReturn(consumerResponse);
        ccdPollingService.pollCcdEndpoint();
        Task task = getTask();
        verify(ccdConnectorService, times(1))
            .searchCases(eq("idam_token"), eq("service_token"), eq("2019-09-25T11:55"), anyString());
        verify(queueProducer, times(1)).placeItemsInQueue(eq(Collections.singletonList(task)), any());
        verify(lastRunTimeService, times(1)).updateLastRuntime(any(LocalDateTime.class));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testPollCcdWhenTheResponseIsNotCorrect()
            throws IOException, CcdConnectionException, IdamConnectionException {
        Map<String, Object> searchResult = caseSearchResult();
        List<Object> cases = (List<Object>) searchResult.get("cases");
        Map<String, Object> ccdCase = (Map<String, Object>) cases.get(0);
        ccdCase.remove("id");
        when(ccdConnectorService.searchCases(anyString(), anyString(), anyString(), anyString()))
            .thenReturn(searchResult);
        ccdPollingService.pollCcdEndpoint();
        verify(ccdConnectorService, times(1))
            .searchCases(eq("idam_token"), eq("service_token"), eq("2019-09-20T11:55"), anyString());
        verify(queueProducer, times(1)).placeItemsInQueue(eq(Collections.emptyList()), any());
        verify(lastRunTimeService, times(1)).updateLastRuntime(any(LocalDateTime.class));
    }

    @Test
    public void testWhenLastRunLessThanThirtyMinutes() throws CcdConnectionException, IdamConnectionException {
        when(lastRunTimeService.getMinDate()).thenReturn(LocalDateTime.now().minusMinutes(25L));
        ccdPollingService.pollCcdEndpoint();
        verify(ccdConnectorService, times(0)).searchCases(any(), any(), any(), any());
        verify(queueProducer, times(0)).placeItemsInQueue(any(), any());
        verify(lastRunTimeService, times(0)).updateLastRuntime(any(LocalDateTime.class));
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

    private Task getTask() {
        return Task.builder()
            .caseTypeId("DIVORCE")
            .id("1563460551495313")
            .jurisdiction("DIVORCE")
            .state("Submitted")
            .lastModifiedDate(LocalDateTime.of(2019,7,18,14,36,25, 862000000))
            .build();
    }

    private String composeQuery(String date) {
        return "{\"query\":{\"bool\":{\"must\":[{\"range\":{\"last_modified\":{\"gte\":\""
            + date + "\"}}},{\"match\":{\"state\":{\"query\": \"Submitted AwaitingHWFDecision DARequested\","
            + "\"operator\": \"or\"}}}]}},\"size\": 500}";
    }
}
