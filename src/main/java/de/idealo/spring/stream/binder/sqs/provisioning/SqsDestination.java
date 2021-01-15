package de.idealo.spring.stream.binder.sqs.provisioning;

import org.springframework.cloud.stream.provisioning.ConsumerDestination;
import org.springframework.cloud.stream.provisioning.ProducerDestination;

public class SqsDestination implements ConsumerDestination, ProducerDestination {

    private final String name;

    public SqsDestination(String name) {
        this.name = name;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getNameForPartition(int partition) {
        throw new UnsupportedOperationException("Partitioning is not supported for SQS");
    }

}
