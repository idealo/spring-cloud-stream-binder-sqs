package de.idealo.spring.stream.binder.sqs;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cloud.stream.binder.ExtendedConsumerProperties;

import software.amazon.awssdk.services.sqs.SqsAsyncClient;

import de.idealo.spring.stream.binder.sqs.properties.SqsConsumerProperties;
import de.idealo.spring.stream.binder.sqs.properties.SqsExtendedBindingProperties;
import de.idealo.spring.stream.binder.sqs.provisioning.SqsDestination;
import de.idealo.spring.stream.binder.sqs.provisioning.SqsStreamProvisioner;

@ExtendWith(MockitoExtension.class)
class SqsMessageHandlerBinderTest {

    @Mock
    private SqsAsyncClient amazonSQS;

    private SqsMessageHandlerBinder sqsMessageHandlerBinder;

    @BeforeEach
    void setUp() {
        this.sqsMessageHandlerBinder = new SqsMessageHandlerBinder(amazonSQS, new SqsStreamProvisioner(), new SqsExtendedBindingProperties());
    }

    @Test
    void shouldSaveConsumerAdapter() throws Exception {
        String queueName = "queue1";

        sqsMessageHandlerBinder.createConsumerEndpoint(new SqsDestination(queueName), "group", new ExtendedConsumerProperties<>(new SqsConsumerProperties()));

        assertThat(sqsMessageHandlerBinder.getAdapters()).isNotEmpty();
        assertThat(sqsMessageHandlerBinder.getAdapters().get(0).getQueues()).containsExactly(queueName);
    }

}