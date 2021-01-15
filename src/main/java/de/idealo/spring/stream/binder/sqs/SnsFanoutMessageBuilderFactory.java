package de.idealo.spring.stream.binder.sqs;

import org.springframework.integration.support.DefaultMessageBuilderFactory;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.converter.MessageConversionException;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class SnsFanoutMessageBuilderFactory extends DefaultMessageBuilderFactory {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    @SuppressWarnings("unchecked")
    public <T> MessageBuilder<T> fromMessage(Message<T> message) {
        JsonNode jsonNode;
        try {
            jsonNode = this.objectMapper.readTree((String) message.getPayload());
        } catch (JsonProcessingException e) {
            throw new MessagingException(message, e);
        }

        if (!jsonNode.has("Type")) {
            throw new MessageConversionException("Payload: '" + message.getPayload()
                    + "' does not contain a Type attribute", null);
        }

        if (!"Notification".equals(jsonNode.get("Type").asText())) {
            throw new MessageConversionException(
                    "Payload: '" + message.getPayload() + "' is not a valid notification",
                    null);
        }

        if (!jsonNode.has("Message")) {
            throw new MessageConversionException(
                    "Payload: '" + message.getPayload() + "' does not contain a message",
                    null);
        }

        String messagePayload = jsonNode.get("Message").asText();

        return (MessageBuilder<T>) MessageBuilder.withPayload(messagePayload)
                .copyHeaders(message.getHeaders());
    }
}
