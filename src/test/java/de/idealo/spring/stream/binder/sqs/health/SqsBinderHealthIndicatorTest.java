package de.idealo.spring.stream.binder.sqs.health;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.HashMap;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.Status;

import com.amazonaws.SdkClientException;
import com.amazonaws.services.sqs.AmazonSQSAsync;
import com.amazonaws.services.sqs.model.GetQueueAttributesResult;
import com.amazonaws.services.sqs.model.GetQueueUrlResult;
import com.amazonaws.services.sqs.model.QueueDoesNotExistException;

import de.idealo.spring.stream.binder.sqs.SqsMessageHandlerBinder;
import de.idealo.spring.stream.binder.sqs.inbound.SqsInboundChannelAdapter;

@ExtendWith(MockitoExtension.class)
class SqsBinderHealthIndicatorTest {

    @Mock
    private SqsMessageHandlerBinder sqsMessageHandlerBinder;

    @Mock
    private AmazonSQSAsync amazonSQS;

    @Mock
    private SqsInboundChannelAdapter adapter;

    @InjectMocks
    private SqsBinderHealthIndicator healthIndicator;

    @BeforeEach
    void setUp() {
        lenient().when(sqsMessageHandlerBinder.getAmazonSQS()).thenReturn(amazonSQS);
        when(sqsMessageHandlerBinder.getAdapters()).thenReturn(Collections.singletonList(adapter));
    }

    @Test
    void reportsTrueWhenAllConfiguredQueuesAreRunning() {
        when(adapter.getQueues()).thenReturn(new String[] { "queue1", "queue2" });
        when(adapter.isRunning(any())).thenReturn(true);
        when(amazonSQS.getQueueUrl(anyString())).thenReturn(new GetQueueUrlResult().withQueueUrl("http://queue.url"));

        Health.Builder builder = new Health.Builder();

        healthIndicator.doHealthCheck(builder);

        assertThat(builder.build().getStatus()).isEqualTo(Status.UP);
    }

    @Test
    void reportsTrueWhenAllConfiguredQueueUrlsAreRunning() {
        when(adapter.getQueues()).thenReturn(new String[] { "https://sqs.eu-central-1.amazonaws.com/1234567890/queue1", "https://sqs.eu-central-1.amazonaws.com/1234567890/queue2" });
        when(adapter.isRunning(any())).thenReturn(true);
        when(amazonSQS.getQueueAttributes(anyString(), anyList())).thenReturn(new GetQueueAttributesResult().withAttributes(new HashMap<String, String>() {{
            put("CreatedTimestamp", "1234567890");
        }}));

        Health.Builder builder = new Health.Builder();

        healthIndicator.doHealthCheck(builder);

        assertThat(builder.build().getStatus()).isEqualTo(Status.UP);

    }

    @Test
    void reportsUnknownWhenNoBindingsAreConfigured() {
        when(sqsMessageHandlerBinder.getAdapters()).thenReturn(Collections.emptyList());

        Health.Builder builder = new Health.Builder();

        healthIndicator.doHealthCheck(builder);

        assertThat(builder.build().getStatus()).isEqualTo(Status.UNKNOWN);
    }

    @Test
    void reportsFalseIfAtLeastOneConfiguredQueueIsNotRunning() {
        when(adapter.getQueues()).thenReturn(new String[] { "queue1", "queue2" });
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
        when(adapter.getQueues()).thenReturn(new String[] { "queue1", "queue2" });
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
        when(adapter.getQueues()).thenReturn(new String[] { "queue1", "queue2" });
        when(adapter.isRunning(any())).thenReturn(true);
        when(amazonSQS.getQueueUrl(anyString())).thenThrow(SdkClientException.class);

        Health.Builder builder = new Health.Builder();

        healthIndicator.doHealthCheck(builder);

        Health health = builder.build();
        assertThat(health.getStatus()).isEqualTo(Status.DOWN);
        assertThat(health.getDetails()).containsKey("queue1");
    }

    @Test
    void reportsFalseIfAtLeastOneConfiguredQueueUrlDoesNotExist() {
        when(adapter.getQueues()).thenReturn(new String[] { "https://sqs.eu-central-1.amazonaws.com/1234567890/queue1", "https://sqs.eu-central-1.amazonaws.com/1234567890/queue2" });
        when(adapter.isRunning(any())).thenReturn(true);
        when(amazonSQS.getQueueAttributes(anyString(), anyList())).thenThrow(QueueDoesNotExistException.class);

        Health.Builder builder = new Health.Builder();

        healthIndicator.doHealthCheck(builder);

        Health health = builder.build();
        assertThat(health.getStatus()).isEqualTo(Status.DOWN);
        assertThat(health.getDetails()).containsKey("https://sqs.eu-central-1.amazonaws.com/1234567890/queue1");
    }

    @Test
    void reportsFalseIfAtLeastOneConfiguredQueueUrlIsNotReachable() {
        when(adapter.getQueues()).thenReturn(new String[] { "https://sqs.eu-central-1.amazonaws.com/12345678901/queue1", "https://sqs.eu-central-1.amazonaws.com/12345678901/queue2" });
        when(adapter.isRunning(any())).thenReturn(true);
        when(amazonSQS.getQueueAttributes(anyString(), anyList())).thenThrow(SdkClientException.class);

        Health.Builder builder = new Health.Builder();

        healthIndicator.doHealthCheck(builder);

        Health health = builder.build();
        assertThat(health.getStatus()).isEqualTo(Status.DOWN);
        assertThat(health.getDetails()).containsKey("https://sqs.eu-central-1.amazonaws.com/12345678901/queue1");
    }
}
