package uk.gov.hmcts.reform.workallocation.model;

import net.serenitybdd.junit.runners.SerenityRunner;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import uk.gov.hmcts.reform.workallocation.exception.CaseTransformException;
import uk.gov.hmcts.reform.workallocation.services.CcdConnectorService;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;

@RunWith(SerenityRunner.class)
public class TaskTest {

    private Map<String, Object> divorce;
    private Map<String, Object> divorceException;
    private Map<String, Object> divorceEvidence;
    private Map<String, Object> probate;
    private Map<String, Object> probateCaveat;
    private Map<String, Object> probateException;
    private Map<String, Object> fr;
    private Map<String, Object> frException;

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

        divorceException = new HashMap<>();
        divorceException.put("id", 1563460551477777L);
        divorceException.put("jurisdiction", "DIVORCE");
        divorceException.put("state", "ScannedRecordReceived");
        divorceException.put("version", null);
        divorceException.put("case_type_id", "DIVORCE_ExceptionRecord");
        divorceException.put("created_date", null);
        divorceException.put("last_modified", "2020-07-20T14:36:20.962");
        divorceException.put("security_classification", "PUBLIC");

        divorceEvidence = new HashMap<>();
        divorceEvidence.put("id", 1563460551499999L);
        divorceEvidence.put("jurisdiction", "DIVORCE");
        divorceEvidence.put("version", null);
        divorceEvidence.put("case_type_id", "DIVORCE");
        divorceEvidence.put("created_date", null);
        divorceEvidence.put("last_modified", "2019-07-18T14:36:25.862");
        divorceEvidence.put("security_classification", "PUBLIC");

        probate = new HashMap<>();
        probate.put("id", 1572038226692222L);
        probate.put("jurisdiction", "PROBATE");
        probate.put("state", "BOCaseStopped");
        probate.put("version", null);
        probate.put("case_type_id", "GrantOfRepresentation");
        probate.put("created_date", null);
        probate.put("last_modified", "2019-10-25T21:24:18.143");
        probate.put("security_classification", null);
        Map<String, Object> caseData = new HashMap<>();
        probate.put("case_data", caseData);
        caseData.put("applicationType", "Personal");
        caseData.put("caseType", "gop");
        caseData.put("registryLocation", "ctsc");
        caseData.put("evidenceHandled", "No");

        probateCaveat = new HashMap<>();
        probateCaveat.put("id", 1572038226691111L);
        probateCaveat.put("jurisdiction", "PROBATE");
        probateCaveat.put("state", "CaveatRaised");
        probateCaveat.put("version", null);
        probateCaveat.put("case_type_id", "GrantOfRepresentation");
        probateCaveat.put("created_date", null);
        probateCaveat.put("last_modified", "2019-10-25T21:24:18.143");
        probateCaveat.put("security_classification", null);
        Map<String, Object> caveatCaseData = new HashMap<>();
        probateCaveat.put("case_data", caveatCaseData);
        caveatCaseData.put("applicationType", "Personal");
        caveatCaseData.put("registryLocation", "ctsc");

        probateException = new HashMap<>();
        probateException.put("id", 1572038226694444L);
        probateException.put("jurisdiction", "PROBATE");
        probateException.put("state", "ScannedRecordReceived");
        probateException.put("version", null);
        probateException.put("case_type_id", "PROBATE_ExceptionRecord");
        probateException.put("created_date", null);
        probateException.put("last_modified", "2019-10-25T21:24:18.143");
        probateException.put("security_classification", null);
        Map<String, Object> exceptionCaseData = new HashMap<>();
        probateException.put("case_data", exceptionCaseData);
        exceptionCaseData.put("containsPayments", "No");
        exceptionCaseData.put("journeyClassification", "NEW_APPLICATION");

        fr = new HashMap<>();
        fr.put("id", 1563666651495313L);
        fr.put("jurisdiction", "DIVORCE");
        fr.put("state", "applicationSubmitted");
        fr.put("version", null);
        fr.put("case_type_id", "FinancialRemedyMVP2");
        fr.put("created_date", null);
        fr.put("last_modified", "2019-07-18T14:36:25.862");
        fr.put("security_classification", "PUBLIC");

        frException = new HashMap<>();
        frException.put("id", 1563777751477777L);
        frException.put("jurisdiction", "DIVORCE");
        frException.put("state", "ScannedRecordReceived");
        frException.put("version", null);
        frException.put("case_type_id", "FINREM_ExceptionRecord");
        frException.put("created_date", null);
        frException.put("last_modified", "2020-07-20T14:36:20.962");
        frException.put("security_classification", "PUBLIC");
    }

    @Test
    public void testConvertCaseToTaskHappyPath() throws CaseTransformException {
        Task task = Task.fromCcdCase(divorce, CcdConnectorService.CASE_TYPE_ID_DIVORCE, null);
        assertEquals("1563460551495313", task.getId());
        assertEquals("DIVORCE", task.getJurisdiction());
        assertEquals("DIVORCE", task.getCaseTypeId());
    }

    @Test
    public void convertDivorceExceptionCaseToTaskHappyPath() throws CaseTransformException {
        Task task = Task.fromCcdCase(divorceException, CcdConnectorService.CASE_TYPE_ID_DIVORCE_EXCEPTION, null);
        assertEquals("1563460551477777", task.getId());
        assertEquals("DIVORCE", task.getJurisdiction());
        assertEquals("DIVORCE_ExceptionRecord", task.getCaseTypeId());
    }

    @Test
    public void divorceEvidenceHappyPath() throws CaseTransformException {
        Task task = Task.fromCcdCase(divorceEvidence, CcdConnectorService.CASE_TYPE_ID_DIVORCE, "evidenceHandled");
        assertEquals("1563460551499999", task.getId());
        assertEquals("DIVORCE", task.getJurisdiction());
        assertEquals("DIVORCE", task.getCaseTypeId());
    }


    @Test
    @SuppressWarnings("unchecked")
    public void probateGoPConversion() throws CaseTransformException {
        Task task = Task.fromCcdCase(probate, CcdConnectorService.PROBATE_CASE_TYPE_ID_GOP, null);
        assertEquals("CaseStoppedPersonalEvidenceNotHandled", task.getState());
        assertEquals("PROBATE", task.getJurisdiction());

        ((Map<String, Object>)probate.get("case_data")).put("applicationType", "Solicitor");
        task = Task.fromCcdCase(probate, CcdConnectorService.PROBATE_CASE_TYPE_ID_GOP, null);
        assertEquals("CaseStoppedSolicitorsEvidenceNotHandled", task.getState());
        assertEquals("PROBATE", task.getJurisdiction());

        ((Map<String, Object>)probate.get("case_data")).put("caseType", "intestacy");
        task = Task.fromCcdCase(probate, CcdConnectorService.PROBATE_CASE_TYPE_ID_GOP, null);
        assertEquals("CaseStoppedSolicitorsIntestacyEvidenceNotHandled", task.getState());

        ((Map<String, Object>)probate.get("case_data")).put("applicationType", "Personal");
        task = Task.fromCcdCase(probate, CcdConnectorService.PROBATE_CASE_TYPE_ID_GOP, null);
        assertEquals("CaseStoppedPersonalIntestacyEvidenceNotHandled", task.getState());


        ((Map<String, Object>)probate.get("case_data")).put("evidenceHandled", "Yes");
        task = Task.fromCcdCase(probate, CcdConnectorService.PROBATE_CASE_TYPE_ID_GOP, null);
        assertEquals("CaseStoppedPersonalIntestacyEvidenceHandled", task.getState());

        ((Map<String, Object>)probate.get("case_data")).put("applicationType", "Solicitor");
        task = Task.fromCcdCase(probate, CcdConnectorService.PROBATE_CASE_TYPE_ID_GOP, null);
        assertEquals("CaseStoppedSolicitorsIntestacyEvidenceHandled", task.getState());

        ((Map<String, Object>)probate.get("case_data")).put("caseType", "gop");
        task = Task.fromCcdCase(probate, CcdConnectorService.PROBATE_CASE_TYPE_ID_GOP, null);
        assertEquals("CaseStoppedSolicitorsEvidenceHandled", task.getState());

        ((Map<String, Object>)probate.get("case_data")).put("applicationType", "Personal");
        ((Map<String, Object>)probate.get("case_data")).put("caseType", "gop");
        task = Task.fromCcdCase(probate, CcdConnectorService.PROBATE_CASE_TYPE_ID_GOP, null);
        assertEquals("CaseStoppedPersonalEvidenceHandled", task.getState());

        probate.put("state", "CaseCreated");
        task = Task.fromCcdCase(probate, CcdConnectorService.PROBATE_CASE_TYPE_ID_GOP, null);
        assertEquals("CaseCreated", task.getState());

        probate.put("state", "CasePrinted");
        ((Map<String, Object>)probate.get("case_data")).put("evidenceHandled", "No");
        task = Task.fromCcdCase(probate, CcdConnectorService.PROBATE_CASE_TYPE_ID_GOP, null);
        assertEquals("AwaitingDocumentationPersonalEvidenceNotHandled", task.getState());

        probate.put("state", "CasePrinted");
        ((Map<String, Object>)probate.get("case_data")).put("applicationType", "Solicitor");
        ((Map<String, Object>)probate.get("case_data")).put("evidenceHandled", "No");
        task = Task.fromCcdCase(probate, CcdConnectorService.PROBATE_CASE_TYPE_ID_GOP, null);
        assertEquals("AwaitingDocumentationSolicitorEvidenceNotHandled", task.getState());

        probate.put("state", "BOReadyForExamination");
        ((Map<String, Object>)probate.get("case_data")).put("applicationType", "Personal");
        ((Map<String, Object>)probate.get("case_data")).put("caseType", "gop");
        task = Task.fromCcdCase(probate, CcdConnectorService.PROBATE_CASE_TYPE_ID_GOP, null);
        assertEquals("ReadyForExaminationPersonal", task.getState());

        ((Map<String, Object>)probate.get("case_data")).put("applicationType", "Solicitor");
        ((Map<String, Object>)probate.get("case_data")).put("caseType", "gop");
        task = Task.fromCcdCase(probate, CcdConnectorService.PROBATE_CASE_TYPE_ID_GOP, null);
        assertEquals("ReadyForExaminationSolicitor", task.getState());
    }

    @Test
    @SuppressWarnings("unchecked")
    public void probateCaveatConversion() throws CaseTransformException {
        Task task = Task.fromCcdCase(probateCaveat, CcdConnectorService.PROBATE_CASE_TYPE_ID_CAVEAT, null);
        assertEquals("CaveatPersonal", task.getState());
        assertEquals("PROBATE", task.getJurisdiction());

        ((Map<String, Object>)probateCaveat.get("case_data")).put("applicationType", "Solicitor");
        task = Task.fromCcdCase(probateCaveat, CcdConnectorService.PROBATE_CASE_TYPE_ID_CAVEAT, null);
        assertEquals("CaveatSolicitor", task.getState());
        assertEquals("PROBATE", task.getJurisdiction());
    }

    @Test
    @SuppressWarnings("unchecked")
    public void probateBspExceptionConversion() throws CaseTransformException {
        Task task = Task.fromCcdCase(probateException, CcdConnectorService.PROBATE_CASE_TYPE_ID_BSP_EXCEPTION, null);
        assertEquals("BulkScanNewApplicationsReceivedWithoutPayments", task.getState());
        assertEquals("PROBATE", task.getJurisdiction());

        ((Map<String, Object>)probateException.get("case_data")).put("containsPayments", "Yes");
        task = Task.fromCcdCase(probateException, CcdConnectorService.PROBATE_CASE_TYPE_ID_BSP_EXCEPTION, null);
        assertEquals("BulkScanNewApplicationsReceivedWithPayments", task.getState());
        assertEquals("PROBATE", task.getJurisdiction());

        ((Map<String, Object>)probateException.get("case_data")).put("containsPayments", "No");
        ((Map<String, Object>)probateException.get("case_data")).put("journeyClassification",
                "SUPPLEMENTARY_EVIDENCE_WITH_OCR");
        task = Task.fromCcdCase(probateException, CcdConnectorService.PROBATE_CASE_TYPE_ID_BSP_EXCEPTION, null);
        assertEquals("BulkScanSupplementaryEvidenceWithoutPayments", task.getState());
        assertEquals("PROBATE", task.getJurisdiction());

        ((Map<String, Object>)probateException.get("case_data")).put("journeyClassification", "SUPPLEMENTARY_EVIDENCE");
        task = Task.fromCcdCase(probateException, CcdConnectorService.PROBATE_CASE_TYPE_ID_BSP_EXCEPTION, null);
        assertEquals("BulkScanSupplementaryEvidenceWithoutPayments", task.getState());
        assertEquals("PROBATE", task.getJurisdiction());
    }

    @Test
    @SuppressWarnings("unchecked")
    public void frTask() throws CaseTransformException {
        Task task = Task.fromCcdCase(fr, CcdConnectorService.FR_CASE_TYPE, null);
        assertEquals("ConsentAppCreated", task.getState());
        assertEquals("DIVORCE", task.getJurisdiction());

        fr.put("state", "consentOrderApproved");
        task = Task.fromCcdCase(fr, CcdConnectorService.FR_CASE_TYPE, null);
        assertEquals("consentOrderApproved", task.getState());
        assertEquals("DIVORCE", task.getJurisdiction());

        fr.put("state", "orderMade");
        task = Task.fromCcdCase(fr, CcdConnectorService.FR_CASE_TYPE, null);
        assertEquals("consentOrderNotApproved", task.getState());
        assertEquals("DIVORCE", task.getJurisdiction());
    }

    @Test
    @SuppressWarnings("unchecked")
    public void frExceptionTask() throws CaseTransformException {
        Task task = Task.fromCcdCase(frException, CcdConnectorService.FR_EXCEPTION_CASE_TYPE, null);
        assertEquals("ScannedRecordReceivedFormA", task.getState());
        assertEquals("DIVORCE", task.getJurisdiction());
    }

    @Test(expected = CaseTransformException.class)
    public void testConvertCaseToTaskWithoutId() throws CaseTransformException {
        divorce.remove("id");
        Task.fromCcdCase(divorce, CcdConnectorService.PROBATE_CASE_TYPE_ID_GOP, null);
    }

    @Test(expected = CaseTransformException.class)
    public void testConvertCaseToTaskWitWrongDateFormat() throws CaseTransformException {
        divorce.put("last_modified", "asdasd121234");
        Task.fromCcdCase(divorce, CcdConnectorService.PROBATE_CASE_TYPE_ID_GOP, null);
    }
}
