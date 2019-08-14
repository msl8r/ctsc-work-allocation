package uk.gov.hmcts.reform.workallocation.model;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class Task {

    private String id;
    private String state;
    private LocalDateTime lastModifiedDate;

}
