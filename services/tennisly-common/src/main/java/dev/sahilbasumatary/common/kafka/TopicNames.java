package dev.sahilbasumatary.common.kafka;

public final class TopicNames {

    private TopicNames() {}

    public static final String USER_EVENTS = "tennisly.user.events";
    public static final String ORGANIZATION_EVENTS = "tennisly.organization.events";
    public static final String TENNIS_DATA_EVENTS = "tennisly.tennis-data.events";
    public static final String MATCH_EVENTS = "tennisly.match.events";

    public static final String USER_EVENTS_DLQ = "tennisly.user.events.dlq";
    public static final String ORGANIZATION_EVENTS_DLQ = "tennisly.organization.events.dlq";
    public static final String TENNIS_DATA_EVENTS_DLQ = "tennisly.tennis-data.events.dlq";
    public static final String MATCH_EVENTS_DLQ = "tennisly.match.events.dlq";
}
