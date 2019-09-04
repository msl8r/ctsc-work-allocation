package uk.gov.hmcts.reform.workallocation.queue;

import com.microsoft.azure.servicebus.ExceptionPhase;
import com.microsoft.azure.servicebus.IMessage;
import com.microsoft.azure.servicebus.IMessageHandler;
import com.microsoft.azure.servicebus.IQueueClient;
import com.microsoft.azure.servicebus.MessageHandlerOptions;
import com.microsoft.azure.servicebus.primitives.ServiceBusException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

import static java.time.LocalDateTime.now;

@Service
@Slf4j
public class DeadQueueConsumer extends BaseQueueConsumer {

    @Autowired
    private final CtscQueueSupplier queueClientSupplier;

    DeadQueueConsumer(CtscQueueSupplier queueClientSupplier) {
        this.queueClientSupplier = queueClientSupplier;
    }

    @Override
    public Supplier<CompletableFuture<Void>> registerReceiver(DelayedExecutor executorService)
        throws ServiceBusException, InterruptedException {

        IQueueClient deadLetterClient = queueClientSupplier.getDeadQueue();
        IQueueClient sendClient = queueClientSupplier.getQueue();

        deadLetterClient
            .registerMessageHandler(
                new IMessageHandler() {
                    public CompletableFuture<Void> onMessageAsync(IMessage message) {
                        setLastMessageTime(now());
                        try {
                            sendClient.send(message);
                            return deadLetterClient.completeAsync(message.getLockToken());
                        } catch (Exception e) {
                            log.error("failed to retrieve message: ", e);
                            CompletableFuture<Void> completableFuture = new CompletableFuture<>();
                            completableFuture.cancel(true);
                            return completableFuture;
                        }
                    }

                    public void notifyException(Throwable throwable, ExceptionPhase exceptionPhase) {
                        log.error(exceptionPhase.name(), throwable.getMessage());
                    }
                },
                // 1 concurrent call, messages are auto-completed, auto-renew duration
                new MessageHandlerOptions(1, false, Duration.ofMinutes(1)),
                executorService.getExecutorService()
        );

        return () -> deadLetterClient.closeAsync().thenAccept(aVoid -> sendClient.closeAsync());
    }

}
