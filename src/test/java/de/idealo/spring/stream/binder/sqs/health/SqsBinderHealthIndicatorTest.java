package de.idealo.spring.stream.binder.sqs.health;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import java.util.Collections;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.Status;
import org.springframework.integration.aws.inbound.SqsMessageDrivenChannelAdapter;

import com.amazonaws.SdkClientException;
import com.amazonaws.services.sqs.AmazonSQSAsync;
import com.amazonaws.services.sqs.model.GetQueueUrlResult;
import com.amazonaws.services.sqs.model.QueueDoesNotExistException;

import de.idealo.spring.stream.binder.sqs.SqsMessageHandlerBinder;

@ExtendWith(MockitoExtension.class)
class SqsBinderHealthIndicatorTest {

    @Mock
    private SqsMessageHandlerBinder sqsMessageHandlerBinder;

    @Mock
    private AmazonSQSAsync amazonSQS;

    @Mock
    private SqsMessageDrivenChannelAdapter adapter;

    @InjectMocks
    private SqsBinderHealthIndicator healthIndicator;

    @BeforeEach
    void setUp() {
        when(sqsMessageHandlerBinder.getAmazonSQS()).thenReturn(amazonSQS);
        when(sqsMessageHandlerBinder.getAdapters()).thenReturn(Collections.singletonList(adapter));
    }

    @Test
    void reportsTrueWhenAllConfiguredQueuesAreRunning() {
        when(adapter.getQueues()).thenReturn(new String[] {"queue1", "queue2"});
        when(adapter.isRunning(any())).thenReturn(true);
        when(amazonSQS.getQueueUrl(anyString())).thenReturn(new GetQueueUrlResult().withQueueUrl("http://queue.url"));

        Health.Builder builder = new Health.Builder();

        healthIndicator.doHealthCheck(builder);

        assertThat(builder.build().getStatus()).isEqualTo(Status.UP);

    }

    @Test
    void reportsFalseIfAtLeastOneConfiguredQueueIsNotRunning() {
        when(adapter.getQueues()).thenReturn(new String[] {"queue1", "queue2"});
        when(amazonSQS.getQueueUrl(anyString())).thenReturn(new GetQueueUrlResult().withQueueUrl("http://queue.url"));
        when(adapter.isRunning("queue1")).thenReturn(true);
        when(adapter.isRunning("queue2")).thenReturn(false);

        Health.Builder builder = new Health.Builder();

        healthIndicator.doHealthCheck(builder);

        Health health = builder.build();
        assertThat(health.getStatus()).isEqualTo(Status.DOWN);
        assertThat(health.getDetails()).containsKey("queue2");
    }

    @Test
    void reportsFalseIfAtLeastOneConfiguredQueueDoesNotExist() {
        when(adapter.getQueues()).thenReturn(new String[] {"queue1", "queue2"});
        when(adapter.isRunning(any())).thenReturn(true);
        when(amazonSQS.getQueueUrl(anyString())).thenThrow(QueueDoesNotExistException.class);

        Health.Builder builder = new Health.Builder();

        healthIndicator.doHealthCheck(builder);

        Health health = builder.build();
        assertThat(health.getStatus()).isEqualTo(Status.DOWN);
        assertThat(health.getDetails()).containsKey("queue1");
    }

    @Test
    void reportsFalseIfAtLeastOneConfiguredQueueIsNotReachable() {
        when(adapter.getQueues()).thenReturn(new String[] {"queue1", "queue2"});
        when(adapter.isRunning(any())).thenReturn(true);
        when(amazonSQS.getQueueUrl(anyString())).thenThrow(SdkClientException.class);

        Health.Builder builder = new Health.Builder();

        healthIndicator.doHealthCheck(builder);

        Health health = builder.build();
        assertThat(health.getStatus()).isEqualTo(Status.DOWN);
        assertThat(health.getDetails()).containsKey("queue1");
    }
}
