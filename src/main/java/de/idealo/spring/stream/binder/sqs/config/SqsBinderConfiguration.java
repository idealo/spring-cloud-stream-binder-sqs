package de.idealo.spring.stream.binder.sqs.config;

import org.springframework.boot.actuate.autoconfigure.health.ConditionalOnEnabledHealthIndicator;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.stream.binder.Binder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.amazonaws.services.sqs.AmazonSQSAsync;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;

import de.idealo.spring.stream.binder.sqs.SqsMessageHandlerBinder;
import de.idealo.spring.stream.binder.sqs.health.SqsBinderHealthIndicator;
import de.idealo.spring.stream.binder.sqs.properties.SqsExtendedBindingProperties;
import de.idealo.spring.stream.binder.sqs.provisioning.SqsStreamProvisioner;

@Configuration
@ConditionalOnMissingBean(Binder.class)
@EnableConfigurationProperties({ SqsExtendedBindingProperties.class })
public class SqsBinderConfiguration {

    @Bean
    public SqsStreamProvisioner provisioningProvider() {
        return new SqsStreamProvisioner();
    }

    @Bean
    public SqsMessageHandlerBinder sqsMessageHandlerBinder(SqsAsyncClient sqsAsyncClient, SqsStreamProvisioner sqsStreamProvisioner, SqsExtendedBindingProperties extendedBindingProperties) {
        return new SqsMessageHandlerBinder(sqsAsyncClient, sqsStreamProvisioner, extendedBindingProperties);
    }

    @Configuration
    @ConditionalOnClass(HealthIndicator.class)
    @ConditionalOnEnabledHealthIndicator("binders")
    protected static class SqsBinderHealthIndicatorConfiguration {

        @Bean
        @ConditionalOnMissingBean(name = "sqsBinderHealthIndicator")
        public SqsBinderHealthIndicator sqsBinderHealthIndicator(SqsMessageHandlerBinder sqsMessageHandlerBinder) {
            return new SqsBinderHealthIndicator(sqsMessageHandlerBinder);
        }

    }
}
