package com.wegongdu.rillway.audit.event;

import java.io.Serializable;
import java.time.Instant;

/**
 * Marker interface for all structured audit events in Rillway.
 */
public interface AuditEvent extends Serializable {

    String eventId();

    String processInstanceId();

    String definitionId();

    Instant timestamp();

    String eventType();
}
