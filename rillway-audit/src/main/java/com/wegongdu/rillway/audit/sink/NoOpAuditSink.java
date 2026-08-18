package com.wegongdu.rillway.audit.sink;

import com.wegongdu.rillway.audit.event.AuditEvent;

/**
 * No-op implementation of AuditSink.
 */
public class NoOpAuditSink implements AuditSink {

    public static final NoOpAuditSink INSTANCE = new NoOpAuditSink();

    @Override
    public void publish(AuditEvent event) {
        // Discard
    }
}
