package uk.gov.hmcts.reform.workallocation.queue;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.azure.servicebus.IQueueClient;
import com.microsoft.azure.servicebus.Message;
import com.microsoft.azure.servicebus.primitives.ServiceBusException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;
import java.util.function.Function;

@Service
@Slf4j
public class QueueProducer<T> {

    private final ObjectMapper objectMapper;

    @Autowired
    private final CtscQueueSupplier queueClientSupplier;

    private int messageTtl;

    public QueueProducer(CtscQueueSupplier queueClientSupplier, ObjectMapper objectMapper,
                         @Value("${servicebus.queue.messageTTLInDays}") int messageTtl) {
        this.queueClientSupplier = queueClientSupplier;
        this.objectMapper = objectMapper;
        this.messageTtl = messageTtl;
    }

    public void placeItemsInQueue(List<T> items, Function<T, String> extractId) {
        if (items.isEmpty()) {
            return;
        }
        IQueueClient sendClient = queueClientSupplier.getQueue();
        try {
            items.forEach(item -> {
                String messageId = extractId.apply(item);
                try {
                    Message message = createQueueMessage(item, messageId);
                    sendClient.send(message);
                    log.info("items placed on the queue successfully");
                } catch (Exception e) {
                    log.error(String.format("Could not send message to ServiceBus. Message ID: %s", messageId), e);
                }
            });
        } finally {
            try {
                if (sendClient != null) {
                    sendClient.close();
                }
            } catch (ServiceBusException exc) {
                log.error("Failed to close the queue client", exc);
            }
        }
    }

    private Message createQueueMessage(T task, String messageId) throws JsonProcessingException {

        Message message = new Message(
            objectMapper.writeValueAsBytes(task)
        );

        message.setContentType(MediaType.APPLICATION_JSON_UTF8_VALUE);
        message.setMessageId(messageId);
        message.setTimeToLive(Duration.ofDays(messageTtl));
        message.setLabel(task.getClass().getSimpleName());

        return message;
    }
}
