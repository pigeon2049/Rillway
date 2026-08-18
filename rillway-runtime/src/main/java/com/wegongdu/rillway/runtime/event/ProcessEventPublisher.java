package com.wegongdu.rillway.runtime.event;

import com.wegongdu.rillway.core.event.ProcessEvent;
import com.wegongdu.rillway.core.event.ProcessEventListener;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Publisher SPI for dispatching workflow lifecycle events to listeners.
 */
public interface ProcessEventPublisher {

    void publish(ProcessEvent event);

    /**
     * No-op publisher instance.
     */
    ProcessEventPublisher NOOP = event -> {};

    /**
     * Creates a composite publisher delegating to multiple listeners.
     */
    static ProcessEventPublisher composite(List<ProcessEventListener> listeners) {
        if (listeners == null || listeners.isEmpty()) {
            return NOOP;
        }
        return new CompositeProcessEventPublisher(listeners);
    }
}
