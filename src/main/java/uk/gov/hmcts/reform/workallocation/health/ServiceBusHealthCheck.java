package uk.gov.hmcts.reform.workallocation.health;

import com.microsoft.azure.servicebus.IQueueClient;
import com.microsoft.azure.servicebus.primitives.ServiceBusException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.workallocation.queue.CtscQueueSupplier;

@Component
@Slf4j
public class ServiceBusHealthCheck implements HealthIndicator {

    private final CtscQueueSupplier queueClientSupplier;

    public ServiceBusHealthCheck(CtscQueueSupplier queueClientSupplier) {
        this.queueClientSupplier = queueClientSupplier;
    }

    @Override
    public Health health() {
        IQueueClient client = null;
        try {
            client = queueClientSupplier.getQueue();
            return Health.up().build();
        } catch (Exception e) {
            return Health.down().withDetail("Error: ", e.getMessage()).build();
        } finally {
            try {
                if (client != null) {
                    client.close();
                }
            } catch (ServiceBusException e) {
                log.error("Failed to close connection", e);
            }
        }
    }
}
