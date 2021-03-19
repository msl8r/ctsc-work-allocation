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
    public static final String CASE_TYPE_ID_DIVORCE_EXCEPTION = "DIVORCE_ExceptionRecord";
    public static final String PROBATE_CASE_TYPE_ID_GOP = "GrantOfRepresentation";
    public static final String PROBATE_CASE_TYPE_ID_CAVEAT = "Caveat";
    public static final String PROBATE_CASE_TYPE_ID_BSP_EXCEPTION = "PROBATE_ExceptionRecord";
    public static final String FR_CASE_TYPE = "FinancialRemedyMVP2";
    public static final String FR_EXCEPTION_CASE_TYPE = "FINREM_ExceptionRecord";

    private final CcdClient ccdClient;

    @Value("${ccd.dry_run}")
    private boolean dryRun;

    @Value("${ccd.enable_probate}")
    private boolean enableProbate;

    @Value("${ccd.ctids}")
    private String ctids;

    private static final String DATE_RANGE = "{\"query\":{\"bool\":{\"must\":[{\"range\""
            + ":{\"last_modified\":{\"gt\":\"" + FROM_PLACE_HOLDER + "\",\"lte\":\"" + TO_PLACE_HOLDER + "\"}}},";

    private static final String QUERY_DIVORCE_EVIDENCE_HANDLED_TEMPLATE = DATE_RANGE
        + "{\"bool\":{\"should\":[{\"bool\":{\"must\":[{\"match\":{\"data.evidenceHandled\":\"No\"}},"
        + "{\"match\":{\"data.D8DivorceUnit\":\"serviceCentre\"}}]}}]}}]}},"
        + "\"_source\":[\"reference\",\"jurisdiction\",\"state\",\"last_modified\"],\"size\":1000}";

    private static final String QUERY_DIVORCE_TEMPLATE = DATE_RANGE
        + "{\"match\":{\"state\":{\"query\": \"Submitted AwaitingHWFDecision DARequested\","
        + "\"operator\": \"or\"}}}]}},"
        + "\"_source\": [\"reference\", \"jurisdiction\", \"state\", \"last_modified\"],"
        + "\"size\": 1000}";

    private static final String QUERY_DIVORCE_EXCEPTION_TEMPLATE = DATE_RANGE
        + "{\"match\":{\"state\":{\"query\":\"ScannedRecordReceived\",\"operator\":\"or\"}}}]}},"
        + "\"_source\":[\"reference\",\"jurisdiction\",\"state\",\"last_modified\"],\"size\":1000}";

    private static final String PROBATE_GOP_QUERY = DATE_RANGE
        + "{\"bool\":{\"should\":[{\"bool\":{\"must\":[{\"match\":{\"state\":\"CasePrinted\"}},"
        + "{\"match\":{\"data.evidenceHandled\":\"No\"}},{\"match\":{\"data.applicationType\":\"Personal\"}},"
        + "{\"match\":{\"data.registryLocation\":\"ctsc\"}},{\"match\":{\"data.caseType\":\"gop\"}}]}},"
        + "{\"bool\":{\"must\":[{\"match\":{\"state\":\"CasePrinted\"}},{\"match\":{\"data.evidenceHandled\":\"No\"}},"
        + "{\"match\":{\"data.applicationType\":\"Solicitor\"}},{\"match\":{\"data.registryLocation\":\"ctsc\"}},"
        + "{\"match\":{\"data.caseType\":\"gop\"}}]}},{\"bool\":{\"must\":[{\"match\":{\"state\":\"CaseCreated\"}},"
        + "{\"match\":{\"data.registryLocation\":\"ctsc\"}}]}},{\"bool\":{\"must\":[{\"match\":{\"state\":"
        + "\"BOReadyForExamination\"}},{\"match\":{\"data.evidenceHandled\":\"No\"}},{\"match\":"
        + "{\"data.applicationType\":\"Personal\"}},{\"match\":{\"data.caseType\":\"gop\"}},"
        + "{\"match\":{\"data.registryLocation\":\"ctsc\"}}]}},{\"bool\":{\"must\":[{\"match\":"
        + "{\"state\":\"BOReadyForExamination\"}},{\"match\":{\"data.evidenceHandled\":\"No\"}},"
        + "{\"match\":{\"data.applicationType\":\"Solicitor\"}},{\"match\":{\"data.caseType\":\"gop\"}},"
        + "{\"match\":{\"data.registryLocation\":\"ctsc\"}}]}},{\"bool\":{\"must\":[{\"match\":"
        + "{\"state\":\"BOCaseStopped\"}},{\"match\":{\"data.applicationType\":\"Personal\"}},{\"match\":"
        + "{\"data.evidenceHandled\":\"No\"}},{\"match\":{\"data.caseType\":\"gop\"}},{\"match\":"
        + "{\"data.registryLocation\":\"ctsc\"}}]}},{\"bool\":{\"must\":[{\"match\":{\"state\":\"BOCaseStopped\"}},"
        + "{\"match\":{\"data.applicationType\":\"Solicitor\"}},{\"match\":{\"data.evidenceHandled\":\"No\"}},"
        + "{\"match\":{\"data.caseType\":\"gop\"}},{\"match\":{\"data.registryLocation\":\"ctsc\"}}]}},"
        + "{\"bool\":{\"must\":[{\"match\":{\"state\":\"BOCaseStopped\"}},{\"match\":{\"data.applicationType\":"
        + "\"Personal\"}},{\"match\":{\"data.caseType\":\"intestacy\"}},{\"match\":{\"data.evidenceHandled\":\"No\"}},"
        + "{\"match\":{\"data.registryLocation\":\"ctsc\"}}]}},{\"bool\":{\"must\":[{\"match\":{\"state\":"
        + "\"BOCaseStopped\"}},{\"match\":{\"data.applicationType\":\"Solicitor\"}},{\"match\":{\"data.solsWillType\":"
        + "\"NoWill\"}},{\"match\":{\"data.evidenceHandled\":\"No\"}},{\"match\":{\"data.registryLocation\":"
        + "\"ctsc\"}}]}},{\"bool\":{\"must\":[{\"match\":{\"state\":\"BOCaseStopped\"}},{\"match\":"
        + "{\"data.applicationType\":\"Personal\"}},{\"match\":{\"data.evidenceHandled\":\"Yes\"}},{\"match\":"
        + "{\"data.caseType\":\"gop\"}},{\"match\":{\"data.registryLocation\":\"ctsc\"}}]}},{\"bool\":{\"must\":"
        + "[{\"match\":{\"state\":\"BOCaseStopped\"}},{\"match\":{\"data.applicationType\":\"Solicitor\"}},"
        + "{\"match\":{\"data.evidenceHandled\":\"Yes\"}},{\"match\":{\"data.caseType\":\"gop\"}},{\"match\":"
        + "{\"data.registryLocation\":\"ctsc\"}}]}},{\"bool\":{\"must\":[{\"match\":{\"state\":\"BOCaseStopped\"}},"
        + "{\"match\":{\"data.applicationType\":\"Personal\"}},{\"match\":{\"data.caseType\":\"intestacy\"}},{\"match\""
        + ":{\"data.evidenceHandled\":\"Yes\"}},{\"match\":{\"data.registryLocation\":\"ctsc\"}}]}},{\"bool\":{\"must\""
        + ":[{\"match\":{\"state\":\"BOCaseStopped\"}},{\"match\":{\"data.applicationType\":\"Solicitor\"}},"
        + "{\"match\":{\"data.solsWillType\":\"NoWill\"}},{\"match\":{\"data.evidenceHandled\":\"Yes\"}},"
        + "{\"match\":{\"data.registryLocation\":\"ctsc\"}}]}}]}}]}},\"_source\":[\"reference\",\"jurisdiction\","
        + "\"state\",\"last_modified\",\"data.applicationType\",\"data.evidenceHandled\",\"data.caseType\","
        + "\"data.registryLocation\",\"data.solsWillType\"],\"size\":1000}";

    private static final String PROBATE_CAVEAT_QUERY = DATE_RANGE
        + "{\"bool\":{\"should\":[{\"bool\":{\"must\":[{\"match\":{\"state"
        + "\":\"CaveatRaised\"}},{\"match\":{\"data.applicationType\":\"Personal\"}},"
        + "{\"match\":{\"data.registryLocation\":\"ctsc\"}}]}},"
        + "{\"bool\":{\"must\":[{\"match\":{\"state\":\"CaveatRaised\"}},"
        + "{\"match\":{\"data.applicationType\":\"Solicitor\"}},{\"match\":{\"data"
        + ".registryLocation\":\"ctsc\"}}]}}]}}]}},\"_source\":[\"reference\",\"jurisdiction\","
        + "\"state\",\"last_modified\",\"data.applicationType\",\"data.evidenceHandled\",\"data"
        + ".caseType\",\"data.registryLocation\"],\"size\":1000}";

    private static final String PROBATE_BSP_EXCEPTION_QUERY = DATE_RANGE
        + "{\"bool\":{\"should\":[{\"bool\":{\"must\":[{\"match\":{\"state"
        + "\":\"ScannedRecordReceived\"}},{\"match\":{\"data"
        + ".journeyClassification\":\"NEW_APPLICATION\"}},{\"match\":{\"data"
        + ".containsPayments\":\"Yes\"}}]}},"
        + "{\"bool\":{\"must\":[{\"match\":{\"state\":\"ScannedRecordReceived\"}},"
        + "{\"match\":{\"data.journeyClassification\":\"NEW_APPLICATION\"}},{\"match\":{\"data"
        + ".containsPayments\":\"No\"}}]}},"
        + "{\"bool\":{\"must\":[{\"match\":{\"state\":\"ScannedRecordReceived\"}},"
        + "{\"match\":{\"data.journeyClassification\":\"SUPPLEMENTARY_EVIDENCE_WITH_OCR\"}},"
        + "{\"match\":{\"data.containsPayments\":\"No\"}}]}},"
        + "{\"bool\":{\"must\":[{\"match\":{\"state\":\"ScannedRecordReceived\"}},"
        + "{\"match\":{\"data.journeyClassification\":\"SUPPLEMENTARY_EVIDENCE\"}},"
        + "{\"match\":{\"data.containsPayments\":\"No\"}}]}}]}}]}},\"_source\":[\"reference\","
        + "\"jurisdiction\",\"state\",\"last_modified\",\"data.applicationType\",\"data"
        + ".evidenceHandled\",\"data.caseType\",\"data.registryLocation\",\"data.containsPayments\","
        + "\"data.journeyClassification\"],\"size\":1000}";

    private static final String FR_QUERY = DATE_RANGE
        + "{\"match\":{\"state\":{\"query\":\"applicationSubmitted consentOrderApproved orderMade\","
        + "\"operator\":\"or\"}}}]}},\"_source\":[\"reference\",\"jurisdiction\",\"state\","
        + "\"last_modified\"],\"size\":1000}";

    private static final String FR_EXCEPTION_QUERY = DATE_RANGE
        + "{\"bool\":{\"must\":[{\"match\":{\"state\":\"ScannedRecordReceived\"}},"
        + "{\"match\":{\"data.scannedDocuments.value.subtype\":\"FormA\"}}]}}]}},"
        + "\"_source\":[\"reference\",\"jurisdiction\",\"state\",\"last_modified\","
        + "\"data.scannedDocuments\"],\"size\":1000}";

    @Autowired
    public CcdConnectorService(CcdClient ccdClient) {
        this.ccdClient = ccdClient;
    }

    public Map<String, Object> searchDivorceEvidenceHandledCases(String userAuthToken,
                                                  String serviceToken,
                                                  String queryFromDateTime,
                                                  String queryToDateTime,
                                                  String caseTypeId) throws CcdConnectionException {
        String query = QUERY_DIVORCE_EVIDENCE_HANDLED_TEMPLATE.replace(FROM_PLACE_HOLDER, queryFromDateTime)
                .replace(TO_PLACE_HOLDER, queryToDateTime);

        Map<String, Object> evidenceHandledCases = searchCases(
                userAuthToken,
                serviceToken,
                query,
                caseTypeId
        );
        evidenceHandledCases.put("EVIDENCE_FLOW", "evidenceHandled");
        return evidenceHandledCases;
    }

    public Map<String, Object> searchDivorceCases(String userAuthToken,
                                                  String serviceToken,
                                                  String queryFromDateTime,
                                                  String queryToDateTime,
                                                  String caseTypeId) throws CcdConnectionException {
        String query = QUERY_DIVORCE_TEMPLATE.replace(FROM_PLACE_HOLDER, queryFromDateTime)
            .replace(TO_PLACE_HOLDER, queryToDateTime);

        if (caseTypeId.equalsIgnoreCase(CASE_TYPE_ID_DIVORCE_EXCEPTION)) {
            query = QUERY_DIVORCE_EXCEPTION_TEMPLATE.replace(FROM_PLACE_HOLDER, queryFromDateTime)
                    .replace(TO_PLACE_HOLDER, queryToDateTime);
        }
        return searchCases(
            userAuthToken,
            serviceToken,
            query,
            caseTypeId
        );
    }

    public Map<String, Object> findProbateCases(String userAuthToken,
                                                  String serviceToken,
                                                  String queryFromDateTime,
                                                  String queryToDateTime,
                                                  String caseTypeId) throws CcdConnectionException {
        String query = PROBATE_GOP_QUERY.replace(FROM_PLACE_HOLDER, queryFromDateTime)
                .replace(TO_PLACE_HOLDER, queryToDateTime);

        if (caseTypeId.equalsIgnoreCase(PROBATE_CASE_TYPE_ID_CAVEAT)) {
            query = PROBATE_CAVEAT_QUERY.replace(FROM_PLACE_HOLDER, queryFromDateTime)
                    .replace(TO_PLACE_HOLDER, queryToDateTime);
        }

        if (caseTypeId.equalsIgnoreCase(PROBATE_CASE_TYPE_ID_BSP_EXCEPTION)) {
            query = PROBATE_BSP_EXCEPTION_QUERY.replace(FROM_PLACE_HOLDER, queryFromDateTime)
                    .replace(TO_PLACE_HOLDER, queryToDateTime);
        }
        log.info("enableProbate.{} ", enableProbate);
        if (enableProbate) {
            return searchCases(
                userAuthToken,
                serviceToken,
                query,
                caseTypeId
            );
        } else {
            /*
              This is done just to prevent Probate to do ES query.
              If Probate query need to turn on then change the change
              enable_probate: false to enable_probate: true in application
              yaml
             */
            log.info("Do not query probate ES.");
            Map<String, Object> response = prepareDryResponse();
            response.put("case_type_id", caseTypeId);
            return response;
        }
    }

    public Map<String, Object> findFinancialRemedyCases(String userAuthToken,
                                                        String serviceToken,
                                                        String queryFromDateTime,
                                                        String queryToDateTime,
                                                        String caseTypeId) throws CcdConnectionException {
        String query = FR_QUERY.replace(FROM_PLACE_HOLDER, queryFromDateTime)
            .replace(TO_PLACE_HOLDER, queryToDateTime);

        if (caseTypeId.equalsIgnoreCase(FR_EXCEPTION_CASE_TYPE)) {
            query = FR_EXCEPTION_QUERY.replace(FROM_PLACE_HOLDER, queryFromDateTime)
                .replace(TO_PLACE_HOLDER, queryToDateTime);
        }

        return searchCases(
            userAuthToken,
            serviceToken,
            query,
            caseTypeId);
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
