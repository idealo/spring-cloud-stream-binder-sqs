package de.idealo.spring.stream.binder.sqs.health;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.actuate.health.AbstractHealthIndicator;
import org.springframework.boot.actuate.health.Health;
import org.springframework.integration.aws.inbound.SqsMessageDrivenChannelAdapter;
import org.springframework.util.Assert;

import com.amazonaws.SdkClientException;
import com.amazonaws.services.sqs.model.QueueDoesNotExistException;

import de.idealo.spring.stream.binder.sqs.SqsMessageHandlerBinder;

/**
 * Code from
 * https://github.com/spring-cloud/spring-cloud-aws/pull/342
 */
public class SqsBinderHealthIndicator extends AbstractHealthIndicator {

    private static final Logger LOGGER = LoggerFactory.getLogger(SqsBinderHealthIndicator.class);

    private final SqsMessageHandlerBinder sqsMessageHandlerBinder;

    public SqsBinderHealthIndicator(SqsMessageHandlerBinder sqsMessageHandlerBinder) {
        Assert.notNull(sqsMessageHandlerBinder, "SqsMessageHandlerBinder must not be null");
        this.sqsMessageHandlerBinder = sqsMessageHandlerBinder;
    }

    @Override
    protected void doHealthCheck(Health.Builder builder) {
        boolean allListenersRunning = true;

        // TODO set status to unknown if no adapters are available (isEmpty)
        for (SqsMessageDrivenChannelAdapter adapter : this.sqsMessageHandlerBinder.getAdapters()) {
            for (String queueName : adapter.getQueues()) {
                if (!adapter.isRunning(queueName)) {
                    builder.down().withDetail(queueName, "listener is not running");
                    allListenersRunning = false;
                }

                if (!isReachable(queueName)) {
                    builder.down().withDetail(queueName, "queue is not reachable");
                    allListenersRunning = false;
                }
            }
        }

        if (allListenersRunning) {
            builder.up();
        }
    }

    private boolean isReachable(String queueName) {
        try {
            this.sqsMessageHandlerBinder.getAmazonSQS().getQueueUrl(queueName);
            return true;
        } catch (QueueDoesNotExistException e) {
            LOGGER.warn("Queue '{}' does not exist", queueName);
            return false;
        } catch (SdkClientException e) {
            LOGGER.error("Queue '{}' is not reachable", queueName, e);
            return false;
        }
    }
}
