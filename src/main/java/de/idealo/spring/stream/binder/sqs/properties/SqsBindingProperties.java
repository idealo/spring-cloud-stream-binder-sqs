package de.idealo.spring.stream.binder.sqs.properties;

import org.springframework.cloud.stream.binder.BinderSpecificPropertiesProvider;

public class SqsBindingProperties implements BinderSpecificPropertiesProvider {

    private SqsConsumerProperties consumer = new SqsConsumerProperties();

    private SqsProducerProperties producer = new SqsProducerProperties();

    @Override
    public SqsConsumerProperties getConsumer() {
        return consumer;
    }

    public void setConsumer(SqsConsumerProperties consumer) {
        this.consumer = consumer;
    }

    @Override
    public SqsProducerProperties getProducer() {
        return producer;
    }

    public void setProducer(SqsProducerProperties producer) {
        this.producer = producer;
    }

}
