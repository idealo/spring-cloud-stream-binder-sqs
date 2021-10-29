# Spring Cloud Stream Binder for AWS SQS

spring-cloud-stream-binder-sqs lets you use [Spring Cloud Stream](https://spring.io/projects/spring-cloud-stream) with the AWS Simple Queue Service (SQS).

## Installation

```xml
<dependencies>
    <dependency>
        <groupId>de.idealo.spring</groupId>
        <artifactId>spring-cloud-stream-binder-sqs</artifactId>
        <version>1.5.0</version>
    </dependency>
</dependencies>
```

## Usage

With the library in your dependencies you can configure your Spring Cloud Stream bindings as usual. The type name for this binder is `sqs`. The destination needs to match the queue name, the specific ARN will be looked up from the available queue in the account.

You may also provide additional configuration options:

- **Consumers**
  - **maxNumberOfMessages** - Maximum number of messages to retrieve with one poll to SQS. Must be a number between 1 and 10.
  - **visibilityTimeout** - The duration in seconds that polled messages are hidden from subsequent poll requests after having been retrieved.
  - **waitTimeout** - The duration in seconds that the system will wait for new messages to arrive when polling. Uses the Amazon SQS long polling feature. The value should be between 1 and 20.
  - **messageDeletionPolicy** - The deletion policy for messages that are retrieved from SQS. Defaults to NO_REDRIVE.
  - **snsFanout** - Whether the incoming message has the SNS format and should be deserialized automatically. Defaults to true.

**Example Configuration:**

```yaml
spring:
  cloud:
    stream:
      sqs:
        bindings:
          someFunction-in-0:
            consumer:
              snsFanout: false
      bindings:
        someFunction-in-0:
          destination: input-queue-name
        someFunction-out-0:
          destination: output-queue-name
```

You may also provide your own beans of `AmazonSQSAsync` to override those that are created by [spring-cloud-aws-autoconfigure](https://github.com/spring-cloud/spring-cloud-aws/tree/master/spring-cloud-aws-autoconfigure).

### FIFO queues

To use [FIFO SQS queues](https://docs.aws.amazon.com/AWSSimpleQueueService/latest/SQSDeveloperGuide/FIFO-queues.html) you will need to provide a deduplication id and a group id.
With this binder you may set these using the message headers `SqsHeaders.GROUP_ID` and `SqsHeaders.DEDUPLICATION_ID`.
The example below shows how you could use a FIFO queue in real life.

**Example Configuration:**

```yaml
spring:
  cloud:
    stream:
      bindings:
        someFunction-in-0:
          destination: input-queue-name
        someFunction-out-0:
          destination: output-queue-name.fifo
```

```java
class Application {
  @Bean
  public Message<String> someFunction(String input) {
    return MessageBuilder.withPayload(input)
            .setHeader(SqsHeaders.GROUP_ID, "my-application")
            .setHeader(SqsHeaders.DEDUPLICATION_ID, UUID.randomUUID())
            .build();
  }
}
```
