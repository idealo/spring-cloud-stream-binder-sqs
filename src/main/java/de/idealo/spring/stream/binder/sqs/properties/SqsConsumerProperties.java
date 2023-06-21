package de.idealo.spring.stream.binder.sqs.properties;


import java.time.Duration;

public class SqsConsumerProperties {

    /**
     * Set the number of messages that should be returned per poll.
     * If a value greater than 10 is provided, the result of multiple polls will be combined.
     * Default is 10.
     *
     * {@link io.awspring.cloud.sqs.listener.SqsContainerOptionsBuilder#maxMessagesPerPoll(int)}
     */
    private Integer maxMessagesPerPoll = 10;


    /**
     * The duration in seconds that polled messages are hidden from subsequent poll requests
     * after having been retrieved. Default is 30 seconds.
     *
     * {@link io.awspring.cloud.sqs.listener.SqsContainerOptionsBuilder#messageVisibility(Duration)}
     */
    private Integer visibilityTimeout = 30;

    /**
     * The duration in seconds that the system will wait for new messages to arrive when polling.
     * The value should be between 1 and 20. Default is 10.
     *
     * {@link io.awspring.cloud.sqs.listener.SqsContainerOptionsBuilder#pollTimeout(Duration)}
     */
    private Integer pollTimeout = 10;

    /**
     * The number of milliseconds that the listener is given to gracefully finish its work on shutdown before
     * interrupting the current thread. Default value is 10 seconds.
     *
     * {@link io.awspring.cloud.sqs.listener.SqsContainerOptionsBuilder#listenerShutdownTimeout(Duration)}
     */
    private Long listenerShutdownTimeout = 10L;

    /**
     * Whether the incoming message has the SNS format and should be deserialized automatically.
     * Defaults to true.
     */
    private boolean snsFanout = true;

    /**
     * @deprecated
     * This property was renamed. Use {@link SqsConsumerProperties#getMaxMessagesPerPoll()} instead.
     */
    @Deprecated(since = "3.0.0", forRemoval = true)
    public Integer getMaxNumberOfMessages() {
        return maxMessagesPerPoll;
    }

    /**
     * @deprecated
     * This property was renamed. Use {@link SqsConsumerProperties#getMaxMessagesPerPoll()} instead.
     */
    @Deprecated(since = "3.0.0", forRemoval = true)
    public void setMaxNumberOfMessages(Integer maxNumberOfMessages) {
        this.maxMessagesPerPoll = maxNumberOfMessages;
    }

    /**
     * @deprecated
     * This property was renamed. Use {@link SqsConsumerProperties#getPollTimeout()} instead.
     */
    @Deprecated(since = "3.0.0", forRemoval = true)
    public Integer getWaitTimeout() {
        return pollTimeout;
    }

    /**
     * @deprecated
     * This property was renamed. Use {@link SqsConsumerProperties#setPollTimeout(Integer)} instead.
     */
    @Deprecated(since = "3.0.0", forRemoval = true)
    public void setWaitTimeout(Integer waitTimeout) {
        this.pollTimeout = waitTimeout;
    }

    /**
     * @deprecated
     * This property was renamed. Use {@link SqsConsumerProperties#getListenerShutdownTimeout()} instead.
     */
    @Deprecated(since = "3.0.0", forRemoval = true)
    public Long getQueueStopTimeout() {
        return listenerShutdownTimeout;
    }

    /**
     * @deprecated
     * This property was renamed. Use {@link SqsConsumerProperties#setListenerShutdownTimeout(Long)} instead.
     */
    @Deprecated(since = "3.0.0", forRemoval = true)
    public void setQueueStopTimeout(final Long queueStopTimeout) {
        this.listenerShutdownTimeout = queueStopTimeout;
    }

    public Integer getVisibilityTimeout() {
        return visibilityTimeout;
    }

    public void setVisibilityTimeout(Integer visibilityTimeout) {
        this.visibilityTimeout = visibilityTimeout;
    }

    public boolean isSnsFanout() {
        return snsFanout;
    }

    public void setSnsFanout(boolean snsFanout) {
        this.snsFanout = snsFanout;
    }


    public Integer getMaxMessagesPerPoll() {
        return maxMessagesPerPoll;
    }

    public void setMaxMessagesPerPoll(final Integer maxMessagesPerPoll) {
        this.maxMessagesPerPoll = maxMessagesPerPoll;
    }

    public Integer getPollTimeout() {
        return pollTimeout;
    }

    public void setPollTimeout(final Integer pollTimeout) {
        this.pollTimeout = pollTimeout;
    }

    public Long getListenerShutdownTimeout() {
        return listenerShutdownTimeout;
    }

    public void setListenerShutdownTimeout(final Long listenerShutdownTimeout) {
        this.listenerShutdownTimeout = listenerShutdownTimeout;
    }
}
