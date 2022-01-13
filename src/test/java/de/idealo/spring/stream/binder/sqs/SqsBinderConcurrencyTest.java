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

    @SpyBean
    private AmazonSQSAsync amazonSQS;

    @BeforeAll
    static void beforeAll() throws Exception {
        localStack.execInContainer("awslocal", "sqs", "create-queue", "--queue-name", "concurrentQueue");
    }

    @Test
    void shouldPassMessageToConsumer() {
        String queueUrl = amazonSQS.getQueueUrl("concurrentQueue").getQueueUrl();

        amazonSQS.sendMessage(queueUrl, "delayedMessage");
        amazonSQS.sendMessage(queueUrl, "processedMessage");

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
        AmazonSQSAsync amazonSQS() {
            return AmazonSQSAsyncClientBuilder.standard()
                    .withEndpointConfiguration(localStack.getEndpointConfiguration(SQS))
                    .withCredentials(localStack.getDefaultCredentialsProvider())
                    .build();
        }

        @Bean
        Consumer<String> input() {
            return (input) -> {
                if("delayedMessage".equals(input)) {
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
