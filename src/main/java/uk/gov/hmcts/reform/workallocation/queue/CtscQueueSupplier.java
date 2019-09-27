package uk.gov.hmcts.reform.workallocation.queue;

import com.microsoft.azure.servicebus.IQueueClient;

public interface CtscQueueSupplier {

    IQueueClient getQueue();

    IQueueClient getDeadQueue();
}
