package de.idealo.spring.stream.binder.sqs;

public final class SqsHeaders {

    public static final String PREFIX = "sqs_";

    public static final String DELAY = PREFIX + "delay";

    public static final String GROUP_ID = PREFIX + "groupId";

    public static final String DEDUPLICATION_ID = PREFIX + "deduplicationId";

    private SqsHeaders() {}

}
