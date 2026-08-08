package dev.sahilbasumatary.analyticsservice.domain;

public final class ReindexJobStatus {

    public static final String PENDING = "PENDING";
    public static final String RUNNING = "RUNNING";
    public static final String COMPLETED = "COMPLETED";
    public static final String FAILED = "FAILED";

    private ReindexJobStatus() {}
}
