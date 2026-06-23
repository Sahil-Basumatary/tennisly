package dev.sahilbasumatary.matchservice.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import dev.sahilbasumatary.matchservice.entity.MatchStatus;
import dev.sahilbasumatary.matchservice.exception.InvalidMatchStateException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class MatchStateMachineTest {

    private MatchStateMachine stateMachine;

    @BeforeEach
    void setUp() {
        stateMachine = new MatchStateMachine();
    }

    @Test
    void allowsNormalLiveMatchLifecycle() {
        assertDoesNotThrow(
                () ->
                        stateMachine.assertCanTransition(
                                MatchStatus.SCHEDULED, MatchStatus.IN_PROGRESS));
        assertDoesNotThrow(
                () ->
                        stateMachine.assertCanTransition(
                                MatchStatus.IN_PROGRESS, MatchStatus.SUSPENDED));
        assertDoesNotThrow(
                () ->
                        stateMachine.assertCanTransition(
                                MatchStatus.SUSPENDED, MatchStatus.IN_PROGRESS));
        assertDoesNotThrow(
                () ->
                        stateMachine.assertCanTransition(
                                MatchStatus.IN_PROGRESS, MatchStatus.COMPLETED));
    }

    @Test
    void blocksTerminalMatchReopening() {
        assertThrows(
                InvalidMatchStateException.class,
                () ->
                        stateMachine.assertCanTransition(
                                MatchStatus.COMPLETED, MatchStatus.IN_PROGRESS));
        assertThrows(
                InvalidMatchStateException.class,
                () ->
                        stateMachine.assertCanTransition(
                                MatchStatus.CANCELLED, MatchStatus.IN_PROGRESS));
    }

    @Test
    void recordsPointsOnlyDuringLiveMatch() {
        assertDoesNotThrow(() -> stateMachine.assertCanRecordPoint(MatchStatus.IN_PROGRESS));
        assertThrows(
                InvalidMatchStateException.class,
                () -> stateMachine.assertCanRecordPoint(MatchStatus.SCHEDULED));
        assertThrows(
                InvalidMatchStateException.class,
                () -> stateMachine.assertCanRecordPoint(MatchStatus.COMPLETED));
    }
}
