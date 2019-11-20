package uk.gov.hmcts.reform.workallocation.data.migration;

import org.springframework.boot.autoconfigure.flyway.FlywayMigrationStrategy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
public class DisableFlywayMigrationStrategy {

    @Bean
    @Primary
    public FlywayMigrationStrategy emptyMigrationStrategy() {
        return null;
    }
}
