package de.idealo.spring.stream.binder.sqs;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.testcontainers.containers.localstack.LocalStackContainer.Service.SQS;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Supplier;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.health.HealthEndpoint;
import org.springframework.boot.actuate.health.Status;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.messaging.support.MessageBuilder;
import org.testcontainers.containers.localstack.LocalStackContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;
import reactor.test.StepVerifier;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;
import software.amazon.awssdk.services.sqs.model.GetQueueUrlRequest;
import software.amazon.awssdk.services.sqs.model.Message;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;

@Testcontainers
@SpringBootTest(properties = {
        "spring.cloud.aws.region.static=eu-central-1",
        "spring.cloud.stream.bindings.input-in-0.destination=queue1",
        "spring.cloud.stream.sqs.bindings.input-in-0.consumer.snsFanout=false",
        "spring.cloud.stream.bindings.output-out-0.destination=queue2",
        "spring.cloud.stream.bindings.fifoOutput-out-0.destination=queue3.fifo",
        "spring.cloud.stream.bindings.delayedOutput-out-0.destination=queue4",
        "spring.cloud.function.definition=input;output;fifoOutput;delayedOutput"
})
class SqsBinderTest {

    @Container
    private static final LocalStackContainer localStack = new LocalStackContainer(DockerImageName.parse("localstack/localstack:0.12.10"))
            .withServices(SQS)
            .withEnv("DEFAULT_REGION", "eu-central-1");

    private static final Sinks.Many<String> inputSink = Sinks.many().multicast().onBackpressureBuffer();
    private static final Sinks.Many<String> outputSink = Sinks.many().multicast().onBackpressureBuffer();
    private static final Sinks.Many<org.springframework.messaging.Message<String>> fifoOutputSink = Sinks.many().multicast().onBackpressureBuffer();
    private static final Sinks.Many<org.springframework.messaging.Message<String>> delayedOutputSink = Sinks.many().multicast().onBackpressureBuffer();

    @Autowired
    private SqsAsyncClient amazonSQS;

    @Autowired
    private HealthEndpoint healthEndpoint;

    @BeforeAll
    static void beforeAll() throws Exception {
        localStack.execInContainer("awslocal", "sqs", "create-queue", "--queue-name", "queue1");
        localStack.execInContainer("awslocal", "sqs", "create-queue", "--queue-name", "queue2");
        localStack.execInContainer("awslocal", "sqs", "create-queue", "--queue-name", "queue3.fifo", "--attributes", "FifoQueue=true");
        localStack.execInContainer("awslocal", "sqs", "create-queue", "--queue-name", "queue4");
    }

    @Test
    void shouldPassMessageToConsumer() throws ExecutionException, InterruptedException {
        String testMessage = "test message";

        String queueUrl = amazonSQS.getQueueUrl(GetQueueUrlRequest.builder().queueName("queue1").build()).get().queueUrl();
        amazonSQS.sendMessage(SendMessageRequest.builder().queueUrl(queueUrl).messageBody(testMessage).build()).get();

        StepVerifier.create(inputSink.asFlux())
                .assertNext(message -> {
                    assertThat(message).isEqualTo(testMessage);
                })
                .verifyTimeout(Duration.ofSeconds(1));
    }

    @Test
    void shouldPublishMessageFromProducer() throws ExecutionException, InterruptedException {
        String testMessage = "test message";

        outputSink.tryEmitNext(testMessage);

        String queueUrl = amazonSQS.getQueueUrl(GetQueueUrlRequest.builder().queueName("queue2").build()).get().queueUrl();

        await().atMost(1, TimeUnit.SECONDS).untilAsserted(() -> {
            List<Message> messages = amazonSQS.receiveMessage(ReceiveMessageRequest.builder().queueUrl(queueUrl).build()).get().messages();
            assertThat(messages).hasSize(1)
                    .extracting("body")
                    .containsExactly(testMessage);
        });
    }

    @Test
    void shouldPublishMessageToFifoQueue() throws ExecutionException, InterruptedException {
        org.springframework.messaging.Message<String> message = MessageBuilder
                .withPayload("fifo body")
                .setHeader(SqsHeaders.GROUP_ID, "my-group")
                .setHeader(SqsHeaders.DEDUPLICATION_ID, "unique1")
                .build();

        fifoOutputSink.tryEmitNext(message);

        String queueUrl = amazonSQS.getQueueUrl(GetQueueUrlRequest.builder().queueName("queue3.fifo").build()).get().queueUrl();
        await().atMost(1, TimeUnit.SECONDS).untilAsserted(() -> {
            List<Message> messages = amazonSQS.receiveMessage(ReceiveMessageRequest.builder().queueUrl(queueUrl).build()).get().messages();
            assertThat(messages).hasSize(1)
                    .extracting("body")
                    .containsExactly(message.getPayload());
        });
    }

    @Test
    void shouldPublishDelayedMessage() throws ExecutionException, InterruptedException {
        org.springframework.messaging.Message<String> message = MessageBuilder
                .withPayload("test message")
                .setHeader(SqsHeaders.DELAY, 5)
                .build();

        delayedOutputSink.tryEmitNext(message);

        String queueUrl = amazonSQS.getQueueUrl(GetQueueUrlRequest.builder().queueName("queue4").build()).get().queueUrl();
        await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
            List<Message> messages = amazonSQS.receiveMessage(ReceiveMessageRequest.builder().queueUrl(queueUrl).build()).get().messages();
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
        SqsAsyncClient amazonSQS() {
            return SqsAsyncClient.builder()
                    .endpointOverride(localStack.getEndpointOverride(SQS))
                    .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create(localStack.getAccessKey(), localStack.getSecretKey())))
                    .region(Region.EU_CENTRAL_1)
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

        @Bean
        Supplier<Flux<org.springframework.messaging.Message<String>>> delayedOutput() {
            return delayedOutputSink::asFlux;
        }
    }

    @SpringBootApplication
    static class Application {
    }
}
