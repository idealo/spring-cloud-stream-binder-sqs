package de.idealo.spring.stream.binder.sqs;

import static org.assertj.core.api.Assertions.assertThat;
import static org.testcontainers.containers.localstack.LocalStackContainer.Service.SQS;

import java.util.Locale;
import java.util.function.Function;

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

import com.amazonaws.services.sqs.AmazonSQSAsync;
import com.amazonaws.services.sqs.AmazonSQSAsyncClientBuilder;

@Testcontainers
@SpringBootTest(properties = {
        "cloud.aws.stack.auto=false",
        "cloud.aws.region.static=eu-central-1",
        "spring.cloud.stream.bindings.input-in-0.destination=queue1",
        "spring.cloud.stream.bindings.function.definition=input"
})
class SqsBinderAT {

    @Container
    static private LocalStackContainer localStack = new LocalStackContainer(DockerImageName.parse("localstack/localstack:0.12.10"))
            .withServices(SQS)
            .withEnv("DEFAULT_REGION", "eu-central-1");

    @Autowired
    private AmazonSQSAsync amazonSQS;

    @Autowired
    private HealthEndpoint healthEndpoint;

    @BeforeAll
    static void beforeAll() throws Exception {
        localStack.execInContainer("awslocal", "sqs", "create-queue", "--queue-name", "queue1");
    }

    @Test
    void canTestHealth() throws Exception {

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
        Function<String, String> input() {
            return (String s) -> s.toUpperCase(Locale.getDefault());
        }
    }

    @SpringBootApplication
    static class Application {
    }
}
