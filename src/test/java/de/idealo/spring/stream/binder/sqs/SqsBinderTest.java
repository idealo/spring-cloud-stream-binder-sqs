package de.idealo.spring.stream.binder.sqs;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.testcontainers.containers.localstack.LocalStackContainer.Service.SQS;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Supplier;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.health.HealthEndpoint;
import org.springframework.boot.actuate.health.Status;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.context.annotation.Bean;
import org.springframework.messaging.support.MessageBuilder;
import org.testcontainers.containers.localstack.LocalStackContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import com.amazonaws.services.sqs.AmazonSQSAsync;
import com.amazonaws.services.sqs.AmazonSQSAsyncClientBuilder;
import com.amazonaws.services.sqs.model.Message;
import com.amazonaws.services.sqs.model.SendMessageRequest;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;
import reactor.test.StepVerifier;

@Testcontainers
@SpringBootTest(properties = {
        "cloud.aws.stack.auto=false",
        "cloud.aws.region.static=eu-central-1",
        "spring.cloud.stream.bindings.input-in-0.destination=queue1",
        "spring.cloud.stream.sqs.bindings.input-in-0.consumer.snsFanout=false",
        "spring.cloud.stream.bindings.output-out-0.destination=queue2",
        "spring.cloud.stream.bindings.fifoOutput-out-0.destination=queue3.fifo",
        "spring.cloud.function.definition=input;output;fifoOutput"
})
class SqsBinderTest {

    @Container
    private static final LocalStackContainer localStack = new LocalStackContainer(DockerImageName.parse("localstack/localstack:0.12.10"))
            .withServices(SQS)
            .withEnv("DEFAULT_REGION", "eu-central-1");

    private static final Sinks.Many<String> inputSink = Sinks.many().multicast().onBackpressureBuffer();
    private static final Sinks.Many<String> outputSink = Sinks.many().multicast().onBackpressureBuffer();
    private static final Sinks.Many<org.springframework.messaging.Message<String>> fifoOutputSink = Sinks.many().multicast().onBackpressureBuffer();

    @SpyBean
    private AmazonSQSAsync amazonSQS;

    @Autowired
    private HealthEndpoint healthEndpoint;

    @BeforeAll
    static void beforeAll() throws Exception {
        localStack.execInContainer("awslocal", "sqs", "create-queue", "--queue-name", "queue1");
        localStack.execInContainer("awslocal", "sqs", "create-queue", "--queue-name", "queue2");
        localStack.execInContainer("awslocal", "sqs", "create-queue", "--queue-name", "queue3.fifo", "--attributes", "FifoQueue=true");
    }

    @Test
    void shouldPassMessageToConsumer() {
        String testMessage = "test message";

        String queueUrl = amazonSQS.getQueueUrl("queue1").getQueueUrl();
        amazonSQS.sendMessage(queueUrl, testMessage);

        StepVerifier.create(inputSink.asFlux())
                .assertNext(message -> {
                    assertThat(message).isEqualTo(testMessage);
                })
                .verifyTimeout(Duration.ofSeconds(1));
    }

    @Test
    void shouldPublishMessageFromProducer() {
        String testMessage = "test message";

        outputSink.tryEmitNext(testMessage);

        String queueUrl = amazonSQS.getQueueUrl("queue2").getQueueUrl();

        await().atMost(1, TimeUnit.SECONDS).untilAsserted(() -> {
            List<Message> messages = amazonSQS.receiveMessage(queueUrl).getMessages();
            assertThat(messages).hasSize(1)
                    .extracting("body")
                    .containsExactly(testMessage);
        });
    }

    @Test
    void shouldPublishMessageToFifoQueue() {
        org.springframework.messaging.Message<String> message = MessageBuilder
                .withPayload("fifo body")
                .setHeader(SqsHeaders.GROUP_ID, "my-group")
                .setHeader(SqsHeaders.DEDUPLICATION_ID, "unique1")
                .build();

        fifoOutputSink.tryEmitNext(message);

        ArgumentCaptor<SendMessageRequest> captor = ArgumentCaptor.forClass(SendMessageRequest.class);
        verify(amazonSQS, timeout(500)).sendMessageAsync(captor.capture(), any());

        SendMessageRequest actualRequest = captor.getValue();
        assertThat(actualRequest.getMessageGroupId()).isEqualTo("my-group");
        assertThat(actualRequest.getMessageDeduplicationId()).isEqualTo("unique1");

        String queueUrl = amazonSQS.getQueueUrl("queue3.fifo").getQueueUrl();
        await().atMost(1, TimeUnit.SECONDS).untilAsserted(() -> {
            List<Message> messages = amazonSQS.receiveMessage(queueUrl).getMessages();
            assertThat(messages).hasSize(1)
                    .extracting("body")
                    .containsExactly(message.getPayload());
        });
    }

    @Test
    void canTestHealth() {
        assertThat(healthEndpoint.health().getStatus()).isEqualTo(Status.UP);
        assertThat(healthEndpoint.healthForPath("sqsBinder").getStatus()).isEqualTo(Status.UP);
    }

    @TestConfiguration
    static class AwsConfig {

        @Bean
        AmazonSQSAsync amazonSQS() {
            return AmazonSQSAsyncClientBuilder.standard()
                    .withEndpointConfiguration(localStack.getEndpointConfiguration(SQS))
                    .withCredentials(localStack.getDefaultCredentialsProvider())
                    .build();
        }

        @Bean
        Consumer<String> input() {
            return inputSink::tryEmitNext;
        }

        @Bean
        Supplier<Flux<String>> output() {
            return outputSink::asFlux;
        }

        @Bean
        Supplier<Flux<org.springframework.messaging.Message<String>>> fifoOutput() {
            return fifoOutputSink::asFlux;
        }
    }

    @SpringBootApplication
    static class Application {
    }
}
