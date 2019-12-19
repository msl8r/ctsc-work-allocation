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
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString(callSuper = true)
@EqualsAndHashCode
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonNaming(PropertyNamingStrategy.SnakeCaseStrategy.class)
public class Task {

    @NotNull
    @Pattern(regexp = "^\\d{16}$", message = "id must contains only numbers and should be 16 digits")
    private String id;

    @NotNull
    private String state;

    @NotNull
    private String jurisdiction;

    @NotNull
    private String caseTypeId;

    @NotNull
    private LocalDateTime lastModifiedDate;

    public static Task fromCcdCase(Map<String, Object> caseData, String caseTypeId) throws CaseTransformException {
        if (CcdConnectorService.CASE_TYPE_ID_DIVORCE.equals(caseTypeId)) {
            return fromCcdDivorceCase(caseData);
        }
        if (CcdConnectorService.CASE_TYPE_ID_PROBATE.equals(caseTypeId)) {
            return fromCcdProbateCase(caseData);
        }
        throw new CaseTransformException("Unknown case type: " + caseTypeId);
    }

    private static Task fromCcdDivorceCase(Map<String, Object> caseData) throws CaseTransformException {
        try {
            LocalDateTime lastModifiedDate = LocalDateTime.parse(caseData.get("last_modified").toString());
            return Task.builder()
                .id(((Long)caseData.get("id")).toString())
                .state((String) caseData.get("state"))
                .jurisdiction((String) caseData.get("jurisdiction"))
                .caseTypeId(CcdConnectorService.CASE_TYPE_ID_DIVORCE)
                .lastModifiedDate(lastModifiedDate)
                .build();
        } catch (Exception e) {
            throw new CaseTransformException("Failed to transform the case", e);
        }
    }

    private static Task fromCcdProbateCase(Map<String, Object> caseData) throws CaseTransformException {
        try {
            LocalDateTime lastModifiedDate = LocalDateTime.parse(caseData.get("last_modified").toString());
            return Task.builder()
                .id(((Long)caseData.get("id")).toString())
                .state(getProbateState(caseData))
                .jurisdiction((String) caseData.get("jurisdiction"))
                .caseTypeId(CcdConnectorService.CASE_TYPE_ID_PROBATE)
                .lastModifiedDate(lastModifiedDate)
                .build();
        } catch (Exception e) {
            throw new CaseTransformException("Failed to transform the case", e);
        }
    }

    @SuppressWarnings("unchecked")
    private static String getProbateState(Map<String, Object> caseData) {
        String state = (String) caseData.get("state");
        Map<String, Object> caseProperties = (Map<String, Object>) caseData.get("case_data");
        if ("CaseCreated".equals(state)) {
            return "CaseCreated";
        }
        if ("CasePrinted".equals(state) && "No".equals(caseProperties.get("evidenceHandled"))) {
            return "AwaitingDocumentation";
        }
        if ("BOReadyForExamination".equals(state) && "Personal".equals(caseProperties.get("applicationType"))) {
            return "ReadyforExamination-Personal";
        }
        if ("BOReadyForExamination".equals(state) && "Solicitor".equals(caseProperties.get("applicationType"))) {
            return "ReadyforExamination-Solicitor";
        }
        if ("BOCaseStopped".equals(state) && caseProperties.get("evidenceHandled") != null) {
            return "CaseStopped";
        }
        return state;
    }
}
