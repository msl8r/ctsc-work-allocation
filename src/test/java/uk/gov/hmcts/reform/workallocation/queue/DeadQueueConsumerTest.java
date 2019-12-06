package uk.gov.hmcts.reform.workallocation.queue;

import com.microsoft.azure.servicebus.IMessage;
import com.microsoft.azure.servicebus.IMessageHandler;
import com.microsoft.azure.servicebus.IQueueClient;
import com.microsoft.azure.servicebus.Message;
import com.microsoft.azure.servicebus.MessageHandlerOptions;
import com.microsoft.azure.servicebus.primitives.ServiceBusException;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class DeadQueueConsumerTest {

    private CtscQueueSupplier queueClientSupplier;
    private DeadQueueConsumer deadQueueConsumer;
    private IMessage errorMessage = new Message();

    @Before
    public void setUp() {
        queueClientSupplier = new CtscQueueSupplier() {
            @Override
            public IQueueClient getQueue() {
                try {
                    return getQueueClient();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }

            @Override
            public IQueueClient getDeadQueue() {
                try {
                    return getQueueClient();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        };

        deadQueueConsumer = new DeadQueueConsumer(queueClientSupplier);
    }

    @Test
    public void testPickUpDeadLettersHappyPath() throws ServiceBusException, InterruptedException {
        DelayedExecutor executor = new DelayedExecutor(Executors.newScheduledThreadPool(1));
        deadQueueConsumer.runConsumer(executor, LocalDateTime.now());
    }

    private IQueueClient getQueueClient() throws ServiceBusException, InterruptedException {

        IQueueClient client = mock(IQueueClient.class);
        doAnswer(invocation -> {
            MessageHandlerOptions handlerOptions = invocation.getArgument(1);
            Assert.assertEquals(Duration.ofMinutes(1), handlerOptions.getMaxAutoRenewDuration());
            Assert.assertFalse(handlerOptions.isAutoComplete());
            Assert.assertEquals(1, handlerOptions.getMaxConcurrentCalls());

            IMessage message = mock(IMessage.class);
            when(message.getLockToken()).thenReturn(UUID.randomUUID());
            IMessageHandler handler = invocation.getArgument(0);
            CompletableFuture future = handler.onMessageAsync(message);
            Assert.assertFalse(future.isCancelled());
            verify(client, times(1)).completeAsync(any(UUID.class));

            future = handler.onMessageAsync(errorMessage);
            Assert.assertTrue(future.isCancelled());
            verify(client, times(1)).completeAsync(any(UUID.class));

            return null;
        }).when(client).registerMessageHandler(any(IMessageHandler.class), any(MessageHandlerOptions.class),
            any(ExecutorService.class));
        when(client.completeAsync(any(UUID.class))).thenReturn(CompletableFuture.completedFuture(null));

        doThrow(new RuntimeException()).when(client).send(errorMessage);
        return client;
    }
}
