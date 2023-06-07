package de.idealo.spring.stream.binder.sqs;

import static org.assertj.core.api.Assertions.assertThat;
import static org.testcontainers.containers.localstack.LocalStackContainer.Service.SQS;

import java.time.Duration;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.health.HealthEndpoint;
import org.springframework.boot.actuate.health.Status;
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
import software.amazon.awssdk.services.sqs.SqsAsyncClient;
import software.amazon.awssdk.services.sqs.model.GetQueueUrlRequest;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;

@Testcontainers
@SpringBootTest(properties = {
        "cloud.aws.stack.auto=false",
        "cloud.aws.region.static=eu-central-1",
        "spring.cloud.stream.bindings.input-in-0.destination=queue1",
        "spring.cloud.stream.bindings.function.definition=input"
})
class SqsBinderForSNSTest {

    @Container
    private static final LocalStackContainer localStack = new LocalStackContainer(DockerImageName.parse("localstack/localstack:0.12.10"))
            .withServices(SQS)
            .withEnv("DEFAULT_REGION", "eu-central-1");

    private static final Sinks.Many<String> sink = Sinks.many().multicast().onBackpressureBuffer();

    @Autowired
    private SqsAsyncClient amazonSQS;

    @Autowired
    private HealthEndpoint healthEndpoint;

    @BeforeAll
    static void beforeAll() throws Exception {
        localStack.execInContainer("awslocal", "sqs", "create-queue", "--queue-name", "queue1");
    }

    @Test
    void shouldPassMessageToConsumer() throws ExecutionException, InterruptedException {
        final String testMessage = "test message";

        String json =
                "{" +
                        "\"Type\": \"Notification\"," +
                        "\"Message\": \"" + testMessage + "\"" +
                        "}";

        String queueUrl = amazonSQS.getQueueUrl(GetQueueUrlRequest.builder().queueName("queue1").build()).get().queueUrl();
        amazonSQS.sendMessage(SendMessageRequest.builder().queueUrl(queueUrl).messageBody(json).build());

        StepVerifier.create(sink.asFlux())
                .assertNext(message -> {
                    assertThat(message).isEqualTo(testMessage);
                })
                .verifyTimeout(Duration.ofSeconds(1));
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
                    .build();
        }

        @Bean
        Consumer<String> input() {
            return sink::tryEmitNext;
        }
    }

    @SpringBootApplication
    static class Application {
    }
}
