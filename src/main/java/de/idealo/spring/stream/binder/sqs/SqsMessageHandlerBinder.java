package de.idealo.spring.stream.binder.sqs;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import org.springframework.cloud.stream.binder.AbstractMessageChannelBinder;
import org.springframework.cloud.stream.binder.BinderSpecificPropertiesProvider;
import org.springframework.cloud.stream.binder.ExtendedConsumerProperties;
import org.springframework.cloud.stream.binder.ExtendedProducerProperties;
import org.springframework.cloud.stream.binder.ExtendedPropertiesBinder;
import org.springframework.cloud.stream.provisioning.ConsumerDestination;
import org.springframework.cloud.stream.provisioning.ProducerDestination;
import org.springframework.integration.aws.outbound.SqsMessageHandler;
import org.springframework.integration.channel.AbstractMessageChannel;
import org.springframework.integration.core.MessageProducer;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHandler;

import io.awspring.cloud.sqs.listener.QueueNotFoundStrategy;
import io.awspring.cloud.sqs.listener.SqsContainerOptions;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;

import de.idealo.spring.stream.binder.sqs.inbound.SqsInboundChannelAdapter;
import de.idealo.spring.stream.binder.sqs.properties.SqsConsumerProperties;
import de.idealo.spring.stream.binder.sqs.properties.SqsExtendedBindingProperties;
import de.idealo.spring.stream.binder.sqs.properties.SqsProducerProperties;
import de.idealo.spring.stream.binder.sqs.provisioning.SqsStreamProvisioner;

public class SqsMessageHandlerBinder
        extends AbstractMessageChannelBinder<ExtendedConsumerProperties<SqsConsumerProperties>, ExtendedProducerProperties<SqsProducerProperties>, SqsStreamProvisioner>
        implements ExtendedPropertiesBinder<MessageChannel, SqsConsumerProperties, SqsProducerProperties> {

    private final SqsAsyncClient sqsAsyncClient;
    private final SqsExtendedBindingProperties extendedBindingProperties;
    private final List<SqsInboundChannelAdapter> adapters = new ArrayList<>();

    public SqsMessageHandlerBinder(SqsAsyncClient amazonSQS, SqsStreamProvisioner provisioningProvider, SqsExtendedBindingProperties extendedBindingProperties) {
        super(new String[0], provisioningProvider);
        this.sqsAsyncClient = amazonSQS;
        this.extendedBindingProperties = extendedBindingProperties;
    }

    public SqsAsyncClient getSqsAsyncClient() {
        return sqsAsyncClient;
    }

    public List<SqsInboundChannelAdapter> getAdapters() {
        return new ArrayList<>(adapters);
    }

    @Override
    protected MessageHandler createProducerMessageHandler(ProducerDestination destination, ExtendedProducerProperties<SqsProducerProperties> producerProperties, MessageChannel errorChannel) throws Exception {
        SqsMessageHandler sqsMessageHandler = new SqsMessageHandler(sqsAsyncClient);
        sqsMessageHandler.setQueue(destination.getName());
        sqsMessageHandler.setBeanFactory(getBeanFactory());

        sqsMessageHandler.setDelayExpressionString(String.format("headers.get('%s')", SqsHeaders.DELAY));
        sqsMessageHandler.setMessageGroupIdExpressionString(String.format("headers.get('%s')", SqsHeaders.GROUP_ID));
        sqsMessageHandler.setMessageDeduplicationIdExpressionString(String.format("headers.get('%s')", SqsHeaders.DEDUPLICATION_ID));

        return sqsMessageHandler;
    }

    @Override
    protected MessageProducer createConsumerEndpoint(ConsumerDestination destination, String group, ExtendedConsumerProperties<SqsConsumerProperties> properties) throws Exception {
        final SqsContainerOptions sqsContainerOptions =
                SqsContainerOptions.builder()
                        .maxMessagesPerPoll(properties.getExtension().getMaxNumberOfMessages())
                        .messageVisibility(Duration.ofSeconds(properties.getExtension().getVisibilityTimeout()))
                        .pollTimeout(Duration.ofSeconds(properties.getExtension().getWaitTimeout()))
                        .listenerShutdownTimeout(Duration.ofSeconds(properties.getExtension().getQueueStopTimeout()))
                        .queueNotFoundStrategy(QueueNotFoundStrategy.FAIL)
                        .build();
        SqsInboundChannelAdapter adapter = new SqsInboundChannelAdapter(sqsAsyncClient, destination.getName());
        adapter.setSqsContainerOptions(sqsContainerOptions);
        adapter.setConcurrency(properties.getConcurrency());

        if (properties.getExtension().isSnsFanout()) {
            adapter.setMessageBuilderFactory(new SnsFanoutMessageBuilderFactory());
        }

        this.adapters.add(adapter);

        return adapter;
    }

    @Override
    public SqsConsumerProperties getExtendedConsumerProperties(String channelName) {
        return this.extendedBindingProperties.getExtendedConsumerProperties(channelName);
    }

    @Override
    public SqsProducerProperties getExtendedProducerProperties(String channelName) {
        return this.extendedBindingProperties.getExtendedProducerProperties(channelName);
    }

    @Override
    public String getDefaultsPrefix() {
        return this.extendedBindingProperties.getDefaultsPrefix();
    }

    @Override
    public Class<? extends BinderSpecificPropertiesProvider> getExtendedPropertiesEntryClass() {
        return this.extendedBindingProperties.getExtendedPropertiesEntryClass();
    }

    @Override
    protected void postProcessOutputChannel(MessageChannel outputChannel, ExtendedProducerProperties<SqsProducerProperties> producerProperties) {
        ((AbstractMessageChannel) outputChannel).addInterceptor(new SqsPayloadConvertingChannelInterceptor());
    }
}
