package uk.gov.hmcts.reform.workallocation.queue;

import com.microsoft.azure.servicebus.IQueueClient;
import com.microsoft.azure.servicebus.QueueClient;
import com.microsoft.azure.servicebus.ReceiveMode;
import com.microsoft.azure.servicebus.primitives.ConnectionStringBuilder;
import com.microsoft.azure.servicebus.primitives.ServiceBusException;
import org.springframework.beans.factory.annotation.Value;
import uk.gov.hmcts.reform.workallocation.exception.ConnectionException;

import java.util.function.Supplier;

public class QueueClientSupplier implements Supplier<IQueueClient> {

    private final String connectionString;
    private final String entityPath;

    public QueueClientSupplier(@Value("${servicebus.queue.connectionString}") String connectionString,
                               @Value("${servicebus.queue.entityPath}") String entityPath) {
        this.connectionString = connectionString;
        this.entityPath = entityPath;
    }

    @Override
    public IQueueClient get() {
        try {
            return new QueueClient(
                new ConnectionStringBuilder(connectionString, entityPath),
                ReceiveMode.PEEKLOCK
            );
        } catch (InterruptedException | ServiceBusException exception) {
            throw new ConnectionException("Unable to connect to Azure service bus", exception);
        }
    }
}
