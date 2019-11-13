package uk.gov.hmcts.reform.workallocation.health;

import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

public class BaseHealthCheck implements HealthIndicator {

    private final RestTemplate restTemplate;
    private final String baseUrl;
    private static final String LIVENESS_ENDPOINT = "/health/liveness";

    public BaseHealthCheck(RestTemplateBuilder restTemplateBuilder, String baseUrl) {
        this.baseUrl = baseUrl;
        this.restTemplate = restTemplateBuilder
            .setConnectTimeout(Duration.ofSeconds(2))
            .setReadTimeout(Duration.ofSeconds(2))
            .build();
    }

    @Override
    public Health health() {
        ResponseEntity<String> response = null;
        try {
            response = restTemplate.getForEntity(baseUrl + LIVENESS_ENDPOINT, String.class);
        } catch (Exception e) {
            return Health.down().withDetail("Error: ", e.getMessage()).build();
        }
        if (response != null && response.getStatusCode().equals(HttpStatus.OK) && response.getBody().contains("UP")) {
            return Health.up().build();
        } else {
            return Health.down()
                .withDetail("Error: ",
                    "status_code: " + response.getStatusCode() + ", response: " + response.getBody())
                .build();
        }
    }
}
