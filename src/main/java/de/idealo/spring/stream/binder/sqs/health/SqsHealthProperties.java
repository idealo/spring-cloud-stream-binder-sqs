package de.idealo.spring.stream.binder.sqs.health;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("health.sqs")
public class SqsHealthProperties {

    /**
     * Queues names to check separated by a comma.
     */
    private String[] queueNames;

    public String[] getQueueNames() {
        return queueNames;
    }

    public void setQueueNames(final String[] queueNames) {
        this.queueNames = queueNames;
    }
}
