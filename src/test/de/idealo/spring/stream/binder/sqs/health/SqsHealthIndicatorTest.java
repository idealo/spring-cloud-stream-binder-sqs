package de.idealo.spring.stream.binder.sqs.health;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.Status;

import com.amazonaws.SdkClientException;
import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.model.GetQueueUrlResult;
import com.amazonaws.services.sqs.model.QueueDoesNotExistException;
import io.awspring.cloud.messaging.listener.SimpleMessageListenerContainer;

class SqsHealthIndicatorTest {

    private final SimpleMessageListenerContainer simpleMessageListenerContainer = mock(SimpleMessageListenerContainer.class);

    private final AmazonSQS amazonSQS = mock(AmazonSQS.class);

    private final SqsHealthProperties sqsHealthProperties = mock(SqsHealthProperties.class);

    private final SqsHealthIndicator healthIndicator = new SqsHealthIndicator(simpleMessageListenerContainer, amazonSQS, sqsHealthProperties);

    @Test
    public void reportsTrueWhenAllConfiguredQueuesAreRunning() {

        when(sqsHealthProperties.getQueueNames()).thenReturn(new String[] {"queue1", "queue2"});
        when(simpleMessageListenerContainer.isRunning(any())).thenReturn(true);
        when(amazonSQS.getQueueUrl(anyString())).thenReturn(new GetQueueUrlResult().withQueueUrl("http://queue.url"));

        Health.Builder builder = new Health.Builder();

        healthIndicator.doHealthCheck(builder);

        assertThat(builder.build().getStatus()).isEqualTo(Status.UP);

    }

    @Test
    public void reportsTrueNoQueuesAreConfigured() {
        when(sqsHealthProperties.getQueueNames()).thenReturn(new String[]{});
        Health.Builder builder = new Health.Builder();

        healthIndicator.doHealthCheck(builder);

        assertThat(builder.build().getStatus()).isEqualTo(Status.UP);
    }

    @Test
    public void reportsFalseIfAtLeastOneConfiguredQueueIsNotRunning() {

        when(sqsHealthProperties.getQueueNames()).thenReturn(new String[] {"queue1", "queue2"});
        when(amazonSQS.getQueueUrl(anyString())).thenReturn(new GetQueueUrlResult().withQueueUrl("http://queue.url"));
        when(simpleMessageListenerContainer.isRunning("queue1")).thenReturn(true);
        when(simpleMessageListenerContainer.isRunning("queue2")).thenReturn(false);

        Health.Builder builder = new Health.Builder();

        healthIndicator.doHealthCheck(builder);

        Health health = builder.build();
        assertThat(health.getStatus()).isEqualTo(Status.DOWN);
        assertThat(health.getDetails()).containsKey("queue2");
    }

    @Test
    public void reportsFalseIfAtLeastOneConfiguredQueueDoesNotExist() {

        when(sqsHealthProperties.getQueueNames()).thenReturn(new String[] {"queue1", "queue2"});
        when(simpleMessageListenerContainer.isRunning(any())).thenReturn(true);
        when(amazonSQS.getQueueUrl(anyString())).thenThrow(QueueDoesNotExistException.class);

        Health.Builder builder = new Health.Builder();

        healthIndicator.doHealthCheck(builder);

        Health health = builder.build();
        assertThat(health.getStatus()).isEqualTo(Status.DOWN);
        assertThat(health.getDetails()).containsKey("queue1");
    }

    @Test
    public void reportsFalseIfAtLeastOneConfiguredQueueIsNotReachable() {

        when(sqsHealthProperties.getQueueNames()).thenReturn(new String[] {"queue1", "queue2"});
        when(simpleMessageListenerContainer.isRunning(any())).thenReturn(true);
        when(amazonSQS.getQueueUrl(anyString())).thenThrow(SdkClientException.class);

        Health.Builder builder = new Health.Builder();

        healthIndicator.doHealthCheck(builder);

        Health health = builder.build();
        assertThat(health.getStatus()).isEqualTo(Status.DOWN);
        assertThat(health.getDetails()).containsKey("queue1");
    }
}
