package de.idealo.spring.stream.binder.sqs.inbound;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import io.awspring.cloud.sqs.config.SqsMessageListenerContainerFactory;
import io.awspring.cloud.sqs.listener.SqsMessageListenerContainer;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;

@ExtendWith(MockitoExtension.class)
class SqsInboundChannelAdapterTest {
    @Mock
    private SqsAsyncClient amazonSQS;

    @Mock
    private SqsMessageListenerContainerFactory.Builder<Object> listenerContainerFactoryBuilder;

    @Mock
    private SqsMessageListenerContainerFactory listenerContainerFactory;

    @Mock
    private SqsMessageListenerContainer listenerContainer;

    @Test
    void shouldDefaultToSingleListenerContainer() {
        SqsInboundChannelAdapter sut = new SqsInboundChannelAdapter(amazonSQS, "test1");
        ReflectionTestUtils.setField(sut, "sqsMessageListenerContainerFactory", listenerContainerFactoryBuilder);
        when(listenerContainerFactoryBuilder.build()).thenReturn(listenerContainerFactory);
        when(listenerContainerFactory.createContainer("test1")).thenReturn(listenerContainer);

        sut.afterPropertiesSet();

        verify(listenerContainerFactory, times(1)).createContainer(eq("test1"));
    }

    @Test
    void shouldCreateMultipleListenerContainers() {
        SqsInboundChannelAdapter sut = new SqsInboundChannelAdapter(amazonSQS, "test1");
        ReflectionTestUtils.setField(sut, "sqsMessageListenerContainerFactory", listenerContainerFactoryBuilder);
        when(listenerContainerFactoryBuilder.build()).thenReturn(listenerContainerFactory);
        when(listenerContainerFactory.createContainer("test1")).thenReturn(listenerContainer);

        sut.setConcurrency(3);
        sut.afterPropertiesSet();

        verify(listenerContainerFactory, times(3)).createContainer(eq("test1"));
    }

    @Test
    void shouldStartAllListenerContainers() {
        SqsInboundChannelAdapter sut = new SqsInboundChannelAdapter(amazonSQS, "test1");
        ReflectionTestUtils.setField(sut, "sqsMessageListenerContainerFactory", listenerContainerFactoryBuilder);
        when(listenerContainerFactoryBuilder.build()).thenReturn(listenerContainerFactory);
        when(listenerContainerFactory.createContainer("test1")).thenReturn(listenerContainer);

        sut.setConcurrency(3);
        sut.afterPropertiesSet();
        sut.doStart();

        verify(listenerContainer, times(3)).start();
    }

    @Test
    void shouldStopAllListenerContainers() {
        SqsInboundChannelAdapter sut = new SqsInboundChannelAdapter(amazonSQS, "test1");
        ReflectionTestUtils.setField(sut, "sqsMessageListenerContainerFactory", listenerContainerFactoryBuilder);
        when(listenerContainerFactoryBuilder.build()).thenReturn(listenerContainerFactory);
        when(listenerContainerFactory.createContainer("test1")).thenReturn(listenerContainer);

        sut.setConcurrency(3);
        sut.afterPropertiesSet();
        sut.doStop();

        verify(listenerContainer, times(3)).stop();
    }


    @Test
    void shouldReturnTrueIfAnyContainerIsRunning() {
        SqsInboundChannelAdapter sut = new SqsInboundChannelAdapter(amazonSQS, "test1");
        ReflectionTestUtils.setField(sut, "sqsMessageListenerContainerFactory", listenerContainerFactoryBuilder);
        when(listenerContainerFactoryBuilder.build()).thenReturn(listenerContainerFactory);
        when(listenerContainerFactory.createContainer("test1")).thenReturn(listenerContainer);

        when(listenerContainer.getQueueNames()).thenReturn(List.of("test1"));
        when(listenerContainer.isRunning()).thenReturn(false, true);

        sut.setConcurrency(2);
        sut.afterPropertiesSet();

        assertThat(sut.isRunning("test1")).isTrue();
        verify(listenerContainer, times(2)).isRunning();
    }

    @Test
    void shouldReturnFalseIfNoContainerIsRunning() {
        SqsInboundChannelAdapter sut = new SqsInboundChannelAdapter(amazonSQS, "test1");
        ReflectionTestUtils.setField(sut, "sqsMessageListenerContainerFactory", listenerContainerFactoryBuilder);
        when(listenerContainerFactoryBuilder.build()).thenReturn(listenerContainerFactory);
        when(listenerContainerFactory.createContainer("test1")).thenReturn(listenerContainer);

        when(listenerContainer.getQueueNames()).thenReturn(List.of("test1"));
        when(listenerContainer.isRunning()).thenReturn(false);

        sut.setConcurrency(2);
        sut.afterPropertiesSet();

        assertThat(sut.isRunning("test1")).isFalse();
        verify(listenerContainer, times(2)).isRunning();
    }

    @Test
    void shouldReturnQueues() {
        SqsInboundChannelAdapter sut = new SqsInboundChannelAdapter(amazonSQS, "test1", "test2");

        assertThat(sut.getQueues()).containsExactly("test1", "test2");
    }

}