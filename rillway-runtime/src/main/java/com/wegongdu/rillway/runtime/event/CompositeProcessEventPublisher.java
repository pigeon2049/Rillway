package com.wegongdu.rillway.runtime.event;

import com.wegongdu.rillway.core.event.ProcessEvent;
import com.wegongdu.rillway.core.event.ProcessEventListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Composite dispatcher that fans out events to registered ProcessEventListeners.
 */
public class CompositeProcessEventPublisher implements ProcessEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(CompositeProcessEventPublisher.class);

    private final List<ProcessEventListener> listeners = new CopyOnWriteArrayList<>();

    public CompositeProcessEventPublisher(List<ProcessEventListener> initialListeners) {
        if (initialListeners != null) {
            this.listeners.addAll(initialListeners);
        }
    }

    public void addListener(ProcessEventListener listener) {
        if (listener != null) {
            this.listeners.add(listener);
        }
    }

    public void removeListener(ProcessEventListener listener) {
        if (listener != null) {
            this.listeners.remove(listener);
        }
    }

    public List<ProcessEventListener> getListeners() {
        return List.copyOf(listeners);
    }

    @Override
    public void publish(ProcessEvent event) {
        if (event == null) {
            return;
        }

        for (ProcessEventListener listener : listeners) {
            try {
                if (event instanceof ProcessEvent.ProcessStartedEvent started) {
                    listener.onProcessStarted(started);
                } else if (event instanceof ProcessEvent.NodeEnteredEvent entered) {
                    listener.onNodeEntered(entered);
                } else if (event instanceof ProcessEvent.NodeCompletedEvent completed) {
                    listener.onNodeCompleted(completed);
                } else if (event instanceof ProcessEvent.ProcessCompletedEvent completed) {
                    listener.onProcessCompleted(completed);
                } else if (event instanceof ProcessEvent.ProcessFailedEvent failed) {
                    listener.onProcessFailed(failed);
                } else {
                    listener.onEvent(event);
                }
            } catch (Exception ex) {
                log.error("Error executing ProcessEventListener [{}] on event [{}]: {}",
                        listener.getClass().getName(), event.getClass().getSimpleName(), ex.getMessage(), ex);
            }
        }
    }
}
