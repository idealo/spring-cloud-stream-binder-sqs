# Spring Cloud Stream Binder for AWS SQS

spring-cloud-stream-binder-sns lets you use [Spring Cloud Stream](https://spring.io/projects/spring-cloud-stream) with the AWS Simple Queue Service (SQS). Currently it only supports consuming from SQS queues to your service, producing will be added later.

## Installation

```xml
<dependencies>
    <dependency>
        <groupId>de.idealo.spring</groupId>
        <artifactId>spring-cloud-stream-binder-sqs</artifactId>
        <version>1.0.0</version>
    </dependency>
</dependencies>
```

## Usage

With the library in your dependencies you can configure your Spring Cloud Stream bindings as usual. The type name for this binder is `sqs`. The destination needs to match the queue name, the specific ARN will be looked up from the available queue in the account.

You may also provide additional configuration options:

- **maxNumberOfMessages** - Maximum number of messages to retrieve with one poll to SQS. Must be a number between 1 and 10.
- **visibilityTimeout** - The duration in seconds that polled messages are hidden from subsequent poll requests after having been retrieved.
- **waitTimeout** - The duration in seconds that the system will wait for new messages to arrive when polling. Uses the Amazon SQS long polling feature. The value should be between 1 and 20.
- **messageDeletionPolicy** - The deletion policy for messages that are retrieved from SQS. Defaults to NO_REDRIVE.
- **snsFanout** - Whether the incoming message has the SNS format and should be deserialized automatically. Defaults to false.

**Example Configuration:**

```yaml
spring:
  cloud:
    stream:
      sqs:
        bindings:
          someFunction-in-0:
            consumer:
              snsFanout: true
      bindings:
        someFunction-in-0:
          destination: queue-name
```

You may also provide your own beans of `AmazonSQSAsync` to override those that are created by [spring-cloud-aws-autoconfigure](https://github.com/spring-cloud/spring-cloud-aws/tree/master/spring-cloud-aws-autoconfigure).
