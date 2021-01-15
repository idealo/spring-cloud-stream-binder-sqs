package de.idealo.spring.stream.binder.sqs.provisioning;

import org.springframework.cloud.stream.binder.ExtendedConsumerProperties;
import org.springframework.cloud.stream.binder.ExtendedProducerProperties;
import org.springframework.cloud.stream.provisioning.ConsumerDestination;
import org.springframework.cloud.stream.provisioning.ProducerDestination;
import org.springframework.cloud.stream.provisioning.ProvisioningProvider;

import de.idealo.spring.stream.binder.sqs.properties.SqsConsumerProperties;
import de.idealo.spring.stream.binder.sqs.properties.SqsProducerProperties;

public class SqsStreamProvisioner implements ProvisioningProvider<ExtendedConsumerProperties<SqsConsumerProperties>, ExtendedProducerProperties<SqsProducerProperties>> {
    @Override
    public ProducerDestination provisionProducerDestination(String name, ExtendedProducerProperties<SqsProducerProperties> properties) {
        return new SqsDestination(name);
    }

    @Override
    public ConsumerDestination provisionConsumerDestination(String name, String group, ExtendedConsumerProperties<SqsConsumerProperties> properties) {
        return new SqsDestination(name);
    }
}
