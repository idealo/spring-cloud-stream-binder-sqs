package de.idealo.spring.stream.binder.sqs.properties;

import org.springframework.cloud.aws.messaging.listener.SqsMessageDeletionPolicy;

public class SqsConsumerProperties {

    private Integer maxNumberOfMessages;

    private Integer visibilityTimeout;

    private Integer waitTimeout;

    private Integer queueStopTimeout;

    private SqsMessageDeletionPolicy messageDeletionPolicy;

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

    public Integer getQueueStopTimeout() {
        return queueStopTimeout;
    }

    public void setQueueStopTimeout(Integer queueStopTimeout) {
        this.queueStopTimeout = queueStopTimeout;
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
