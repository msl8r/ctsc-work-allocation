package uk.gov.hmcts.reform.workallocation.model;

import org.junit.Before;
import org.junit.Test;
import uk.gov.hmcts.reform.workallocation.exception.CaseTransformException;
import uk.gov.hmcts.reform.workallocation.services.CcdConnectorService;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;

public class TaskTest {

    private Map<String, Object> divorce;
    private Map<String, Object> probate;

    @Before
    public void setUp() {
        divorce = new HashMap<>();
        divorce.put("id", 1563460551495313L);
        divorce.put("jurisdiction", "DIVORCE");
        divorce.put("state", "Submitted");
        divorce.put("version", null);
        divorce.put("case_type_id", "DIVORCE");
        divorce.put("created_date", null);
        divorce.put("last_modified", "2019-07-18T14:36:25.862");
        divorce.put("security_classification", "PUBLIC");

        probate = new HashMap<>();
        probate.put("id", 1572038226693576L);
        probate.put("jurisdiction", "PROBATE");
        probate.put("state", "BOReadyForExamination");
        probate.put("version", null);
        probate.put("case_type_id", "GrantOfRepresentation");
        probate.put("created_date", null);
        probate.put("last_modified", "2019-10-25T21:24:18.143");
        probate.put("security_classification", null);
        Map<String, Object> caseData = new HashMap<>();
        probate.put("case_data", caseData);
        caseData.put("evidenceHandled", "Yes");
        caseData.put("applicationType", "Personal");
    }

    @Test
    public void testConvertCaseToTaskHappyPath() throws CaseTransformException {
        Task task = Task.fromCcdCase(divorce, CcdConnectorService.CASE_TYPE_ID_DIVORCE);
        assertEquals("1563460551495313", task.getId());
        assertEquals("DIVORCE", task.getJurisdiction());
        assertEquals("DIVORCE", task.getCaseTypeId());
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testProbateConversion() throws CaseTransformException {
        Task task = Task.fromCcdCase(probate, CcdConnectorService.CASE_TYPE_ID_PROBATE);
        assertEquals("ReadyforExamination-Personal", task.getState());
        assertEquals("PROBATE", task.getJurisdiction());

        ((Map<String, Object>)probate.get("case_data")).put("applicationType", "Solicitor");
        task = Task.fromCcdCase(probate, CcdConnectorService.CASE_TYPE_ID_PROBATE);
        assertEquals("ReadyforExamination-Solicitor", task.getState());
    }


    @Test(expected = CaseTransformException.class)
    public void testConvertCaseToTaskWithoutId() throws CaseTransformException {
        divorce.remove("id");
        Task.fromCcdCase(divorce, CcdConnectorService.CASE_TYPE_ID_PROBATE);
    }

    @Test(expected = CaseTransformException.class)
    public void testConvertCaseToTaskWitWrongDateFormat() throws CaseTransformException {
        divorce.put("last_modified", "asdasd121234");
        Task.fromCcdCase(divorce, CcdConnectorService.CASE_TYPE_ID_PROBATE);
    }
}
