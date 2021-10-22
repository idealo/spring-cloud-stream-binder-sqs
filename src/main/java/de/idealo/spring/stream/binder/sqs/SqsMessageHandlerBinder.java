package de.idealo.spring.stream.binder.sqs;

import java.util.ArrayList;
import java.util.List;

import org.springframework.cloud.stream.binder.AbstractMessageChannelBinder;
import org.springframework.cloud.stream.binder.BinderSpecificPropertiesProvider;
import org.springframework.cloud.stream.binder.ExtendedConsumerProperties;
import org.springframework.cloud.stream.binder.ExtendedProducerProperties;
import org.springframework.cloud.stream.binder.ExtendedPropertiesBinder;
import org.springframework.cloud.stream.provisioning.ConsumerDestination;
import org.springframework.cloud.stream.provisioning.ProducerDestination;
import org.springframework.integration.aws.inbound.SqsMessageDrivenChannelAdapter;
import org.springframework.integration.aws.outbound.SqsMessageHandler;
import org.springframework.integration.channel.AbstractMessageChannel;
import org.springframework.integration.core.MessageProducer;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHandler;

import com.amazonaws.services.sqs.AmazonSQSAsync;

import de.idealo.spring.stream.binder.sqs.properties.SqsConsumerProperties;
import de.idealo.spring.stream.binder.sqs.properties.SqsExtendedBindingProperties;
import de.idealo.spring.stream.binder.sqs.properties.SqsProducerProperties;
import de.idealo.spring.stream.binder.sqs.provisioning.SqsStreamProvisioner;

public class SqsMessageHandlerBinder
        extends AbstractMessageChannelBinder<ExtendedConsumerProperties<SqsConsumerProperties>, ExtendedProducerProperties<SqsProducerProperties>, SqsStreamProvisioner>
        implements ExtendedPropertiesBinder<MessageChannel, SqsConsumerProperties, SqsProducerProperties> {

    private final AmazonSQSAsync amazonSQS;
    private final SqsExtendedBindingProperties extendedBindingProperties;
    private final List<SqsMessageDrivenChannelAdapter> adapters = new ArrayList<>();

    public SqsMessageHandlerBinder(AmazonSQSAsync amazonSQS, SqsStreamProvisioner provisioningProvider, SqsExtendedBindingProperties extendedBindingProperties) {
        super(new String[0], provisioningProvider);
        this.amazonSQS = amazonSQS;
        this.extendedBindingProperties = extendedBindingProperties;
    }

    public AmazonSQSAsync getAmazonSQS() {
        return amazonSQS;
    }

    public List<SqsMessageDrivenChannelAdapter> getAdapters() {
        return adapters;
    }

    @Override
    protected MessageHandler createProducerMessageHandler(ProducerDestination destination, ExtendedProducerProperties<SqsProducerProperties> producerProperties, MessageChannel errorChannel) throws Exception {
        SqsMessageHandler sqsMessageHandler = new SqsMessageHandler(amazonSQS);
        sqsMessageHandler.setQueue(destination.getName());
        sqsMessageHandler.setFailureChannel(errorChannel);
        sqsMessageHandler.setBeanFactory(getBeanFactory());
        return sqsMessageHandler;
    }

    @Override
    protected MessageProducer createConsumerEndpoint(ConsumerDestination destination, String group, ExtendedConsumerProperties<SqsConsumerProperties> properties) throws Exception {
        SqsMessageDrivenChannelAdapter adapter = new SqsMessageDrivenChannelAdapter(amazonSQS, destination.getName());
        adapter.setMaxNumberOfMessages(properties.getExtension().getMaxNumberOfMessages());
        adapter.setVisibilityTimeout(properties.getExtension().getVisibilityTimeout());
        adapter.setWaitTimeOut(properties.getExtension().getWaitTimeout());

        if (properties.getExtension().getMessageDeletionPolicy() != null) {
            adapter.setMessageDeletionPolicy(properties.getExtension().getMessageDeletionPolicy());
        }

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
