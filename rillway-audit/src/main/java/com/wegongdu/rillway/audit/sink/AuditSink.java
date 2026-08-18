package com.wegongdu.rillway.audit.sink;

import com.wegongdu.rillway.audit.event.AuditEvent;

/**
 * Sink interface for publishing audit events.
 */
public interface AuditSink {

    void publish(AuditEvent event);
}
