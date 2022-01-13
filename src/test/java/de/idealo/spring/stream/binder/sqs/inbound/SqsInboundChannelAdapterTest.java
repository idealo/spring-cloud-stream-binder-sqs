package de.idealo.spring.stream.binder.sqs.inbound;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.messaging.core.DestinationResolver;
import org.springframework.test.util.ReflectionTestUtils;

import com.amazonaws.services.sqs.AmazonSQSAsync;

import io.awspring.cloud.core.env.ResourceIdResolver;
import io.awspring.cloud.messaging.config.SimpleMessageListenerContainerFactory;
import io.awspring.cloud.messaging.listener.SimpleMessageListenerContainer;

@ExtendWith(MockitoExtension.class)
class SqsInboundChannelAdapterTest {
    @Mock
    private AmazonSQSAsync amazonSQS;

    @Mock
    private SimpleMessageListenerContainerFactory listenerContainerFactory;

    @Mock
    private SimpleMessageListenerContainer listenerContainer;

    @Test
    void shouldDefaultToSingleListenerContainer() {
        SqsInboundChannelAdapter sut = new SqsInboundChannelAdapter(amazonSQS, "test1");
        ReflectionTestUtils.setField(sut, "simpleMessageListenerContainerFactory", listenerContainerFactory);
        when(listenerContainerFactory.createSimpleMessageListenerContainer()).thenReturn(listenerContainer);

        sut.afterPropertiesSet();

        verify(listenerContainerFactory, times(1)).createSimpleMessageListenerContainer();
    }

    @Test
    void shouldCreateMultipleListenerContainers() {
        SqsInboundChannelAdapter sut = new SqsInboundChannelAdapter(amazonSQS, "test1");
        ReflectionTestUtils.setField(sut, "simpleMessageListenerContainerFactory", listenerContainerFactory);
        when(listenerContainerFactory.createSimpleMessageListenerContainer()).thenReturn(listenerContainer);

        sut.setConcurrency(3);
        sut.afterPropertiesSet();

        verify(listenerContainerFactory, times(3)).createSimpleMessageListenerContainer();
    }

    @Test
    void shouldStartAllListenerContainers() {
        SqsInboundChannelAdapter sut = new SqsInboundChannelAdapter(amazonSQS, "test1");
        ReflectionTestUtils.setField(sut, "simpleMessageListenerContainerFactory", listenerContainerFactory);
        when(listenerContainerFactory.createSimpleMessageListenerContainer()).thenReturn(listenerContainer);

        sut.setConcurrency(3);
        sut.afterPropertiesSet();
        sut.doStart();

        verify(listenerContainer, times(3)).start();
    }

    @Test
    void shouldStopAllListenerContainers() {
        SqsInboundChannelAdapter sut = new SqsInboundChannelAdapter(amazonSQS, "test1");
        ReflectionTestUtils.setField(sut, "simpleMessageListenerContainerFactory", listenerContainerFactory);
        when(listenerContainerFactory.createSimpleMessageListenerContainer()).thenReturn(listenerContainer);

        sut.setConcurrency(3);
        sut.afterPropertiesSet();
        sut.doStop();

        verify(listenerContainer, times(3)).stop();
    }

    @Test
    void shouldDestroyAllListenerContainers() {
        SqsInboundChannelAdapter sut = new SqsInboundChannelAdapter(amazonSQS, "test1");
        ReflectionTestUtils.setField(sut, "simpleMessageListenerContainerFactory", listenerContainerFactory);
        when(listenerContainerFactory.createSimpleMessageListenerContainer()).thenReturn(listenerContainer);

        sut.setConcurrency(3);
        sut.afterPropertiesSet();
        sut.destroy();

        verify(listenerContainer, times(3)).destroy();
    }

    @Test
    void shouldReturnTrueIfAnyContainerIsRunning() {
        SqsInboundChannelAdapter sut = new SqsInboundChannelAdapter(amazonSQS, "test1");
        ReflectionTestUtils.setField(sut, "simpleMessageListenerContainerFactory", listenerContainerFactory);
        when(listenerContainerFactory.createSimpleMessageListenerContainer()).thenReturn(listenerContainer);

        when(listenerContainer.isRunning("test1")).thenReturn(false, true);

        sut.setConcurrency(2);
        sut.afterPropertiesSet();

        assertThat(sut.isRunning("test1")).isTrue();
        verify(listenerContainer, times(2)).isRunning("test1");
    }

    @Test
    void shouldReturnFalseIfNoContainerIsRunning() {
        SqsInboundChannelAdapter sut = new SqsInboundChannelAdapter(amazonSQS, "test1");
        ReflectionTestUtils.setField(sut, "simpleMessageListenerContainerFactory", listenerContainerFactory);
        when(listenerContainerFactory.createSimpleMessageListenerContainer()).thenReturn(listenerContainer);

        when(listenerContainer.isRunning("test1")).thenReturn(false);

        sut.setConcurrency(2);
        sut.afterPropertiesSet();

        assertThat(sut.isRunning("test1")).isFalse();
        verify(listenerContainer, times(2)).isRunning("test1");
    }

    @Test
    void shouldReturnQueues() {
        SqsInboundChannelAdapter sut = new SqsInboundChannelAdapter(amazonSQS, "test1", "test2");

        assertThat(sut.getQueues()).containsExactly("test1", "test2");
    }

    @Test
    void shouldSetTaskExecutor(@Mock AsyncTaskExecutor taskExecutor) {
        SqsInboundChannelAdapter sut = new SqsInboundChannelAdapter(amazonSQS, "test1");
        ReflectionTestUtils.setField(sut, "simpleMessageListenerContainerFactory", listenerContainerFactory);

        sut.setTaskExecutor(taskExecutor);

        verify(listenerContainerFactory).setTaskExecutor(taskExecutor);
    }

    @Test
    void shouldSetMaxNumberOfMessages() {
        SqsInboundChannelAdapter sut = new SqsInboundChannelAdapter(amazonSQS, "test1");
        ReflectionTestUtils.setField(sut, "simpleMessageListenerContainerFactory", listenerContainerFactory);

        sut.setMaxNumberOfMessages(5);

        verify(listenerContainerFactory).setMaxNumberOfMessages(5);
    }

    @Test
    void shouldSetVisibilityTimeout() {
        SqsInboundChannelAdapter sut = new SqsInboundChannelAdapter(amazonSQS, "test1");
        ReflectionTestUtils.setField(sut, "simpleMessageListenerContainerFactory", listenerContainerFactory);

        sut.setVisibilityTimeout(180);

        verify(listenerContainerFactory).setVisibilityTimeout(180);
    }

    @Test
    void shouldSetWaitTimeout() {
        SqsInboundChannelAdapter sut = new SqsInboundChannelAdapter(amazonSQS, "test1");
        ReflectionTestUtils.setField(sut, "simpleMessageListenerContainerFactory", listenerContainerFactory);

        sut.setWaitTimeOut(180);

        verify(listenerContainerFactory).setWaitTimeOut(180);
    }

    @Test
    void shouldSetResourceIdResolver(@Mock ResourceIdResolver resourceIdResolver) {
        SqsInboundChannelAdapter sut = new SqsInboundChannelAdapter(amazonSQS, "test1");
        ReflectionTestUtils.setField(sut, "simpleMessageListenerContainerFactory", listenerContainerFactory);

        sut.setResourceIdResolver(resourceIdResolver);

        verify(listenerContainerFactory).setResourceIdResolver(resourceIdResolver);
    }

    @Test
    void shouldSetAutoStartup() {
        SqsInboundChannelAdapter sut = new SqsInboundChannelAdapter(amazonSQS, "test1");
        ReflectionTestUtils.setField(sut, "simpleMessageListenerContainerFactory", listenerContainerFactory);

        sut.setAutoStartup(true);

        verify(listenerContainerFactory).setAutoStartup(true);
    }

    @Test
    void shouldSetDestinationResolver(@Mock DestinationResolver<String> destinationResolver) {
        SqsInboundChannelAdapter sut = new SqsInboundChannelAdapter(amazonSQS, "test1");
        ReflectionTestUtils.setField(sut, "simpleMessageListenerContainerFactory", listenerContainerFactory);

        sut.setDestinationResolver(destinationResolver);

        verify(listenerContainerFactory).setDestinationResolver(destinationResolver);
    }

    @Test
    void shouldSetQueueStopTimeout() {
        SqsInboundChannelAdapter sut = new SqsInboundChannelAdapter(amazonSQS, "test1");
        ReflectionTestUtils.setField(sut, "simpleMessageListenerContainerFactory", listenerContainerFactory);
        when(listenerContainerFactory.createSimpleMessageListenerContainer()).thenReturn(listenerContainer);

        sut.setQueueStopTimeout(120L);
        sut.afterPropertiesSet();

        verify(listenerContainer).setQueueStopTimeout(120L);
    }

}