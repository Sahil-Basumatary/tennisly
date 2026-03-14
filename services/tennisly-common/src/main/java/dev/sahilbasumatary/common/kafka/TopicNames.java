package dev.sahilbasumatary.common.kafka;

public final class TopicNames {

    private TopicNames() {}

    public static final String USER_EVENTS = "tennisly.user.events";
    public static final String ORGANIZATION_EVENTS = "tennisly.organization.events";

    public static final String USER_EVENTS_DLQ = "tennisly.user.events.dlq";
    public static final String ORGANIZATION_EVENTS_DLQ = "tennisly.organization.events.dlq";
}
