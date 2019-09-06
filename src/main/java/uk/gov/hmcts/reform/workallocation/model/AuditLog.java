package uk.gov.hmcts.reform.workallocation.model;

import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.TimeZone;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Data
@NoArgsConstructor
@Entity
@JsonNaming(PropertyNamingStrategy.SnakeCaseStrategy.class)
@Table(name = "logging_event")
public class AuditLog {

    @Id
    @Column(name = "event_id")
    private long id;

    @Column(name = "timestmp")
    private long time;

    @Column(name = "level_string")
    private String level;

    @Column(name = "formatted_message")
    private String message;

    public LocalDateTime getTime() {
        return LocalDateTime.ofInstant(Instant.ofEpochMilli(time), TimeZone.getDefault().toZoneId());
    }

    @Override
    public String toString() {
        return getTime().toString() + " - " + level + " - " + message;
    }
}
