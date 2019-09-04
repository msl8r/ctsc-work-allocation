package uk.gov.hmcts.reform.workallocation.queue;

import org.junit.Test;
import org.mockito.Mockito;

import java.time.LocalDateTime;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;

public class DelayedExecutorTest {

    @Test
    public void testShutdown() {
        ScheduledExecutorService executorService = Mockito.mock(ScheduledExecutorService.class);
        DelayedExecutor executor = new DelayedExecutor(executorService);
        executor.shutdown();
        Mockito.verify(executorService, Mockito.times(1)).shutdown();
    }

    @Test
    public void testSchedule() {
        ScheduledExecutorService executorService = Mockito.mock(ScheduledExecutorService.class);
        DelayedExecutor executor = new DelayedExecutor(executorService);
        Supplier<CompletableFuture<Void>> job = () -> CompletableFuture.completedFuture(null);
        executor.schedule(LocalDateTime::now, 30L, job);
        Mockito.verify(executorService, Mockito.times(1))
            .scheduleWithFixedDelay(any(Runnable.class), eq(30L), eq(30L), eq(TimeUnit.SECONDS));
    }
}
