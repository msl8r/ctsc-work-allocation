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
import uk.gov.hmcts.reform.workallocation.services.EmailSendingService;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Supplier;

@Slf4j
public class QueueConsumer<T> {

    private final ExecutorService executorService = Executors.newSingleThreadExecutor();

    @Autowired
    @Setter
    private ObjectMapper objectMapper;

    @Autowired
    @Setter
    private Supplier<IQueueClient> queueClientSupplier;

    @Autowired
    @Setter
    private EmailSendingService<T> emailSendingService;

    @Value("${ccd.deeplinkBaseUrl}")
    @Setter
    private String deeplinkBaseUrl;

    private final Class<T> clazz;

    public QueueConsumer(Class<T> clazz) {
        this.clazz = clazz;
    }

    public void registerReceiver() throws ServiceBusException, InterruptedException {
        queueClientSupplier.get()
            .registerMessageHandler(
                new IMessageHandler() {
                    public CompletableFuture<Void> onMessageAsync(IMessage message) {
                        if (message.getLabel() != null
                            && message.getContentType() != null
                            && message.getLabel().contentEquals(clazz.getSimpleName())
                            && message.getContentType().contentEquals("application/json;charset=UTF-8")) {

                            T messageObject = null;
                            try {
                                byte[] body = message.getMessageBody().getBinaryData().get(0);
                                messageObject = objectMapper.readValue(body, clazz);
                                log.info("Received message: " + messageObject);
                                emailSendingService.sendEmail(messageObject, deeplinkBaseUrl);
                            } catch (Exception e) {
                                log.error("failed to retrieve message: ", e);
                                CompletableFuture<Void> completableFuture = new CompletableFuture<>();
                                completableFuture.cancel(true);
                                return completableFuture;
                            }
                        }
                        return CompletableFuture.completedFuture(null);
                    }

                    public void notifyException(Throwable throwable, ExceptionPhase exceptionPhase) {
                        log.error(exceptionPhase.name(), throwable.getMessage());
                    }
                },
                new MessageHandlerOptions(1, true, Duration.ofMinutes(2)),
                executorService);
    }
}
