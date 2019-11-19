package uk.gov.hmcts.reform.workallocation.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.test.util.ReflectionTestUtils;
import uk.gov.hmcts.reform.workallocation.ccd.CcdClient;
import uk.gov.hmcts.reform.workallocation.exception.CcdConnectionException;
import uk.gov.hmcts.reform.workallocation.services.CcdConnectorService;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

public class CcdConnectionServiceTest {

    @Mock
    private CcdClient ccdClient;

    private CcdConnectorService ccdConnectorService;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        ccdConnectorService = new CcdConnectorService(ccdClient);
        ReflectionTestUtils.setField(ccdConnectorService, "ctids", "DIVORCE");
        ReflectionTestUtils.setField(ccdConnectorService, "dryRun", true);
    }

    @Test
    public void testDryRun() throws CcdConnectionException {
        Map<String, Object> result = ccdConnectorService.searchCases("", "", "", "");
        Assert.assertEquals(0, result.get("total"));
        Assert.assertTrue(((List)result.get("cases")).isEmpty());
    }

    @Test
    public void testNormalRun() throws CcdConnectionException, IOException {
        ReflectionTestUtils.setField(ccdConnectorService, "dryRun", false);
        when(ccdClient.searchCases(any(), any(), any(), any())).thenReturn(caseSearchResult());
        Map<String, Object> result = ccdConnectorService.searchCases("", "", "", "");
        Assert.assertEquals(1, result.get("total"));
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
    //CHECKSTYLE:ON
}
