package de.idealo.spring.stream.binder.sqs.properties;

import org.springframework.cloud.aws.messaging.listener.SqsMessageDeletionPolicy;

public class SqsConsumerProperties {

    /**
     * Maximum number of messages to retrieve with one poll to SQS.
     * Must be a number between 1 and 10.
     *
     * {@link org.springframework.cloud.aws.messaging.listener.SimpleMessageListenerContainer#setMaxNumberOfMessages(Integer)}
     */
    private Integer maxNumberOfMessages;

    /**
     * The duration in seconds that polled messages are hidden from subsequent poll requests
     * after having been retrieved.
     *
     * {@link org.springframework.cloud.aws.messaging.config.SimpleMessageListenerContainerFactory#setVisibilityTimeout(Integer)}
     */
    private Integer visibilityTimeout;

    /**
     * The duration in seconds that the system will wait for new messages to arrive when polling.
     * Uses the Amazon SQS long polling feature. The value should be between 1 and 20.
     *
     * {@link org.springframework.cloud.aws.messaging.config.SimpleMessageListenerContainerFactory#setWaitTimeOut(Integer)}
     */
    private Integer waitTimeout;

    /**
     * The deletion policy for messages that are retrieved from SQS. Defaults to NO_REDRIVE.
     */
    private SqsMessageDeletionPolicy messageDeletionPolicy;

    /**
     * Whether the incoming message has the SNS format and should be deserialized automatically.
     * Defaults to false.
     */
    private boolean snsFanout;

    public Integer getMaxNumberOfMessages() {
        return maxNumberOfMessages;
    }

    public void setMaxNumberOfMessages(Integer maxNumberOfMessages) {
        this.maxNumberOfMessages = maxNumberOfMessages;
    }

    public Integer getVisibilityTimeout() {
        return visibilityTimeout;
    }

    public void setVisibilityTimeout(Integer visibilityTimeout) {
        this.visibilityTimeout = visibilityTimeout;
    }

    public Integer getWaitTimeout() {
        return waitTimeout;
    }

    public void setWaitTimeout(Integer waitTimeout) {
        this.waitTimeout = waitTimeout;
    }

    public SqsMessageDeletionPolicy getMessageDeletionPolicy() {
        return messageDeletionPolicy;
    }

    public void setMessageDeletionPolicy(SqsMessageDeletionPolicy messageDeletionPolicy) {
        this.messageDeletionPolicy = messageDeletionPolicy;
    }

    public boolean isSnsFanout() {
        return snsFanout;
    }

    public void setSnsFanout(boolean snsFanout) {
        this.snsFanout = snsFanout;
    }
}
