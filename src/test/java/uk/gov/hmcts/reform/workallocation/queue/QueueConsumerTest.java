package uk.gov.hmcts.reform.workallocation.queue;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.microsoft.azure.servicebus.IMessage;
import com.microsoft.azure.servicebus.IMessageHandler;
import com.microsoft.azure.servicebus.IQueueClient;
import com.microsoft.azure.servicebus.MessageBody;
import com.microsoft.azure.servicebus.MessageHandlerOptions;
import com.microsoft.azure.servicebus.primitives.ServiceBusException;
import net.serenitybdd.junit.runners.SerenityRunner;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import uk.gov.hmcts.reform.workallocation.model.Task;
import uk.gov.hmcts.reform.workallocation.services.EmailSendingService;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(SerenityRunner.class)
public class QueueConsumerTest {

    private ObjectMapper mapper;

    private EmailSendingService emailSendingService;

    @Before
    public void setUp() {
        mapper = new ObjectMapper();
        mapper.registerModule(new Jdk8Module()).registerModule(new JavaTimeModule());
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testRegisterReceiver() throws ServiceBusException, InterruptedException {
        this.emailSendingService = mock(EmailSendingService.class);
        QueueConsumer<Task> consumer = new QueueConsumer<>(Task.class);
        consumer.setQueueClientSupplier(new CtscQueueSupplier() {
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
        });
        consumer.setObjectMapper(mapper);
        consumer.setEmailSendingService(emailSendingService);
        DelayedExecutor executor = new DelayedExecutor(Executors.newScheduledThreadPool(1));
        consumer.registerReceiver(executor);

    }

    private IQueueClient getQueueClient() throws ServiceBusException, InterruptedException {

        IQueueClient client = mock(IQueueClient.class);
        doAnswer(invocation -> {
            MessageHandlerOptions handlerOptions = invocation.getArgument(1);

            Assert.assertEquals(Duration.ofMinutes(2), handlerOptions.getMaxAutoRenewDuration());
            Assert.assertTrue(handlerOptions.isAutoComplete());
            Assert.assertEquals(1, handlerOptions.getMaxConcurrentCalls());

            // Test happy path
            IMessage message = mock(IMessage.class);
            when(message.getContentType()).thenReturn("application/json;charset=UTF-8");
            when(message.getLabel()).thenReturn(Task.class.getSimpleName());
            when(message.getMessageBody()).thenReturn(createMessageBody());
            when(message.getLockToken()).thenReturn(UUID.randomUUID());
            IMessageHandler handler = invocation.getArgument(0);
            CompletableFuture future = handler.onMessageAsync(message);
            Assert.assertFalse(future.isCancelled());
            verify(emailSendingService, times(1)).sendEmail(any(), any());

            // Test when exception
            message = mock(IMessage.class);
            when(message.getContentType()).thenReturn("application/json;charset=UTF-8");
            when(message.getLabel()).thenReturn(Task.class.getSimpleName());
            future = handler.onMessageAsync(message);
            Assert.assertTrue(future.isCancelled());
            verify(emailSendingService, times(1)).sendEmail(any(), any());

            // Test when label is wrong
            message = mock(IMessage.class);
            when(message.getContentType()).thenReturn("application/json;charset=UTF-8");
            when(message.getLabel()).thenReturn("SomeOtherClass");
            future = handler.onMessageAsync(message);
            Assert.assertTrue(future.isDone());
            verify(emailSendingService, times(1)).sendEmail(any(), any());

            return null;
        }).when(client).registerMessageHandler(any(IMessageHandler.class), any(MessageHandlerOptions.class),
            any(ExecutorService.class));

        when(client.completeAsync(any(UUID.class))).thenReturn(CompletableFuture.completedFuture(null));
        return client;
    }

    private MessageBody createMessageBody() throws JsonProcessingException {
        Task task = Task.builder()
            .lastModifiedDate(LocalDateTime.now())
            .state("Submitted")
            .jurisdiction("DIVORCE")
            .id("123")
            .caseTypeId("DIVORCE")
            .build();
        return MessageBody.fromBinaryData(Collections.singletonList(mapper.writeValueAsBytes(task)));
    }
}
