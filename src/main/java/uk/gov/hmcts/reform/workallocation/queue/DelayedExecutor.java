package uk.gov.hmcts.reform.workallocation.queue;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import static java.time.LocalDateTime.now;

@Slf4j
@ToString
@EqualsAndHashCode
public class DelayedExecutor {

    @Getter
    private final ScheduledExecutorService executorService;

    public DelayedExecutor(ScheduledExecutorService executorService) {
        this.executorService = executorService;
    }

    public CompletableFuture<Void> schedule(Supplier<LocalDateTime> lastMessageTimeFunc,
                                            long timeoutSeconds,
                                            Supplier<CompletableFuture<Void>> job, LocalDateTime finishTime) {
        CompletableFuture<Void> ret = new CompletableFuture<>();
        Future future = executorService.scheduleWithFixedDelay(() -> {
            LocalDateTime lastMessageTime = lastMessageTimeFunc.get();
            log.info("Schedule is running and checking for time {}", lastMessageTime);
            if (now().isAfter(lastMessageTime.plusSeconds(timeoutSeconds)) || now().isAfter(finishTime)) {
                log.info("closing receiver client");
                job.get().thenAccept(aVoid -> ret.complete(null));
            }
        }, timeoutSeconds, timeoutSeconds, TimeUnit.SECONDS);
        return ret.whenComplete((aVoid, throwable) -> {
            log.info("Cancelling job");
            future.cancel(false);
        });
    }

    public void shutdown() {
        executorService.shutdown();
    }
}
