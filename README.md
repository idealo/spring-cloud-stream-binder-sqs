# Spring Cloud Stream Binder for AWS SQS

spring-cloud-stream-binder-sqs lets you use [Spring Cloud Stream](https://spring.io/projects/spring-cloud-stream) with
the AWS Simple Queue Service (SQS).

## Installation

```xml

<dependencies>
    <dependency>
        <groupId>de.idealo.spring</groupId>
        <artifactId>spring-cloud-stream-binder-sqs</artifactId>
        <version>1.9.0</version>
    </dependency>
</dependencies>
```

## Compatabilty

| spring-cloud-stream-binder-sqs | spring-boot | spring-cloud-aws | spring.cloud-version | aws sdk | java compiler/runtime |
|--------------------------------|-------------|------------------|----------------------|---------|-----------------------|
| 1.9.0                          | 2.7.x       | 2.4.x            | 2021.0.5             | 1.x     | 8                     |
| 3.0.0                          | 3.1.x       | 3.0.x            | 2022.0.3             | 2.x     | 17                    |

Changes in 3.0:

* removed consumer configuration for **messageDeletionPolicy**: the default behaviour is now that Messages will be
  acknowledged when message processing is successful.

## Usage

With the library in your dependencies you can configure your Spring Cloud Stream bindings as usual. The type name for
this binder is `sqs`. The destination needs to match the queue name, the specific ARN will be looked up from the
available queue in the account.

You may also provide additional configuration options:

- **Consumers**
    - **maxNumberOfMessages** - Maximum number of messages to retrieve with one poll to SQS. Must be a number between 1
      and 10.
    - **visibilityTimeout** - The duration in seconds that polled messages are hidden from subsequent poll requests
      after having been retrieved.
    - **waitTimeout** - The duration in seconds that the system will wait for new messages to arrive when polling. Uses
      the Amazon SQS long polling feature. The value should be between 1 and 20.
    - **queueStopTimeout** - The number of milliseconds that the queue worker is given to gracefully finish its work on
      shutdown before interrupting the current thread. Default value is 10 seconds.
    - **snsFanout** - Whether the incoming message has the SNS format and should be deserialized automatically. Defaults
      to true.

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

You may also provide your own beans of `SqsAsyncClient` to override those that are created
by [spring-cloud-aws-autoconfigure](https://github.com/spring-cloud/spring-cloud-aws/tree/master/spring-cloud-aws-autoconfigure).

### FIFO queues

To use [FIFO SQS queues](https://docs.aws.amazon.com/AWSSimpleQueueService/latest/SQSDeveloperGuide/FIFO-queues.html)
you will need to provide a deduplication id and a group id.
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

### Concurrency

Consumers in the SQS binder support the Spring Cloud Stream `concurrency` property.
By specifying a value you will launch `concurrency`  threads continuously polling for `maxNumberOfMessages` each.
The threads will process all messages asynchronously, but each thread will wait for its current batch of messages to all
complete processing before retrieving new ones.
If your message processing is highly variable from message to message it is recommended to set a lower value
for `maxNumberOfMessages` and a higher value for `concurrency`.
Note that this will increase the amount of API calls done against SQS.

**Example Configuration:**

```yaml
spring:
  cloud:
    stream:
      sqs:
        bindings:
          someFunction-in-0:
            consumer:
              maxNumberOfMessages: 5
      bindings:
        someFunction-in-0:
          destination: input-queue-name
          consumer:
            concurrency: 10
```
