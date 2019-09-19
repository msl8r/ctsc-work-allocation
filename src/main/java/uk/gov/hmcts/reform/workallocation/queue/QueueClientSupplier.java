package uk.gov.hmcts.reform.workallocation.queue;

import com.microsoft.azure.servicebus.IQueueClient;
import com.microsoft.azure.servicebus.QueueClient;
import com.microsoft.azure.servicebus.ReceiveMode;
import com.microsoft.azure.servicebus.primitives.ConnectionStringBuilder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import uk.gov.hmcts.reform.workallocation.exception.ConnectionException;

@Slf4j
public class QueueClientSupplier implements CtscQueueSupplier {

    private final String connectionString;
    private final String entityPath;

    public QueueClientSupplier(@Value("${servicebus.queue.connectionString}") String connectionString,
                               @Value("${servicebus.queue.entityPath}") String entityPath) {
        this.connectionString = connectionString;
        this.entityPath = entityPath;
    }

    @Override
    public IQueueClient getQueue() {
        return getServiceBusQueue(entityPath);
    }

    @Override
    public IQueueClient getDeadQueue() {
        return getServiceBusQueue(entityPath + "/$deadletterqueue");
    }

    private IQueueClient getServiceBusQueue(String entityPath) {
        try {
            return new QueueClient(
                new ConnectionStringBuilder(connectionString, entityPath),
                ReceiveMode.PEEKLOCK
            );
        } catch (Exception exception) {
            throw new ConnectionException("Unable to connect to Azure service bus", exception);
        }
    }
}
