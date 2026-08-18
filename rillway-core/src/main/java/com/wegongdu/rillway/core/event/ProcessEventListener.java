package com.wegongdu.rillway.core.event;

/**
 * Listener interface for observing workflow process lifecycle events.
 */
@FunctionalInterface
public interface ProcessEventListener {

    void onEvent(ProcessEvent event);

    default void onProcessStarted(ProcessEvent.ProcessStartedEvent event) {
        onEvent(event);
    }

    default void onNodeEntered(ProcessEvent.NodeEnteredEvent event) {
        onEvent(event);
    }

    default void onNodeCompleted(ProcessEvent.NodeCompletedEvent event) {
        onEvent(event);
    }

    default void onProcessCompleted(ProcessEvent.ProcessCompletedEvent event) {
        onEvent(event);
    }

    default void onProcessFailed(ProcessEvent.ProcessFailedEvent event) {
        onEvent(event);
    }
}
