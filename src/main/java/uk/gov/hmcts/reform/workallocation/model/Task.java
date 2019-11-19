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

    public static Task fromCcdDCase(Map<String, Object> caseData, String caseTypeId) throws CaseTransformException {
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

}
