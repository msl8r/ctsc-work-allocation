package uk.gov.hmcts.reform.workallocation.queue;

import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import java.time.LocalDateTime;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;

public class BaseQueueConsumerTest {

    @Test
    @SuppressWarnings("unchecked")
    public void testrunConsumer() {
        BaseQueueConsumer consumer = new BaseQueueConsumer() {
            @Override
            public Supplier<CompletableFuture<Void>> registerReceiver(DelayedExecutor executorService) {
                return () -> CompletableFuture.completedFuture(null);
            }
        };
        DelayedExecutor executor = Mockito.mock(DelayedExecutor.class);
        doAnswer(invocation -> {
            Supplier<LocalDateTime> getLastTime = invocation.getArgument(0);
            long timeout = invocation.getArgument(1);
            Supplier<CompletableFuture<Void>> jobToRun = invocation.getArgument(2);
            Assert.assertEquals(consumer.getLastMessageTime(), getLastTime.get());
            return CompletableFuture.completedFuture(null);
        }).when(executor).schedule(any(Supplier.class), any(Long.class), any(Supplier.class), any(LocalDateTime.class));
        consumer.runConsumer(executor, LocalDateTime.now());


    }
}
