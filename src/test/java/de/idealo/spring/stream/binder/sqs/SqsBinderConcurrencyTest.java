package de.idealo.spring.stream.binder.sqs;

import static org.assertj.core.api.Assertions.assertThat;
import static org.testcontainers.containers.localstack.LocalStackContainer.Service.SQS;

import java.time.Duration;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.testcontainers.containers.localstack.LocalStackContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import reactor.core.publisher.Sinks;
import reactor.test.StepVerifier;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;
import software.amazon.awssdk.services.sqs.model.GetQueueUrlRequest;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;

@Testcontainers
@SpringBootTest(properties = {
        "spring.cloud.aws.region.static=eu-central-1",
        "spring.cloud.stream.bindings.input-in-0.destination=concurrentQueue",
        "spring.cloud.stream.sqs.bindings.input-in-0.consumer.snsFanout=false",
        "spring.cloud.stream.sqs.bindings.input-in-0.consumer.maxNumberOfMessages=1",
        "spring.cloud.function.definition=input",
        "spring.cloud.stream.bindings.input-in-0.consumer.concurrency=2"
})
class SqsBinderConcurrencyTest {

    @Container
    private static final LocalStackContainer localStack = new LocalStackContainer(DockerImageName.parse("localstack/localstack:0.12.10"))
            .withServices(SQS)
            .withEnv("DEFAULT_REGION", "eu-central-1");

    private static final Sinks.Many<String> inputSink = Sinks.many().multicast().onBackpressureBuffer();

    @Autowired
    private SqsAsyncClient amazonSQS;

    @BeforeAll
    static void beforeAll() throws Exception {
        localStack.execInContainer("awslocal", "sqs", "create-queue", "--queue-name", "concurrentQueue");
    }

    @Test
    void shouldPassMessageToConsumer() throws ExecutionException, InterruptedException {
        String queueUrl = amazonSQS.getQueueUrl(GetQueueUrlRequest.builder().queueName("concurrentQueue").build()).get().queueUrl();

        amazonSQS.sendMessage(SendMessageRequest.builder().queueUrl(queueUrl).messageBody("delayedMessage").build());
        amazonSQS.sendMessage(SendMessageRequest.builder().queueUrl(queueUrl).messageBody("processedMessage").build());

        StepVerifier.create(inputSink.asFlux())
                .assertNext(message -> {
                    assertThat(message).isEqualTo("processedMessage");
                })
                .thenAwait(Duration.ofSeconds(1))
                .assertNext(message -> {
                    assertThat(message).isEqualTo("delayedMessage");
                })
                .verifyTimeout(Duration.ofSeconds(2));
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
            return (input) -> {
                if ("delayedMessage".equals(input)) {
                    try {
                        Thread.sleep(1000L);
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                }
                inputSink.tryEmitNext(input);
            };
        }
    }

    @SpringBootApplication
    static class Application {
    }
}
