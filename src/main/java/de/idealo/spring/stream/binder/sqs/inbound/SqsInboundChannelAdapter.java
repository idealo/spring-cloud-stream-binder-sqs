package de.idealo.spring.stream.binder.sqs.inbound;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.beans.factory.BeanCreationException;
import org.springframework.context.annotation.Scope;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.integration.aws.support.AwsHeaders;
import org.springframework.integration.endpoint.MessageProducerSupport;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.core.DestinationResolver;
import org.springframework.messaging.handler.HandlerMethod;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import com.amazonaws.services.sqs.AmazonSQSAsync;

import io.awspring.cloud.core.env.ResourceIdResolver;
import io.awspring.cloud.messaging.config.SimpleMessageListenerContainerFactory;
import io.awspring.cloud.messaging.listener.QueueMessageHandler;
import io.awspring.cloud.messaging.listener.SimpleMessageListenerContainer;
import io.awspring.cloud.messaging.listener.SqsMessageDeletionPolicy;

@Component
@Scope("prototype")
public class SqsInboundChannelAdapter extends MessageProducerSupport {

    private final SimpleMessageListenerContainerFactory simpleMessageListenerContainerFactory = new SimpleMessageListenerContainerFactory();

    private final String[] queues;

    private final List<SimpleMessageListenerContainer> listenerContainers = new ArrayList<>();

    private int concurrency = 1;

    private Long queueStopTimeout;

    private SqsMessageDeletionPolicy messageDeletionPolicy = SqsMessageDeletionPolicy.NO_REDRIVE;

    public SqsInboundChannelAdapter(AmazonSQSAsync amazonSqs, String... queues) {
        Assert.noNullElements(queues, "'queues' must not be empty");
        this.simpleMessageListenerContainerFactory.setAmazonSqs(amazonSqs);
        this.queues = Arrays.copyOf(queues, queues.length);
    }

    public void setTaskExecutor(AsyncTaskExecutor taskExecutor) {
        this.simpleMessageListenerContainerFactory.setTaskExecutor(taskExecutor);
    }

    public void setMaxNumberOfMessages(Integer maxNumberOfMessages) {
        this.simpleMessageListenerContainerFactory.setMaxNumberOfMessages(maxNumberOfMessages);
    }

    public void setConcurrency(int concurrency) {
        this.concurrency = concurrency;
    }

    public void setVisibilityTimeout(Integer visibilityTimeout) {
        this.simpleMessageListenerContainerFactory.setVisibilityTimeout(visibilityTimeout);
    }

    public void setWaitTimeOut(Integer waitTimeOut) {
        this.simpleMessageListenerContainerFactory.setWaitTimeOut(waitTimeOut);
    }

    public void setResourceIdResolver(ResourceIdResolver resourceIdResolver) {
        this.simpleMessageListenerContainerFactory.setResourceIdResolver(resourceIdResolver);
    }

    @Override
    public void setAutoStartup(boolean autoStartUp) {
        super.setAutoStartup(autoStartUp);
        this.simpleMessageListenerContainerFactory.setAutoStartup(autoStartUp);
    }

    public void setDestinationResolver(DestinationResolver<String> destinationResolver) {
        this.simpleMessageListenerContainerFactory.setDestinationResolver(destinationResolver);
    }

    public void setQueueStopTimeout(long queueStopTimeout) {
        this.queueStopTimeout = queueStopTimeout;
    }

    public void setMessageDeletionPolicy(SqsMessageDeletionPolicy messageDeletionPolicy) {
        Assert.notNull(messageDeletionPolicy, "'messageDeletionPolicy' must not be null.");
        this.messageDeletionPolicy = messageDeletionPolicy;
    }

    @Override
    protected void onInit() {
        super.onInit();

        for (int i = 0; i < concurrency; i++) {
            SimpleMessageListenerContainer container = this.simpleMessageListenerContainerFactory.createSimpleMessageListenerContainer();
            this.listenerContainers.add(container);
            if (this.queueStopTimeout != null) {
                container.setQueueStopTimeout(this.queueStopTimeout);
            }
            container.setMessageHandler(new IntegrationQueueMessageHandler());
            try {
                container.afterPropertiesSet();
            } catch (Exception e) {
                throw new BeanCreationException("Cannot instantiate 'SimpleMessageListenerContainer'", e);
            }
        }
    }

    @Override
    protected void doStart() {
        super.doStart();
        this.listenerContainers.forEach(SimpleMessageListenerContainer::start);
    }

    @Override
    protected void doStop() {
        super.doStop();
        this.listenerContainers.forEach(SimpleMessageListenerContainer::stop);
    }

    public boolean isRunning(String logicalQueueName) {
        return this.listenerContainers.stream()
                .anyMatch(container -> container.isRunning(logicalQueueName));
    }

    public String[] getQueues() {
        return Arrays.copyOf(this.queues, this.queues.length);
    }

    @Override
    public void destroy() {
        this.listenerContainers.forEach(SimpleMessageListenerContainer::destroy);
    }

    private class IntegrationQueueMessageHandler extends QueueMessageHandler {

        @Override
        public Map<MappingInformation, HandlerMethod> getHandlerMethods() {
            Set<String> uniqueQueues = new HashSet<>(Arrays.asList(SqsInboundChannelAdapter.this.queues));
            MappingInformation mappingInformation = new MappingInformation(uniqueQueues,
                    SqsInboundChannelAdapter.this.messageDeletionPolicy);
            return Collections.singletonMap(mappingInformation, null);
        }

        @Override
        protected void handleMessageInternal(Message<?> message, String lookupDestination) {
            MessageHeaders headers = message.getHeaders();

            Message<?> messageToSend = getMessageBuilderFactory().fromMessage(message)
                    .removeHeaders("LogicalResourceId", "MessageId", "ReceiptHandle", "Acknowledgment")
                    .setHeader(AwsHeaders.MESSAGE_ID, headers.get("MessageId"))
                    .setHeader(AwsHeaders.RECEIPT_HANDLE, headers.get("ReceiptHandle"))
                    .setHeader(AwsHeaders.RECEIVED_QUEUE, headers.get("LogicalResourceId"))
                    .setHeader(AwsHeaders.ACKNOWLEDGMENT, headers.get("Acknowledgment")).build();

            sendMessage(messageToSend);
        }

    }
}
