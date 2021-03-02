package uk.gov.hmcts.reform.workallocation.queue;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.azure.servicebus.ExceptionPhase;
import com.microsoft.azure.servicebus.IMessage;
import com.microsoft.azure.servicebus.IMessageHandler;
import com.microsoft.azure.servicebus.IQueueClient;
import com.microsoft.azure.servicebus.MessageHandlerOptions;
import com.microsoft.azure.servicebus.primitives.ServiceBusException;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import uk.gov.hmcts.reform.workallocation.email.IEmailSendingService;
import uk.gov.hmcts.reform.workallocation.model.Task;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

import static java.time.LocalDateTime.now;

@Slf4j
public class QueueConsumer<T> extends BaseQueueConsumer {

    @Autowired
    @Setter
    private ObjectMapper objectMapper;

    @Autowired
    @Setter
    private CtscQueueSupplier queueClientSupplier;

    @Autowired
    @Setter
    private IEmailSendingService emailSendingService;

    @Value("${ccd.deeplinkBaseUrl}")
    @Setter
    private String deeplinkBaseUrl;

    private final Class<T> clazz;

    public QueueConsumer(Class<T> clazz) {
        this.clazz = clazz;
    }


    @Override
    public Supplier<CompletableFuture<Void>> registerReceiver(DelayedExecutor executorService)
        throws ServiceBusException, InterruptedException {
        IQueueClient receiver = queueClientSupplier.getQueue();

        receiver
            .registerMessageHandler(
                new IMessageHandler() {
                    public CompletableFuture<Void> onMessageAsync(IMessage message) {
                        setLastMessageTime(now());
                        if (message.getLabel() != null
                            && message.getContentType() != null
                            && message.getLabel().contentEquals(clazz.getSimpleName())
                            && message.getContentType().contentEquals("application/json;charset=UTF-8")) {

                            T messageObject = null;
                            try {
                                byte[] body = message.getMessageBody().getBinaryData().get(0);
                                messageObject = objectMapper.readValue(body, clazz);
                                log.info("Received message: " + messageObject);
                                // TODO: make email sending generic
                                /**
                                 * DTSBPS-395 Stopping task poller for Probate
                                 * */
                                Task messageObj = (Task) messageObject;
                                if (messageObj.getJurisdiction().equals("DIVORCE")) {
                                    emailSendingService.sendEmail(messageObj, deeplinkBaseUrl);
                                } else {
                                    log.info("Ignore probate message id {}: ",messageObj.getId());
                                }
                            } catch (Exception e) {
                                log.error("failed to parse/send message: ", e);
                                CompletableFuture<Void> failure = new CompletableFuture<>();
                                failure.cancel(true);
                                return failure;
                            }
                        }
                        return CompletableFuture.completedFuture(null);
                    }

                    public void notifyException(Throwable throwable, ExceptionPhase exceptionPhase) {
                        log.error(exceptionPhase.name(), throwable.getMessage());
                    }
                },
                new MessageHandlerOptions(1, true, Duration.ofMinutes(2)),
                executorService.getExecutorService());

        return receiver::closeAsync;
    }
}
