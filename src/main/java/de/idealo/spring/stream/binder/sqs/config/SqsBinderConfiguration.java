package de.idealo.spring.stream.binder.sqs.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.stream.binder.Binder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.amazonaws.services.sqs.AmazonSQSAsync;
import io.awspring.cloud.messaging.listener.SimpleMessageListenerContainer;

import de.idealo.spring.stream.binder.sqs.SqsMessageHandlerBinder;
import de.idealo.spring.stream.binder.sqs.health.SqsHealthIndicator;
import de.idealo.spring.stream.binder.sqs.health.SqsHealthProperties;
import de.idealo.spring.stream.binder.sqs.properties.SqsExtendedBindingProperties;
import de.idealo.spring.stream.binder.sqs.provisioning.SqsStreamProvisioner;

@Configuration
@ConditionalOnMissingBean(Binder.class)
@EnableConfigurationProperties({ SqsExtendedBindingProperties.class, SqsHealthProperties.class })
public class SqsBinderConfiguration {

    @Bean
    public SqsStreamProvisioner provisioningProvider() {
        return new SqsStreamProvisioner();
    }

    @Bean
    public SqsMessageHandlerBinder sqsMessageHandlerBinder(AmazonSQSAsync amazonSQS, SqsStreamProvisioner sqsStreamProvisioner, SqsExtendedBindingProperties extendedBindingProperties) {
        return new SqsMessageHandlerBinder(amazonSQS, sqsStreamProvisioner, extendedBindingProperties);
    }

    @Bean
    @ConditionalOnMissingBean(SqsHealthIndicator.class)
    @ConditionalOnClass(name = "org.springframework.boot.actuate.health.HealthIndicator")
    public SqsHealthIndicator sqsHealthIndicator(SimpleMessageListenerContainer container, AmazonSQSAsync amazonSQS, SqsHealthProperties sqsHealthProperties) {

        return new SqsHealthIndicator(container, amazonSQS, sqsHealthProperties);
    }
}
