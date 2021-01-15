package de.idealo.spring.stream.binder.sqs.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.stream.binder.AbstractExtendedBindingProperties;
import org.springframework.cloud.stream.binder.BinderSpecificPropertiesProvider;

@ConfigurationProperties("spring.cloud.stream.sqs")
public class SqsExtendedBindingProperties extends AbstractExtendedBindingProperties<SqsConsumerProperties, SqsProducerProperties, SqsBindingProperties> {

    private static final String DEFAULTS_PREFIX = "spring.cloud.stream.sqs.default";

    @Override
    public String getDefaultsPrefix() {
        return DEFAULTS_PREFIX;
    }

    @Override
    public Class<? extends BinderSpecificPropertiesProvider> getExtendedPropertiesEntryClass() {
        return SqsBindingProperties.class;
    }
}
