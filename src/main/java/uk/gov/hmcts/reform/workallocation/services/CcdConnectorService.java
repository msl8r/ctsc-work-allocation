package uk.gov.hmcts.reform.workallocation.services;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.workallocation.ccd.CcdClient;
import uk.gov.hmcts.reform.workallocation.exception.CcdConnectionException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

@Service
@Slf4j
public class CcdConnectorService {

    public static final String FROM_PLACE_HOLDER = "[FROM]";
    public static final String TO_PLACE_HOLDER = "[TO]";
    public static final String CASE_TYPE_ID_DIVORCE = "DIVORCE";
    public static final String CASE_TYPE_ID_PROBATE = "GrantOfRepresentation";

    private final CcdClient ccdClient;

    @Value("${ccd.dry_run}")
    private boolean dryRun;

    @Value("${ccd.ctids}")
    private String ctids;

    private static final String QUERY_DIVORCE_TEMPLATE = "{\"query\":{\"bool\":{\"must\":[{\"range\":"
        + "{\"last_modified\":{\"gt\":\""
        + FROM_PLACE_HOLDER + "\", \"lte\":\"" + TO_PLACE_HOLDER + "\"}}}"
        + ",{\"match\":{\"state\":{\"query\": \"Submitted AwaitingHWFDecision DARequested\","
        + "\"operator\": \"or\"}}}]}},"
        + "\"_source\": [\"reference\", \"jurisdiction\", \"state\", \"last_modified\"],"
        + "\"size\": 1000}";

    private static final String QUERY_PROBATE_TEMPLATE = "{\"query\":{\"bool\":{\"must\":[{\"range\":"
        + "{\"last_modified\":{\"gt\":\"" + FROM_PLACE_HOLDER + "\",\"lte\":\"" + TO_PLACE_HOLDER + "\"}}},"
        + "{\"bool\":{\"should\":[{\"bool\":{\"must\":[{\"match\":{\"state\":\"CasePrinted\"}},{\"match\":"
        + "{\"data.evidenceHandled\":\"No\"}},{\"match\":{\"data.registryLocation\":\"ctsc\"}}]}},{\"bool\":"
        + "{\"must\":[{\"match\":{\"state\":\"CaseCreated\"}},{\"match\":{\"data.registryLocation\":\"ctsc\"}}]}},"
        + "{\"bool\":{\"must\":[{\"match\":{\"state\":"
        + "\"BOReadyForExamination\"}},{\"match\":{\"data.applicationType\":\"Personal\"}},{\"match\":"
        + "{\"data.caseType\":\"gop\"}},{\"match\":{\"data.registryLocation\":\"ctsc\"}}]}},{\"bool\":"
        + "{\"must\":[{\"match\":{\"state\":\"BOReadyForExamination\"}},{\"match\":{\"data.applicationType\":"
        + "\"Solicitor\"}},{\"match\":{\"data.caseType\":\"gop\"}},{\"match\":{\"data.registryLocation\":"
        + "\"ctsc\"}}]}},{\"bool\":{\"must\":[{\"match\":{\"state\":\"BOCaseStopped\"}},{\"match\":"
        + "{\"data.evidenceHandled\":\"Yes\"}},{\"match\":{\"data.registryLocation\":\"ctsc\"}}]}},"
        + "{\"bool\":{\"must\":[{\"match\":{\"state\":\"BOCaseStopped\"}},{\"match\":{\"data.evidenceHandled\":"
        + "\"No\"}},{\"match\":{\"data.registryLocation\":\"ctsc\"}}]}}]}}]}},\"_source\":[\"reference\","
        + "\"jurisdiction\",\"state\",\"last_modified\",\"data.applicationType\",\"data.evidenceHandled\","
        + "\"data.caseType\",\"data.registryLocation\"],\"size\":1000}";

    @Autowired
    public CcdConnectorService(CcdClient ccdClient) {
        this.ccdClient = ccdClient;
    }

    public Map<String, Object> searchDivorceCases(String userAuthToken,
                                                  String serviceToken,
                                                  String queryFromDateTime,
                                                  String queryToDateTime) throws CcdConnectionException {
        String query = QUERY_DIVORCE_TEMPLATE.replace(FROM_PLACE_HOLDER, queryFromDateTime)
            .replace(TO_PLACE_HOLDER, queryToDateTime);
        return searchCases(
            userAuthToken,
            serviceToken,
            query,
            CASE_TYPE_ID_DIVORCE
        );
    }

    public Map<String, Object> searchProbateCases(String userAuthToken,
                                                  String serviceToken,
                                                  String queryFromDateTime,
                                                  String queryToDateTime) throws CcdConnectionException {
        String query = QUERY_PROBATE_TEMPLATE.replace(FROM_PLACE_HOLDER, queryFromDateTime)
            .replace(TO_PLACE_HOLDER, queryToDateTime);
        return searchCases(
            userAuthToken,
            serviceToken,
            query,
            CASE_TYPE_ID_PROBATE
        );
    }

    private Map<String, Object> searchCases(String userAuthToken, String serviceToken, String query, String caseTypeId)
            throws CcdConnectionException {
        Map<String, Object> response;
        if (dryRun) {
            log.info("Running dry and not connecting to CCD");
            response = prepareDryResponse();
            return response;
        }
        try {
            response = ccdClient.searchCases(userAuthToken, serviceToken, caseTypeId, query);
            response.put("case_type_id", caseTypeId);
        } catch (Exception e) {
            throw new CcdConnectionException("Failed to connect ccd.", e);
        }
        return response;
    }

    private Map<String, Object> prepareDryResponse() {
        Map<String, Object> response = new HashMap<>();
        response.put("total", 0);
        response.put("cases", new ArrayList<>());
        return response;
    }
}
