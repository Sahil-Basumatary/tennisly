package dev.sahilbasumatary.common.event;

public class TennisDataEvent extends BaseEvent {

    public static final String PLAYERS_SYNCED = "TENNIS_PLAYERS_SYNCED";
    public static final String TOURNAMENTS_SYNCED = "TENNIS_TOURNAMENTS_SYNCED";
    public static final String RANKINGS_SYNCED = "TENNIS_RANKINGS_SYNCED";

    private String resourceType;
    private int recordsProcessed;
    private String provider;
    private boolean successful;

    public TennisDataEvent() {}

    public TennisDataEvent(String eventType, String resourceType) {
        super(eventType, "tennis-data-service");
        this.resourceType = resourceType;
    }

    public static TennisDataEvent playersSynced(int recordsProcessed, String provider) {
        return synced(PLAYERS_SYNCED, "players", recordsProcessed, provider);
    }

    public static TennisDataEvent tournamentsSynced(int recordsProcessed, String provider) {
        return synced(TOURNAMENTS_SYNCED, "tournaments", recordsProcessed, provider);
    }

    public static TennisDataEvent rankingsSynced(int recordsProcessed, String provider) {
        return synced(RANKINGS_SYNCED, "rankings", recordsProcessed, provider);
    }

    private static TennisDataEvent synced(
            String eventType, String resourceType, int recordsProcessed, String provider) {
        TennisDataEvent event = new TennisDataEvent(eventType, resourceType);
        event.recordsProcessed = recordsProcessed;
        event.provider = provider;
        event.successful = true;
        return event;
    }

    public String getResourceType() {
        return resourceType;
    }

    public void setResourceType(String resourceType) {
        this.resourceType = resourceType;
    }

    public int getRecordsProcessed() {
        return recordsProcessed;
    }

    public void setRecordsProcessed(int recordsProcessed) {
        this.recordsProcessed = recordsProcessed;
    }

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public boolean isSuccessful() {
        return successful;
    }

    public void setSuccessful(boolean successful) {
        this.successful = successful;
    }
}
