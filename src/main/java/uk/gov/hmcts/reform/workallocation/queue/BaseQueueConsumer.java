package uk.gov.hmcts.reform.workallocation.queue;

import com.microsoft.azure.servicebus.primitives.ServiceBusException;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

import static java.time.LocalDateTime.now;

public abstract class BaseQueueConsumer {

    public static final long CLIENT_TIMEOUT_SECONDS = 30L;

    @Getter
    @Setter
    private LocalDateTime lastMessageTime = now();

    public CompletableFuture<Void> runConsumer(DelayedExecutor executorService, LocalDateTime finishTime) {

        CompletableFuture<Void> future = new CompletableFuture<>();
        try {
            setLastMessageTime(now());
            Supplier<CompletableFuture<Void>> job = registerReceiver(executorService);
            Supplier<LocalDateTime> getLastMessageTimeFunc = this::getLastMessageTime;
            future = executorService.schedule(getLastMessageTimeFunc, CLIENT_TIMEOUT_SECONDS, job, finishTime);
        } catch (Exception e) {
            future.completeExceptionally(e);
        }
        return future;
    }

    public abstract Supplier<CompletableFuture<Void>> registerReceiver(DelayedExecutor executorService)
        throws ServiceBusException, InterruptedException;
}
