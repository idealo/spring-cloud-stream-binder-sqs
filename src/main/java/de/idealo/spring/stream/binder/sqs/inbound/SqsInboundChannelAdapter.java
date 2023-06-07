package de.idealo.spring.stream.binder.sqs.inbound;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.springframework.context.annotation.Scope;
import org.springframework.integration.aws.support.AwsHeaders;
import org.springframework.integration.endpoint.MessageProducerSupport;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import io.awspring.cloud.sqs.config.SqsMessageListenerContainerFactory;
import io.awspring.cloud.sqs.listener.MessageListener;
import io.awspring.cloud.sqs.listener.SqsContainerOptions;
import io.awspring.cloud.sqs.listener.SqsMessageListenerContainer;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;

//@Component
//@Scope("prototype")
public class SqsInboundChannelAdapter extends MessageProducerSupport {

    private final SqsMessageListenerContainerFactory.Builder<Object> sqsMessageListenerContainerFactory =
            SqsMessageListenerContainerFactory.builder();

    private final String[] queues;

    private SqsContainerOptions sqsContainerOptions;

    private final List<SqsMessageListenerContainer<?>> listenerContainers = new ArrayList<>();

    private int concurrency = 1;

    public SqsInboundChannelAdapter(SqsAsyncClient amazonSqs, String... queues) {
        Assert.noNullElements(queues, "'queues' must not be empty");
        this.sqsMessageListenerContainerFactory.sqsAsyncClient(amazonSqs);
        this.queues = Arrays.copyOf(queues, queues.length);
    }

    public void setSqsContainerOptions(SqsContainerOptions sqsContainerOptions) {
        this.sqsContainerOptions = sqsContainerOptions;
    }

    public void setConcurrency(int concurrency) {
        this.concurrency = concurrency;
    }

    @Override
    protected void onInit() {
        super.onInit();
        if (this.sqsContainerOptions != null) {
            this.sqsMessageListenerContainerFactory.configure(sqsContainerOptionsBuilder ->
                    sqsContainerOptionsBuilder.fromBuilder(this.sqsContainerOptions.toBuilder()));
        }
        this.sqsMessageListenerContainerFactory.messageListener(new SqsInboundChannelAdapter.IntegrationMessageListener());

        for (int i = 0; i < concurrency; i++) {
            final SqsMessageListenerContainer<Object> container = this.sqsMessageListenerContainerFactory.build().createContainer(this.queues);
            this.listenerContainers.add(container);
        }
    }

    @Override
    protected void doStart() {
        super.doStart();
        this.listenerContainers.forEach(SqsMessageListenerContainer::start);
    }

    @Override
    protected void doStop() {
        super.doStop();
        this.listenerContainers.forEach(SqsMessageListenerContainer::stop);
    }

    public boolean isRunning(String logicalQueueName) {
        return this.listenerContainers.stream()
                .filter(container -> container.getQueueNames().contains(logicalQueueName))
                .anyMatch(SqsMessageListenerContainer::isRunning);
    }

    public String[] getQueues() {
        return Arrays.copyOf(this.queues, this.queues.length);
    }


    private class IntegrationMessageListener implements MessageListener<Object> {
        IntegrationMessageListener() {
        }

        @Override
        public void onMessage(Message<Object> message) {
            MessageHeaders headers = message.getHeaders();
            Message<?> messageToSend = getMessageBuilderFactory().fromMessage(message)
                    .removeHeaders("LogicalResourceId", "MessageId", "ReceiptHandle", "Acknowledgment")
                    .setHeader(AwsHeaders.MESSAGE_ID, headers.get("MessageId"))
                    .setHeader("aws_receiptHandle", headers.get("ReceiptHandle"))
                    .setHeader("aws_receivedQueue", headers.get("LogicalResourceId"))
                    .setHeader("aws_acknowledgment", headers.get("Acknowledgment")).build();
            sendMessage(messageToSend);
        }

        @Override
        public void onMessage(Collection<Message<Object>> messages) {
            onMessage(new GenericMessage<>(messages));
        }
    }
}
