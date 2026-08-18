package com.wegongdu.rillway.core.model;

/**
 * Lifecycle status of a process instance.
 */
public enum ProcessStatus {
    CREATED,
    RUNNING,
    WAITING_FOR_DECISION,
    COMPLETED,
    REJECTED,
    FAILED,
    TERMINATED
}
