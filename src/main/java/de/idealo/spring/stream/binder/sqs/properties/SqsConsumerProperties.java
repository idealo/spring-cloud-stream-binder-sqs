package de.idealo.spring.stream.binder.sqs.properties;


import java.time.Duration;

public class SqsConsumerProperties {

    /**
     * Maximum number of messages to retrieve with one poll to SQS.
     * Must be a number between 1 and 10.
     *
     * {@link io.awspring.cloud.sqs.listener.SqsContainerOptionsBuilder#maxMessagesPerPoll(int)}
     */
    private Integer maxNumberOfMessages;

    /**
     * The duration in seconds that polled messages are hidden from subsequent poll requests
     * after having been retrieved.
     *
     * {@link io.awspring.cloud.sqs.listener.SqsContainerOptionsBuilder#messageVisibility(Duration)}
     */
    private Integer visibilityTimeout;

    /**
     * The duration in seconds that the system will wait for new messages to arrive when polling.
     * Uses the Amazon SQS long polling feature. The value should be between 1 and 20.
     *
     * {@link io.awspring.cloud.sqs.listener.SqsContainerOptionsBuilder#pollTimeout(Duration)}
     */
    private Integer waitTimeout;

    /**
     * Whether the incoming message has the SNS format and should be deserialized automatically.
     * Defaults to true.
     */
    private boolean snsFanout = true;

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

    public boolean isSnsFanout() {
        return snsFanout;
    }

    public void setSnsFanout(boolean snsFanout) {
        this.snsFanout = snsFanout;
    }
}
