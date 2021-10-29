package de.idealo.spring.stream.binder.sqs;

public abstract class SqsHeaders {

    private SqsHeaders() {}

    public static final String PREFIX = "sqs_";

    public static final String GROUP_ID = PREFIX + "groupId";

    public static final String DEDUPLICATION_ID = PREFIX + "deduplicationId";

}
