package de.idealo.spring.stream.binder.sqs.health;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.actuate.health.AbstractHealthIndicator;
import org.springframework.boot.actuate.health.Health;
import org.springframework.util.Assert;

import com.amazonaws.SdkClientException;
import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.model.QueueDoesNotExistException;
import io.awspring.cloud.messaging.listener.SimpleMessageListenerContainer;

/**
 * Code from
 * https://github.com/spring-cloud/spring-cloud-aws/pull/342
 */
public class SqsHealthIndicator extends AbstractHealthIndicator {

    private static final Logger LOGGER = LoggerFactory.getLogger(SqsHealthIndicator.class);

    private final SimpleMessageListenerContainer simpleMessageListenerContainer;
    private final AmazonSQS amazonSQS;
    private final SqsHealthProperties sqsHealthProperties;

    public SqsHealthIndicator(SimpleMessageListenerContainer simpleMessageListenerContainer, AmazonSQS amazonSQS, SqsHealthProperties sqsHealthProperties) {
        Assert.notNull(simpleMessageListenerContainer, "SimpleMessageListenerContainer must not be null");
        Assert.notNull(amazonSQS, "AmazonSQS must not be null");
        this.simpleMessageListenerContainer = simpleMessageListenerContainer;
        this.amazonSQS = amazonSQS;
        this.sqsHealthProperties = sqsHealthProperties;
    }

    @Override
    protected void doHealthCheck(Health.Builder builder) {

        boolean allListenersRunning = true;
        for (String queueName : this.sqsHealthProperties.getQueueNames()) {
            if (!this.simpleMessageListenerContainer.isRunning(queueName)) {
                builder.down().withDetail(queueName, "listener is not running");
                allListenersRunning = false;
            }

            if (!isReachable(queueName)) {
                builder.down().withDetail(queueName, "queue is not reachable");
                allListenersRunning = false;
            }
        }
        if (allListenersRunning) {
            builder.up();
        }
    }

    private boolean isReachable(String queueName) {
        try {
            this.amazonSQS.getQueueUrl(queueName);
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
