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
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import uk.gov.hmcts.reform.workallocation.model.Task;
import uk.gov.hmcts.reform.workallocation.services.EmailSendingService;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class QueueConsumerTest {

    private ObjectMapper mapper;

    private EmailSendingService<Task> emailSendingService;

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
        consumer.setQueueClientSupplier(() -> {
            try {
                return getQueueClient();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
        consumer.setObjectMapper(mapper);
        consumer.setEmailSendingService(emailSendingService);
        consumer.registerReceiver();

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
            when(message.getLabel()).thenReturn(Task.class.getName());
            when(message.getMessageBody()).thenReturn(createMessageBody());
            IMessageHandler handler = invocation.getArgument(0);
            CompletableFuture future = handler.onMessageAsync(message);
            Assert.assertFalse(future.isCancelled());
            verify(emailSendingService, times(1)).sendEmail(any(), any());

            // Test when exception
            message = mock(IMessage.class);
            when(message.getContentType()).thenReturn("application/json;charset=UTF-8");
            when(message.getLabel()).thenReturn(Task.class.getName());
            future = handler.onMessageAsync(message);
            Assert.assertTrue(future.isCancelled());
            verify(emailSendingService, times(1)).sendEmail(any(), any());

            return null;
        }).when(client).registerMessageHandler(any(IMessageHandler.class), any(MessageHandlerOptions.class),
            any(ExecutorService.class));

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
