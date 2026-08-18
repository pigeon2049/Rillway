package com.wegongdu.rillway.audit.sink;

import com.wegongdu.rillway.audit.event.AuditEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Thread-safe in-memory sink for testing and lightweight auditing.
 */
public class InMemoryAuditSink implements AuditSink {

    private final List<AuditEvent> events = new CopyOnWriteArrayList<>();

    @Override
    public void publish(AuditEvent event) {
        if (event != null) {
            events.add(event);
        }
    }

    public List<AuditEvent> getEvents() {
        return Collections.unmodifiableList(events);
    }

    public List<AuditEvent> getEventsForInstance(String processInstanceId) {
        if (processInstanceId == null) {
            return Collections.emptyList();
        }
        return events.stream()
                .filter(e -> processInstanceId.equals(e.processInstanceId()))
                .toList();
    }

    @SuppressWarnings("unchecked")
    public <T extends AuditEvent> List<T> getEventsOfType(Class<T> eventClass) {
        return events.stream()
                .filter(eventClass::isInstance)
                .map(e -> (T) e)
                .toList();
    }

    public void clear() {
        events.clear();
    }

    public int size() {
        return events.size();
    }
}
