package de.idealo.spring.stream.binder.sqs.health;

import static java.util.Collections.singletonList;

import java.net.URI;
import java.net.URISyntaxException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.actuate.health.AbstractHealthIndicator;
import org.springframework.boot.actuate.health.Health;
import org.springframework.util.Assert;

import com.amazonaws.SdkClientException;
import com.amazonaws.services.sqs.model.QueueDoesNotExistException;

import de.idealo.spring.stream.binder.sqs.SqsMessageHandlerBinder;
import de.idealo.spring.stream.binder.sqs.inbound.SqsInboundChannelAdapter;

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

        if (sqsMessageHandlerBinder.getAdapters().isEmpty()) {
            builder.unknown();
            allListenersRunning = false;
        }

        for (SqsInboundChannelAdapter adapter : this.sqsMessageHandlerBinder.getAdapters()) {
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
            if (isValidQueueUrl(queueName)) {
                this.sqsMessageHandlerBinder.getAmazonSQS().getQueueAttributes(queueName, singletonList("CreatedTimestamp"));
            } else {
                this.sqsMessageHandlerBinder.getAmazonSQS().getQueueUrl(queueName);
            }
            return true;
        } catch (QueueDoesNotExistException e) {
            LOGGER.warn("Queue '{}' does not exist", queueName);
            return false;
        } catch (SdkClientException e) {
            LOGGER.error("Queue '{}' is not reachable", queueName, e);
            return false;
        }
    }

    private static boolean isValidQueueUrl(String name) {
        try {
            URI candidate = new URI(name);
            return "http".equals(candidate.getScheme()) || "https".equals(candidate.getScheme());
        } catch (URISyntaxException var2) {
            return false;
        }
    }
}
