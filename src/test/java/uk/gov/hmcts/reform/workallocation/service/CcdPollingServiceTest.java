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
import java.util.ArrayList;
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

        Map<String, Object> bulkScanResponse = bulkScanSearchResult();
        bulkScanResponse.put("case_type_id", "PROBATE_ExceptionRecord");
        when(ccdConnectorService.searchBulkScanningCases(anyString(), anyString(), anyString(), anyString()))
            .thenReturn(bulkScanResponse);

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
        List<Task> task3 = getBulkScanTasks();
        verify(ccdConnectorService, times(1))
            .searchDivorceCases(eq("idam_token"), eq("service_token"), eq(queryFromDate), anyString());
        verify(queueProducer, times(1)).placeItemsInQueue(eq(Arrays.asList(task1, task2,
            task3.get(0), task3.get(1), task3.get(2))), any());
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
        List<Task> task3 = getBulkScanTasks();
        verify(ccdConnectorService, times(1))
            .searchDivorceCases(eq("idam_token"), eq("service_token"), eq(queryDate), anyString());
        verify(queueProducer, times(1)).placeItemsInQueue(eq(Arrays.asList(task1, task2,
            task3.get(0), task3.get(1), task3.get(2))), any());
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
        List<Task> task3 = getBulkScanTasks();
        verify(ccdConnectorService, times(1))
            .searchDivorceCases(eq("idam_token"), eq("service_token"), eq("2019-09-25T11:55"), anyString());
        verify(queueProducer, times(1)).placeItemsInQueue(eq(Arrays.asList(task1, task2,
            task3.get(0), task3.get(1), task3.get(2))), any());
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
        List<Task> task3 = getBulkScanTasks();
        verify(ccdConnectorService, times(1))
            .searchDivorceCases(eq("idam_token"), eq("service_token"), eq("2019-09-25T11:55"), anyString());
        verify(queueProducer, times(1)).placeItemsInQueue(eq(Arrays.asList(task1, task2,
            task3.get(0), task3.get(1), task3.get(2))), any());
        verify(lastRunTimeService, times(1)).updateLastRuntime(any(LocalDateTime.class));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testPollCcdWhenTheResponseIsNotCorrect()
            throws IOException, CcdConnectionException, IdamConnectionException {
        Map<String, Object> searchResult = divorceSearchResult();
        searchResult.put("case_type_id", "DIVORCE");
        List<Object> cases = (List<Object>) searchResult.get("cases");
        Map<String, Object> ccdCase = (Map<String, Object>) cases.get(0);
        ccdCase.remove("id");
        when(ccdConnectorService.searchDivorceCases(anyString(), anyString(), anyString(), anyString()))
            .thenReturn(searchResult);
        ccdPollingService.pollCcdEndpoint();
        Task task2 = getProbateTask();
        List<Task> task3 = getBulkScanTasks();
        verify(ccdConnectorService, times(1))
            .searchDivorceCases(eq("idam_token"), eq("service_token"), eq("2019-09-20T11:55"), anyString());
        verify(queueProducer, times(1)).placeItemsInQueue(eq(Arrays.asList(task2,
            task3.get(0), task3.get(1), task3.get(2))), any());
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
            + "     \"state\": \"BOCaseStopped\",\n"
            + "     \"version\": null,\n"
            + "     \"case_type_id\": null,\n"
            + "     \"created_date\": null,\n"
            + "     \"last_modified\": \"2019-10-25T21:24:18.143\",\n"
            + "     \"security_classification\": null,\n"
            + "     \"case_data\":{\n"
            + "         \"evidenceHandled\": \"No\",\n"
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
    private Map<String, Object> bulkScanSearchResult() throws IOException {
        String json = "{\"total\":3,\"cases\":[{\"id\":1585761273173755,\"jurisdiction\":\"PROBATE\",\"state\":\"ScannedRecordReceived\",\"version\":null,\"case_type_id\":null,\"created_date\":null,\"last_modified\":\"2020-04-02T14:58:01.417\",\"last_state_modified_date\":null,\"security_classification\":null,\"case_data\":{\"journeyClassification\":\"NEW_APPLICATION\",\"formType\":\"PA8A\",\"awaitingPaymentDCNProcessing\":\"No\",\"poBoxJurisdiction\":\"PROBATE\",\"containsPayments\":\"Yes\",\"displayWarnings\":\"No\",\"scannedDocuments\":[{\"id\":\"2ff31d44-7d72-4d73-b29f-b2d94ff1d080\",\"value\":{\"fileName\":\"18347040100090007.pdf\",\"scannedDate\":\"2018-12-07T10:46:03.123\",\"subtype\":\"PA8A\",\"type\":\"form\",\"deliveryDate\":\"2019-02-04T14:20:22.123\",\"controlNumber\":\"611001103381822919042\",\"url\":{\"document_binary_url\":\"http://dm-store-demo.service.core-compute-demo.internal/documents/fa448c5a-5acf-4638-a435-943da2ac24a7/binary\",\"document_filename\":\"18347040100090007.pdf\",\"document_url\":\"http://dm-store-demo.service.core-compute-demo.internal/documents/fa448c5a-5acf-4638-a435-943da2ac24a7\"}}},{\"id\":\"cf413126-fa68-439c-be84-704a651fa095\",\"value\":{\"fileName\":\"90000002.pdf\",\"scannedDate\":\"2018-12-27T10:46:03.123\",\"type\":\"other\",\"deliveryDate\":\"2019-02-04T14:20:22.123\",\"controlNumber\":\"89711111189731132222\",\"url\":{\"document_binary_url\":\"http://dm-store-demo.service.core-compute-demo.internal/documents/80c899bc-17ef-447e-9bb3-8c7063cc367b/binary\",\"document_filename\":\"90000002.pdf\",\"document_url\":\"http://dm-store-demo.service.core-compute-demo.internal/documents/80c899bc-17ef-447e-9bb3-8c7063cc367b\"}}},{\"id\":\"fea2c566-fdaf-4976-9cb4-c1f045f5f386\",\"value\":{\"fileName\":\"90000003.pdf\",\"scannedDate\":\"2018-12-27T10:46:03.123\",\"subtype\":\"Passport\",\"type\":\"cherished\",\"deliveryDate\":\"2019-02-04T14:20:22.123\",\"controlNumber\":\"3436110011983400342222\",\"url\":{\"document_binary_url\":\"http://dm-store-demo.service.core-compute-demo.internal/documents/72364cb7-f1ff-42d0-bdda-720b23df9015/binary\",\"document_filename\":\"90000003.pdf\",\"document_url\":\"http://dm-store-demo.service.core-compute-demo.internal/documents/72364cb7-f1ff-42d0-bdda-720b23df9015\"}}}],\"showEnvelopeCaseReference\":\"No\",\"poBox\":\"12625\",\"envelopeCaseReference\":\"\",\"envelopeId\":\"97f25c64-fb47-4c67-91b1-f1d2a24f5ca3\",\"surname\":null,\"showEnvelopeLegacyCaseReference\":\"No\",\"envelopeLegacyCaseReference\":\"\",\"deliveryDate\":\"2019-02-04T14:20:22.123\",\"scanOCRData\":[{\"id\":\"a71b7d9f-27e7-4b09-884a-119fc9e9e1fa\",\"value\":{\"value\":\"Jessica\",\"key\":\"caveatorForenames\"}},{\"id\":\"1005d623-cf1a-45ef-91cb-23041d1dfa8d\",\"value\":{\"value\":\"Diana\",\"key\":\"caveatorMiddleNames\"}},{\"id\":\"2a92131b-b4e1-4067-853a-736ed3c9e7af\",\"value\":{\"value\":\"Simpson\",\"key\":\"caveatorSurnames\"}},{\"id\":\"4dd9615f-6b95-481b-af6b-d95ca864d321\",\"value\":{\"value\":\"testemail@mail.com\",\"key\":\"caveatorEmailAddress\"}},{\"id\":\"7927901e-5351-4a1c-bc52-26b8632c60bd\",\"value\":{\"value\":\"1 January Street\",\"key\":\"caveatorAddressLine1\"}},{\"id\":\"c7580759-6cdd-461c-b961-f628ec1b0cf2\",\"value\":{\"value\":\"January Grove\",\"key\":\"caveatorAddressLine2\"}},{\"id\":\"3b9a8417-c77f-4cb4-a568-390fffe462cb\",\"value\":{\"value\":\"January Town\",\"key\":\"caveatorAddressTown\"}},{\"id\":\"b3c478b1-927f-488d-8d3f-798f2a381ddf\",\"value\":{\"value\":\"Middlesex\",\"key\":\"caveatorAddressCounty\"}},{\"id\":\"cf7234b2-b519-4ae6-a408-6e27ead201f5\",\"value\":{\"value\":\"JJ1 2WW\",\"key\":\"caveatorAddressPostCode\"}},{\"id\":\"9d1ee4f7-7742-4688-bd6e-fd3b335c80b2\",\"value\":{\"value\":\"John Major\",\"key\":\"solsSolicitorRepresentativeName\"}},{\"id\":\"28c8f8fc-8132-454b-a635-cae5b665f0ec\",\"value\":{\"value\":\"Major Solicitors Ltd\",\"key\":\"solsSolicitorFirmName\"}},{\"id\":\"a77ea100-7b81-4254-a178-9bd969570def\",\"value\":{\"value\":\"SOL123456\",\"key\":\"solsSolicitorAppReference\"}},{\"id\":\"29cc248f-1df0-4b1f-ac8f-4f60c3de552e\",\"value\":{\"value\":\"PBA-123456\",\"key\":\"solsFeeAccountNumber\"}},{\"id\":\"28c83fec-f158-4b9d-b3d1-3934c3443d26\",\"value\":{\"value\":\"House of Peas\",\"key\":\"solsSolicitorAddressLine1 \"}},{\"id\":\"2c34a535-eb1a-4057-af8b-5ef35fb6a768\",\"value\":{\"value\":\"London No Way\",\"key\":\"solsSolicitorAddressLine2\"}},{\"id\":\"8039e573-ab7a-4020-899e-fd085a70e6d1\",\"value\":{\"value\":\"London Town\",\"key\":\"solsSolicitorAddressTown\"}},{\"id\":\"04cb37f9-3dfc-4017-9a87-9cd711d121c1\",\"value\":{\"value\":\"Greater London\",\"key\":\"solsSolicitorAddressCounty\"}},{\"id\":\"5c8a7ac9-cab5-4cbd-a9dc-2abc9ab831f3\",\"value\":{\"value\":\"NW1 1LE\",\"key\":\"solsSolicitorAddressPostCode \"}},{\"id\":\"b34760d0-b2a7-4a84-a45b-174bac4f7b33\",\"value\":{\"value\":\"john@test-solicitors.com\",\"key\":\"solsSolicitorEmail\"}},{\"id\":\"811bdf31-310f-494c-9fd0-7b5d44ee7d39\",\"value\":{\"value\":\"02073843749\",\"key\":\"solsSolicitorPhoneNumber\"}},{\"id\":\"e8eeb802-162c-4eb4-94eb-71b80a6a0079\",\"value\":{\"value\":\"Thomas\",\"key\":\"deceasedForenames\"}},{\"id\":\"1be1117e-b76a-458f-a251-d0f8a5254f41\",\"value\":{\"value\":\"Phillip\",\"key\":\"deceasedMiddleNames\"}},{\"id\":\"560f2e65-3981-4bd0-8022-774b5a0e8bff\",\"value\":{\"value\":\"Simpson\",\"key\":\"deceasedSurname\"}},{\"id\":\"aceb756f-d2e0-418c-888a-15c88d761136\",\"value\":{\"value\":\"02022019\",\"key\":\"deceasedDateOfDeath\"}},{\"id\":\"2b34a1f4-cf95-4c90-92e9-5ab7fae83d1e\",\"value\":{\"value\":\"12031950\",\"key\":\"deceasedDateOfBirth\"}},{\"id\":\"6ba1d6f2-4471-4f46-9fc6-90928128edda\",\"value\":{\"value\":\"False\",\"key\":\"deceasedAnyOtherNames\"}},{\"id\":\"62e42cf6-7223-4de5-a75c-6171f9bc7220\",\"value\":{\"value\":\"3 Roebuck Close\",\"key\":\"deceasedAddressLine1\"}},{\"id\":\"5a3fd7ae-3bf6-4784-a292-336b1e81a465\",\"value\":{\"value\":\"Roebuck Street\",\"key\":\"deceasedAddressLine2\"}},{\"id\":\"c692164b-4250-4e71-bd5c-c2f43fe2331f\",\"value\":{\"value\":\"La La Land\",\"key\":\"deceasedAddressTown\"}},{\"id\":\"a3c86436-56a9-48e9-9047-55707807b117\",\"value\":{\"value\":\"Middlesex\",\"key\":\"deceasedAddressCounty\"}},{\"id\":\"92ecda77-2dea-4bb3-a582-4807b66cbcd5\",\"value\":{\"value\":\"LA5 6TE\",\"key\":\"deceasedAddressPostCode\"}},{\"id\":\"0d5c7b12-6d34-40f9-872f-99d5aa4ab804\",\"value\":{\"value\":\"\",\"key\":\"caseReference\"}}],\"openingDate\":\"2019-02-04T14:20:22.123\",\"ocrDataValidationWarnings\":[]},\"data_classification\":null,\"after_submit_callback_response\":null,\"callback_response_status_code\":null,\"callback_response_status\":null,\"delete_draft_response_status_code\":null,\"delete_draft_response_status\":null,\"security_classifications\":null},{\"id\":1585827388328608,\"jurisdiction\":\"PROBATE\",\"state\":\"ScannedRecordReceived\",\"version\":null,\"case_type_id\":null,\"created_date\":null,\"last_modified\":\"2020-04-02T14:58:04.985\",\"last_state_modified_date\":null,\"security_classification\":null,\"case_data\":{\"journeyClassification\":\"NEW_APPLICATION\",\"formType\":\"PA8A\",\"awaitingPaymentDCNProcessing\":\"No\",\"poBoxJurisdiction\":\"PROBATE\",\"containsPayments\":\"Yes\",\"displayWarnings\":\"No\",\"scannedDocuments\":[{\"id\":\"bedbe56c-33f6-4548-8609-a02ca99a652a\",\"value\":{\"fileName\":\"90000002.pdf\",\"scannedDate\":\"2018-12-27T10:46:03.123\",\"type\":\"other\",\"deliveryDate\":\"2019-02-04T14:20:22.123\",\"controlNumber\":\"7700189731132222\",\"url\":{\"document_binary_url\":\"http://dm-store-demo.service.core-compute-demo.internal/documents/7c740f00-dbf0-4d1b-8754-87ca636b2e8c/binary\",\"document_filename\":\"90000002.pdf\",\"document_url\":\"http://dm-store-demo.service.core-compute-demo.internal/documents/7c740f00-dbf0-4d1b-8754-87ca636b2e8c\"}}},{\"id\":\"07907b69-f888-4c6b-8dad-43a1a2c75d3d\",\"value\":{\"fileName\":\"90000003.pdf\",\"scannedDate\":\"2018-12-27T10:46:03.123\",\"subtype\":\"Passport\",\"type\":\"cherished\",\"deliveryDate\":\"2019-02-04T14:20:22.123\",\"controlNumber\":\"4457011983400342222\",\"url\":{\"document_binary_url\":\"http://dm-store-demo.service.core-compute-demo.internal/documents/e6c113ea-285e-4579-a6e8-ae39f7463c86/binary\",\"document_filename\":\"90000003.pdf\",\"document_url\":\"http://dm-store-demo.service.core-compute-demo.internal/documents/e6c113ea-285e-4579-a6e8-ae39f7463c86\"}}},{\"id\":\"bcd3d6f8-0f4d-4e02-a622-b4ccbb36fa8b\",\"value\":{\"fileName\":\"18347040100090007.pdf\",\"scannedDate\":\"2018-12-07T10:46:03.123\",\"subtype\":\"PA8A\",\"type\":\"form\",\"deliveryDate\":\"2019-02-04T14:20:22.123\",\"controlNumber\":\"80003381822919042\",\"url\":{\"document_binary_url\":\"http://dm-store-demo.service.core-compute-demo.internal/documents/272a5376-3530-4ae9-bf2e-df7861205671/binary\",\"document_filename\":\"18347040100090007.pdf\",\"document_url\":\"http://dm-store-demo.service.core-compute-demo.internal/documents/272a5376-3530-4ae9-bf2e-df7861205671\"}}}],\"showEnvelopeCaseReference\":\"No\",\"poBox\":\"12625\",\"envelopeCaseReference\":\"\",\"envelopeId\":\"63ace905-727d-4ffd-8ee6-dfa73f281eb3\",\"surname\":null,\"showEnvelopeLegacyCaseReference\":\"No\",\"envelopeLegacyCaseReference\":\"\",\"deliveryDate\":\"2019-02-04T14:20:22.123\",\"scanOCRData\":[{\"id\":\"d2a0ee94-dde1-40bc-951e-66b2f0bcea62\",\"value\":{\"value\":\"Jessica\",\"key\":\"caveatorForenames\"}},{\"id\":\"f4328d1a-d28d-4863-a976-52784a2e1ebf\",\"value\":{\"value\":\"Diana\",\"key\":\"caveatorMiddleNames\"}},{\"id\":\"a3229ebb-ef25-4ce8-b528-63fe87d477f4\",\"value\":{\"value\":\"Simpson\",\"key\":\"caveatorSurnames\"}},{\"id\":\"70f3f690-d173-4a31-bb1d-5144978c6a3a\",\"value\":{\"value\":\"testemail@mail.com\",\"key\":\"caveatorEmailAddress\"}},{\"id\":\"667a085b-a67c-4f69-9e9d-2077d3a9e100\",\"value\":{\"value\":\"1 January Street\",\"key\":\"caveatorAddressLine1\"}},{\"id\":\"6de2e08b-4e95-4a0f-ba69-69196e5d7cca\",\"value\":{\"value\":\"January Grove\",\"key\":\"caveatorAddressLine2\"}},{\"id\":\"c6fda1b8-132d-4e8f-b520-89e91e8f5d89\",\"value\":{\"value\":\"January Town\",\"key\":\"caveatorAddressTown\"}},{\"id\":\"98875440-16b1-4428-9dba-435ae37ab603\",\"value\":{\"value\":\"Middlesex\",\"key\":\"caveatorAddressCounty\"}},{\"id\":\"1c72b616-c5d9-4919-bf34-fd979f2cf122\",\"value\":{\"value\":\"JJ1 2WW\",\"key\":\"caveatorAddressPostCode\"}},{\"id\":\"1fc6c85b-9c3b-4646-bf55-add32b187117\",\"value\":{\"value\":\"John Major\",\"key\":\"solsSolicitorRepresentativeName\"}},{\"id\":\"1bebd850-0b02-44f6-80b4-7e9bf7512901\",\"value\":{\"value\":\"Major Solicitors Ltd\",\"key\":\"solsSolicitorFirmName\"}},{\"id\":\"9b4f723c-cdc5-4e83-8ecc-f69860aa42be\",\"value\":{\"value\":\"SOL123456\",\"key\":\"solsSolicitorAppReference\"}},{\"id\":\"f53090a0-9bbc-4551-844a-bd1b2fc080b6\",\"value\":{\"value\":\"PBA-123456\",\"key\":\"solsFeeAccountNumber\"}},{\"id\":\"86d93221-166e-4a44-bb76-1fe027d451cc\",\"value\":{\"value\":\"House of Peas\",\"key\":\"solsSolicitorAddressLine1 \"}},{\"id\":\"7a8ff73f-d7a5-404b-8fa9-87d52b857d46\",\"value\":{\"value\":\"London No Way\",\"key\":\"solsSolicitorAddressLine2\"}},{\"id\":\"46c8f4a6-8374-4a51-bef6-9a6a61c4cbe2\",\"value\":{\"value\":\"London Town\",\"key\":\"solsSolicitorAddressTown\"}},{\"id\":\"b3b7e1b1-22b2-4c33-8e23-cdb86a8294ca\",\"value\":{\"value\":\"Greater London\",\"key\":\"solsSolicitorAddressCounty\"}},{\"id\":\"faf2b651-aa41-4dc4-8f24-d142904d7281\",\"value\":{\"value\":\"NW1 1LE\",\"key\":\"solsSolicitorAddressPostCode \"}},{\"id\":\"cbccda8f-7459-480a-b71d-5533a8578a64\",\"value\":{\"value\":\"john@test-solicitors.com\",\"key\":\"solsSolicitorEmail\"}},{\"id\":\"1f620616-7f75-4b8a-801c-9ab1017949a0\",\"value\":{\"value\":\"02073843749\",\"key\":\"solsSolicitorPhoneNumber\"}},{\"id\":\"e3c9b731-5e0f-4241-96e4-5c2411434850\",\"value\":{\"value\":\"Thomas\",\"key\":\"deceasedForenames\"}},{\"id\":\"2dabc5a8-cd89-4166-8956-664a9d0829e0\",\"value\":{\"value\":\"Phillip\",\"key\":\"deceasedMiddleNames\"}},{\"id\":\"154d4ddb-f29b-4ae1-9e05-7f265ec85c0f\",\"value\":{\"value\":\"Simpson\",\"key\":\"deceasedSurname\"}},{\"id\":\"9b5a335b-f4b1-463c-b4b9-de81ef50e990\",\"value\":{\"value\":\"02022019\",\"key\":\"deceasedDateOfDeath\"}},{\"id\":\"451067f9-6ed1-4cca-ac64-2c8d5a7d3b88\",\"value\":{\"value\":\"12031950\",\"key\":\"deceasedDateOfBirth\"}},{\"id\":\"6b2dfe79-c74b-4731-91c9-1422c2eddcf4\",\"value\":{\"value\":\"False\",\"key\":\"deceasedAnyOtherNames\"}},{\"id\":\"6fe5f843-dc0e-4da0-b722-b7738eaf4286\",\"value\":{\"value\":\"3 Roebuck Close\",\"key\":\"deceasedAddressLine1\"}},{\"id\":\"63ece84d-57f7-484a-98d7-572eed075913\",\"value\":{\"value\":\"Roebuck Street\",\"key\":\"deceasedAddressLine2\"}},{\"id\":\"ae2f6fc5-8f25-4917-a5ea-e6ccffa5d8f7\",\"value\":{\"value\":\"La La Land\",\"key\":\"deceasedAddressTown\"}},{\"id\":\"3744d702-42fa-4bf4-8603-6dd9baf61abd\",\"value\":{\"value\":\"Middlesex\",\"key\":\"deceasedAddressCounty\"}},{\"id\":\"cccad379-7d37-4a6a-b2ba-ed2227d96b6a\",\"value\":{\"value\":\"LA5 6TE\",\"key\":\"deceasedAddressPostCode\"}},{\"id\":\"67b00e47-cc86-458d-9e6a-fc7262687637\",\"value\":{\"value\":\"\",\"key\":\"caseReference\"}}],\"openingDate\":\"2019-02-04T14:20:22.123\",\"ocrDataValidationWarnings\":[]},\"data_classification\":null,\"after_submit_callback_response\":null,\"callback_response_status_code\":null,\"callback_response_status\":null,\"delete_draft_response_status_code\":null,\"delete_draft_response_status\":null,\"security_classifications\":null},{\"id\":1585760187504931,\"jurisdiction\":\"PROBATE\",\"state\":\"ScannedRecordReceived\",\"version\":null,\"case_type_id\":null,\"created_date\":null,\"last_modified\":\"2020-04-02T14:57:58.999\",\"last_state_modified_date\":null,\"security_classification\":null,\"case_data\":{\"journeyClassification\":\"NEW_APPLICATION\",\"formType\":\"PA8A\",\"awaitingPaymentDCNProcessing\":\"No\",\"poBoxJurisdiction\":\"PROBATE\",\"containsPayments\":\"Yes\",\"displayWarnings\":\"No\",\"scannedDocuments\":[{\"id\":\"37cac5b4-0cd9-45a4-9702-78a629daac0a\",\"value\":{\"fileName\":\"90000002.pdf\",\"scannedDate\":\"2018-12-27T10:46:03.123\",\"type\":\"other\",\"deliveryDate\":\"2019-02-04T14:20:22.123\",\"controlNumber\":\"89711189731132222\",\"url\":{\"document_binary_url\":\"http://dm-store-demo.service.core-compute-demo.internal/documents/9d7b7e65-7b56-486e-8ae7-5bf11a2118ba/binary\",\"document_filename\":\"90000002.pdf\",\"document_url\":\"http://dm-store-demo.service.core-compute-demo.internal/documents/9d7b7e65-7b56-486e-8ae7-5bf11a2118ba\"}}},{\"id\":\"488c3d6e-b88c-40ec-b430-660690e9fc08\",\"value\":{\"fileName\":\"90000003.pdf\",\"scannedDate\":\"2018-12-27T10:46:03.123\",\"subtype\":\"Passport\",\"type\":\"cherished\",\"deliveryDate\":\"2019-02-04T14:20:22.123\",\"controlNumber\":\"34361111983400342222\",\"url\":{\"document_binary_url\":\"http://dm-store-demo.service.core-compute-demo.internal/documents/21627dee-1a5b-407d-902a-b84efb4f1a27/binary\",\"document_filename\":\"90000003.pdf\",\"document_url\":\"http://dm-store-demo.service.core-compute-demo.internal/documents/21627dee-1a5b-407d-902a-b84efb4f1a27\"}}},{\"id\":\"efe9b7de-21a8-440e-9826-a1ec0caef0c2\",\"value\":{\"fileName\":\"18347040100090007.pdf\",\"scannedDate\":\"2018-12-07T10:46:03.123\",\"subtype\":\"PA8A\",\"type\":\"form\",\"deliveryDate\":\"2019-02-04T14:20:22.123\",\"controlNumber\":\"61111033818919042\",\"url\":{\"document_binary_url\":\"http://dm-store-demo.service.core-compute-demo.internal/documents/e3678f0f-8d29-427a-ba15-e0a1add22a62/binary\",\"document_filename\":\"18347040100090007.pdf\",\"document_url\":\"http://dm-store-demo.service.core-compute-demo.internal/documents/e3678f0f-8d29-427a-ba15-e0a1add22a62\"}}}],\"showEnvelopeCaseReference\":\"No\",\"poBox\":\"12625\",\"envelopeCaseReference\":\"\",\"envelopeId\":\"bbdf7cbe-ea5e-4230-b6cc-d597ea1cf23f\",\"surname\":null,\"showEnvelopeLegacyCaseReference\":\"No\",\"envelopeLegacyCaseReference\":\"\",\"deliveryDate\":\"2019-02-04T14:20:22.123\",\"scanOCRData\":[{\"id\":\"4f120fe0-61f8-499b-a0c8-612427003480\",\"value\":{\"value\":\"Jessica\",\"key\":\"caveatorForenames\"}},{\"id\":\"45379252-4828-4f3f-9d86-90b7f746bf54\",\"value\":{\"value\":\"Diana\",\"key\":\"caveatorMiddleNames\"}},{\"id\":\"f655e3fd-2e0d-4331-a0bf-6fc01c4dc5a9\",\"value\":{\"value\":\"Simpson\",\"key\":\"caveatorSurnames\"}},{\"id\":\"e985eaae-71b1-44b9-9f2c-eeca3f24f138\",\"value\":{\"value\":\"testemail@mail.com\",\"key\":\"caveatorEmailAddress\"}},{\"id\":\"635263c3-3875-4c10-841f-e9916e007eb7\",\"value\":{\"value\":\"1 January Street\",\"key\":\"caveatorAddressLine1\"}},{\"id\":\"23a679c0-db10-44ab-91b4-a7b7438f0a01\",\"value\":{\"value\":\"January Grove\",\"key\":\"caveatorAddressLine2\"}},{\"id\":\"b3b02d87-84f9-43d1-a8fc-714faeeef03d\",\"value\":{\"value\":\"January Town\",\"key\":\"caveatorAddressTown\"}},{\"id\":\"c9de575c-bb08-4d02-bc4f-ecd6ffe864ef\",\"value\":{\"value\":\"Middlesex\",\"key\":\"caveatorAddressCounty\"}},{\"id\":\"73750035-b73a-4e23-8a3d-30cbd0f34d83\",\"value\":{\"value\":\"JJ1 2WW\",\"key\":\"caveatorAddressPostCode\"}},{\"id\":\"32777942-ec87-457f-81f6-403e09477a1f\",\"value\":{\"value\":\"John Major\",\"key\":\"solsSolicitorRepresentativeName\"}},{\"id\":\"86c6c5d2-a930-4979-9ff8-e80e5e9088ce\",\"value\":{\"value\":\"Major Solicitors Ltd\",\"key\":\"solsSolicitorFirmName\"}},{\"id\":\"0c1899d1-d7f4-41f4-b8a5-5860a80f0d58\",\"value\":{\"value\":\"SOL123456\",\"key\":\"solsSolicitorAppReference\"}},{\"id\":\"eada15bf-2387-494d-9a15-dbf4d5fa5cba\",\"value\":{\"value\":\"PBA-123456\",\"key\":\"solsFeeAccountNumber\"}},{\"id\":\"1a77fc98-09c4-41fc-918a-62c24277ce96\",\"value\":{\"value\":\"House of Peas\",\"key\":\"solsSolicitorAddressLine1 \"}},{\"id\":\"8887ef17-6988-40b4-bc97-c5b32cf5536c\",\"value\":{\"value\":\"London No Way\",\"key\":\"solsSolicitorAddressLine2\"}},{\"id\":\"8627e725-b596-4bde-8517-3082d1decd79\",\"value\":{\"value\":\"London Town\",\"key\":\"solsSolicitorAddressTown\"}},{\"id\":\"808addab-ef57-4181-a238-1738beabb3fb\",\"value\":{\"value\":\"Greater London\",\"key\":\"solsSolicitorAddressCounty\"}},{\"id\":\"5f369347-9f8b-4f41-9ec8-51cd2d7d0264\",\"value\":{\"value\":\"NW1 1LE\",\"key\":\"solsSolicitorAddressPostCode \"}},{\"id\":\"8a9d45d6-2394-44b7-a31d-b97c8ebb1c25\",\"value\":{\"value\":\"john@test-solicitors.com\",\"key\":\"solsSolicitorEmail\"}},{\"id\":\"021dd3ac-582f-4eea-b0a7-82d706f800d7\",\"value\":{\"value\":\"02073843749\",\"key\":\"solsSolicitorPhoneNumber\"}},{\"id\":\"b19118d7-83f7-4eb0-8428-3ead891fc283\",\"value\":{\"value\":\"Thomas\",\"key\":\"deceasedForenames\"}},{\"id\":\"8386e8a7-07d0-4cd1-9e6a-648b364d2fa0\",\"value\":{\"value\":\"Phillip\",\"key\":\"deceasedMiddleNames\"}},{\"id\":\"e6b85aa4-c992-4f8f-96f4-b4dcfcd94a6b\",\"value\":{\"value\":\"Simpson\",\"key\":\"deceasedSurname\"}},{\"id\":\"ca1d42e7-4de1-4d5c-8456-5735f3ec97d5\",\"value\":{\"value\":\"02022019\",\"key\":\"deceasedDateOfDeath\"}},{\"id\":\"9efa7642-02eb-4a7c-bae6-472c12064e6d\",\"value\":{\"value\":\"12031950\",\"key\":\"deceasedDateOfBirth\"}},{\"id\":\"729a9e33-5fa3-4192-8f50-9f7c57d8dc97\",\"value\":{\"value\":\"False\",\"key\":\"deceasedAnyOtherNames\"}},{\"id\":\"f9e19403-71d1-42f1-95cb-4cdcac7d4edc\",\"value\":{\"value\":\"3 Roebuck Close\",\"key\":\"deceasedAddressLine1\"}},{\"id\":\"abe0e9dd-8fe4-4d6d-943f-ec3cc5261d57\",\"value\":{\"value\":\"Roebuck Street\",\"key\":\"deceasedAddressLine2\"}},{\"id\":\"c4f904bf-6a22-4e3a-93ce-48ad28f4f3ce\",\"value\":{\"value\":\"La La Land\",\"key\":\"deceasedAddressTown\"}},{\"id\":\"da145130-c6cd-4464-9110-9d58596f889e\",\"value\":{\"value\":\"Middlesex\",\"key\":\"deceasedAddressCounty\"}},{\"id\":\"f58e1cce-0124-4370-8da9-59ebd70081e1\",\"value\":{\"value\":\"LA5 6TE\",\"key\":\"deceasedAddressPostCode\"}},{\"id\":\"e88b0567-7666-4b6e-9c4e-dc4f95cb86aa\",\"value\":{\"value\":\"\",\"key\":\"caseReference\"}}],\"openingDate\":\"2019-02-04T14:20:22.123\",\"ocrDataValidationWarnings\":[]},\"data_classification\":null,\"after_submit_callback_response\":null,\"callback_response_status_code\":null,\"callback_response_status\":null,\"delete_draft_response_status_code\":null,\"delete_draft_response_status\":null,\"security_classifications\":null}]}";
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
            .state("CaseStopped - N")
            .lastModifiedDate(LocalDateTime.of(2019,10,25,21,24,18, 143000000))
            .build();
    }

    private List<Task> getBulkScanTasks() {
        List<Task> tasks = new ArrayList<>();
        Task task1 = Task.builder()
            .caseTypeId("PROBATE_ExceptionRecord")
            .id("1585761273173755")
            .jurisdiction("PROBATE")
            .state("BulkScanning – NewPay")
            .lastModifiedDate(LocalDateTime.of(2020,04,02,14,58,01, 417000000)) //2020-04-02T14:58:01.417
            .build();
        Task task2 = Task.builder()
            .caseTypeId("PROBATE_ExceptionRecord")
            .id("1585827388328608")
            .jurisdiction("PROBATE")
            .state("BulkScanning – NewPay")
            .lastModifiedDate(LocalDateTime.of(2020,04,02,14,58,04, 985000000)) //2020-04-02T14:58:04.985
            .build();
        Task task3 = Task.builder()
            .caseTypeId("PROBATE_ExceptionRecord")
            .id("1585760187504931")
            .jurisdiction("PROBATE")
            .state("BulkScanning – NewPay")
            .lastModifiedDate(LocalDateTime.of(2020,04,02,14,57,58, 999000000)) //2020-04-02T14:57:58.999
            .build();
        tasks.add(task1);
        tasks.add(task2);
        tasks.add(task3);
        return tasks;
    }

    private String composeQuery(String date) {
        return "{\"query\":{\"bool\":{\"must\":[{\"range\":{\"last_modified\":{\"gte\":\""
            + date + "\"}}},{\"match\":{\"state\":{\"query\": \"Submitted AwaitingHWFDecision DARequested\","
            + "\"operator\": \"or\"}}}]}},\"size\": 500}";
    }
}
