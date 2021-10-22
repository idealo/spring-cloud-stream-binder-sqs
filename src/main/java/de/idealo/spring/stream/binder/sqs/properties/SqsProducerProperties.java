package de.idealo.spring.stream.binder.sqs.properties;

public class SqsProducerProperties {

    /**
     * SpEL expression that will be applied to the message to get the group id.
     * You have access to the {@code payload} and {@code headers} properties.
     * Required for FIFO queues.
     */
    private String messageGroupIdExpression;

    /**
     * SpEL expression that will be applied to the message to get the deduplication id.
     * You have access to the {@code payload} and {@code headers} properties.
     * Required for FIFO queues.
     */
    private String messageDeduplicationIdExpression;

    public String getMessageGroupIdExpression() {
        return messageGroupIdExpression;
    }

    public void setMessageGroupIdExpression(String messageGroupIdExpression) {
        this.messageGroupIdExpression = messageGroupIdExpression;
    }

    public String getMessageDeduplicationIdExpression() {
        return messageDeduplicationIdExpression;
    }

    public void setMessageDeduplicationIdExpression(String messageDeduplicationIdExpression) {
        this.messageDeduplicationIdExpression = messageDeduplicationIdExpression;
    }

}
