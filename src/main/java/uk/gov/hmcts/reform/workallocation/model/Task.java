package uk.gov.hmcts.reform.workallocation.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import uk.gov.hmcts.reform.workallocation.exception.CaseTransformException;
import uk.gov.hmcts.reform.workallocation.services.CcdConnectorService;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString(callSuper = true)
@EqualsAndHashCode
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonNaming(PropertyNamingStrategy.SnakeCaseStrategy.class)
public class Task {

    private String id;
    private String state;
    private String jurisdiction;
    private String caseTypeId;
    private LocalDateTime lastModifiedDate;

    public static Task fromCcdCase(Map<String, Object> caseData, String caseTypeId, String evidenceFlow)
            throws CaseTransformException {
        if (CcdConnectorService.CASE_TYPE_ID_DIVORCE.equals(caseTypeId)
                && evidenceFlow != null) {
            caseData.put("state", "SupplementaryEvidence");
            return fromCcdDivorceCase(caseData, caseTypeId);
        }
        if ((CcdConnectorService.CASE_TYPE_ID_DIVORCE.equals(caseTypeId)
                || CcdConnectorService.CASE_TYPE_ID_DIVORCE_EXCEPTION.equals(caseTypeId)) && evidenceFlow == null) {
            return fromCcdDivorceCase(caseData, caseTypeId);
        }
        if (CcdConnectorService.PROBATE_CASE_TYPE_ID_GOP.equals(caseTypeId)
                || CcdConnectorService.PROBATE_CASE_TYPE_ID_CAVEAT.equals(caseTypeId)
                || CcdConnectorService.PROBATE_CASE_TYPE_ID_BSP_EXCEPTION.equals(caseTypeId)) {
            return fromCcdProbateCase(caseData, caseTypeId);
        }
        if (CcdConnectorService.FR_CASE_TYPE.equals(caseTypeId)
            || CcdConnectorService.FR_EXCEPTION_CASE_TYPE.equals(caseTypeId)) {
            return fromCcdFinancialRemedyCase(caseData, caseTypeId);
        }
        throw new CaseTransformException("Unknown case type: " + caseTypeId);
    }

    private static Task fromCcdDivorceCase(Map<String, Object> caseData, String caseTypeId)
            throws CaseTransformException {
        try {
            LocalDateTime lastModifiedDate = LocalDateTime.parse(caseData.get("last_modified").toString());
            return Task.builder()
                .id(((Long)caseData.get("id")).toString())
                .state((String) caseData.get("state"))
                .jurisdiction((String) caseData.get("jurisdiction"))
                .caseTypeId(caseTypeId)
                .lastModifiedDate(lastModifiedDate)
                .build();
        } catch (Exception e) {
            throw new CaseTransformException("Failed to transform the case", e);
        }
    }

    private static Task fromCcdProbateCase(Map<String, Object> caseData, String caseTypeId)
            throws CaseTransformException {
        try {
            LocalDateTime lastModifiedDate = LocalDateTime.parse(caseData.get("last_modified").toString());
            return Task.builder()
                .id(((Long)caseData.get("id")).toString())
                .state(getProbateState(caseData))
                .jurisdiction((String) caseData.get("jurisdiction"))
                .caseTypeId(caseTypeId)
                .lastModifiedDate(lastModifiedDate)
                .build();
        } catch (Exception e) {
            throw new CaseTransformException("Failed to transform the case", e);
        }
    }

    private static Task fromCcdFinancialRemedyCase(Map<String, Object> caseData, String caseTypeId)
        throws CaseTransformException {
        try {
            LocalDateTime lastModifiedDate = LocalDateTime.parse(caseData.get("last_modified").toString());
            return Task.builder()
                .id(((Long)caseData.get("id")).toString())
                .state(getFrState(caseData))
                .jurisdiction((String) caseData.get("jurisdiction"))
                .caseTypeId(caseTypeId)
                .lastModifiedDate(lastModifiedDate)
                .build();
        } catch (Exception e) {
            throw new CaseTransformException("Failed to transform fr the case", e);
        }
    }

    @SuppressWarnings("unchecked")
    private static String getProbateState(Map<String, Object> caseData) {
        String state = (String) caseData.get("state");
        Map<String, Object> caseProperties = (Map<String, Object>) caseData.get("case_data");

        if ("CaseCreated".equals(state)) {
            return "CaseCreated";
        }
        if ("CasePrinted".equals(state) && "Personal".equals(caseProperties.get("applicationType"))
            && "gop".equals(caseProperties.get("caseType")) && "No".equals(caseProperties.get("evidenceHandled"))
            && "ctsc".equals(caseProperties.get("registryLocation"))) {
            return "AwaitingDocumentationPersonalEvidenceNotHandled";
        }
        if ("CasePrinted".equals(state) && "Solicitor".equals(caseProperties.get("applicationType"))
            && "gop".equals(caseProperties.get("caseType")) && "No".equals(caseProperties.get("evidenceHandled"))
            && "ctsc".equals(caseProperties.get("registryLocation"))) {
            return "AwaitingDocumentationSolicitorEvidenceNotHandled";
        }
        if ("BOReadyForExamination".equals(state) && "Personal".equals(caseProperties.get("applicationType"))
            && "gop".equals(caseProperties.get("caseType")) && "No".equals(caseProperties.get("evidenceHandled"))
            && "ctsc".equals(caseProperties.get("registryLocation"))) {
            return "ReadyForExaminationPersonal";
        }
        if ("BOReadyForExamination".equals(state) && "Solicitor".equals(caseProperties.get("applicationType"))
            && "gop".equals(caseProperties.get("caseType")) && "No".equals(caseProperties.get("evidenceHandled"))
            && "ctsc".equals(caseProperties.get("registryLocation"))) {
            return "ReadyForExaminationSolicitor";
        }
        if ("BOCaseStopped".equals(state) && "Personal".equals(caseProperties.get("applicationType"))
                && "gop".equals(caseProperties.get("caseType")) && "No".equals(caseProperties.get("evidenceHandled"))) {
            return "CaseStoppedPersonalEvidenceNotHandled";
        }
        if ("BOCaseStopped".equals(state) && "Solicitor".equals(caseProperties.get("applicationType"))
                && "gop".equals(caseProperties.get("caseType")) && "No".equals(caseProperties.get("evidenceHandled"))) {
            return "CaseStoppedSolicitorsEvidenceNotHandled";
        }
        if ("BOCaseStopped".equals(state) && "Personal".equals(caseProperties.get("applicationType"))
                && "intestacy".equals(caseProperties.get("caseType"))
                && "No".equals(caseProperties.get("evidenceHandled"))) {
            return "CaseStoppedPersonalIntestacyEvidenceNotHandled";
        }
        if ("BOCaseStopped".equals(state) && "Solicitor".equals(caseProperties.get("applicationType"))
                && "intestacy".equals(caseProperties.get("caseType"))
                && "No".equals(caseProperties.get("evidenceHandled"))) {
            return "CaseStoppedSolicitorsIntestacyEvidenceNotHandled";
        }
        if ("BOCaseStopped".equals(state) && "Personal".equals(caseProperties.get("applicationType"))
            && "gop".equals(caseProperties.get("caseType")) && "Yes".equals(caseProperties.get("evidenceHandled"))) {
            return "CaseStoppedPersonalEvidenceHandled";
        }
        if ("BOCaseStopped".equals(state) && "Solicitor".equals(caseProperties.get("applicationType"))
            && "gop".equals(caseProperties.get("caseType")) && "Yes".equals(caseProperties.get("evidenceHandled"))) {
            return "CaseStoppedSolicitorsEvidenceHandled";
        }
        if ("BOCaseStopped".equals(state) && "Personal".equals(caseProperties.get("applicationType"))
            && "intestacy".equals(caseProperties.get("caseType"))
            && "Yes".equals(caseProperties.get("evidenceHandled"))) {
            return "CaseStoppedPersonalIntestacyEvidenceHandled";
        }
        if ("BOCaseStopped".equals(state) && "Solicitor".equals(caseProperties.get("applicationType"))
            && "intestacy".equals(caseProperties.get("caseType"))
            && "Yes".equals(caseProperties.get("evidenceHandled"))) {
            return "CaseStoppedSolicitorsIntestacyEvidenceHandled";
        }
        if ("CaveatRaised".equals(state) && "Personal".equals(caseProperties.get("applicationType"))) {
            return "CaveatPersonal";
        }
        if ("CaveatRaised".equals(state) && "Solicitor".equals(caseProperties.get("applicationType"))) {
            return "CaveatSolicitor";
        }
        if ("ScannedRecordReceived".equals(state) && "No".equals(caseProperties.get("containsPayments"))
                && "NEW_APPLICATION".equals(caseProperties.get("journeyClassification"))) {
            return "BulkScanNewApplicationsReceivedWithoutPayments";
        }
        if ("ScannedRecordReceived".equals(state) && "Yes".equals(caseProperties.get("containsPayments"))
                && "NEW_APPLICATION".equals(caseProperties.get("journeyClassification"))) {
            return "BulkScanNewApplicationsReceivedWithPayments";
        }
        if ("ScannedRecordReceived".equals(state) && "No".equals(caseProperties.get("containsPayments"))
                && ("SUPPLEMENTARY_EVIDENCE_WITH_OCR".equals(caseProperties.get("journeyClassification")))
                || ("SUPPLEMENTARY_EVIDENCE".equals(caseProperties.get("journeyClassification")))) {
            return "BulkScanSupplementaryEvidenceWithoutPayments";
        }
        return state;
    }

    @SuppressWarnings("unchecked")
    private static String getFrState(Map<String, Object> caseData) {
        String state = (String) caseData.get("state");
        if ("applicationSubmitted".equals(state)) {
            return "ConsentAppCreated";
        }
        if ("consentOrderApproved".equals(state)) {
            return "consentOrderApproved";
        }
        if ("orderMade".equals(state)) {
            return "consentOrderNotApproved";
        }
        if ("ScannedRecordReceived".equals(state)) {
            return "ScannedRecordReceivedFormA";
        }
        return state;
    }
}
