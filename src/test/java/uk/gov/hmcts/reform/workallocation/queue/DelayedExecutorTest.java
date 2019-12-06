package uk.gov.hmcts.reform.workallocation.queue;

import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import java.time.LocalDateTime;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
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
        executor.schedule(LocalDateTime::now, 30L, job, LocalDateTime.now());
        Mockito.verify(executorService, Mockito.times(1))
            .scheduleWithFixedDelay(any(Runnable.class), eq(30L), eq(30L), eq(TimeUnit.SECONDS));
    }

    @Test
    public void testScheduleRunAfterDelay() throws InterruptedException {
        ScheduledExecutorService executorService = Executors.newScheduledThreadPool(1);
        DelayedExecutor executor = new DelayedExecutor(executorService);
        Supplier<CompletableFuture<Void>> job = () -> CompletableFuture.completedFuture(null);
        Supplier<LocalDateTime> lastMesageTimeFunc = () -> LocalDateTime.of(2019, 1, 1, 0, 0);
        job.get().complete(null);
        CompletableFuture<Void> ret = executor.schedule(lastMesageTimeFunc, 1L, job, LocalDateTime.now());
        Thread.sleep(1500);
        Assert.assertTrue(ret.isDone());
    }
}
