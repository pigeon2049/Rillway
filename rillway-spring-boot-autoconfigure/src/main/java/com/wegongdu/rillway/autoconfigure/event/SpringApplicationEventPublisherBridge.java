package com.wegongdu.rillway.autoconfigure.event;

import com.wegongdu.rillway.core.event.ProcessEvent;
import com.wegongdu.rillway.core.event.ProcessEventListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;

/**
 * Bridges Rillway core process lifecycle events to Spring's ApplicationEventPublisher,
 * allowing standard Spring @EventListener methods to receive workflow events.
 */
public class SpringApplicationEventPublisherBridge implements ProcessEventListener {

    private static final Logger log = LoggerFactory.getLogger(SpringApplicationEventPublisherBridge.class);

    private final ApplicationEventPublisher applicationEventPublisher;

    public SpringApplicationEventPublisherBridge(ApplicationEventPublisher applicationEventPublisher) {
        this.applicationEventPublisher = applicationEventPublisher;
    }

    @Override
    public void onEvent(ProcessEvent event) {
        if (applicationEventPublisher != null && event != null) {
            try {
                applicationEventPublisher.publishEvent(event);
            } catch (Exception ex) {
                log.error("Error publishing process event [{}] to Spring ApplicationEventPublisher: {}",
                        event.getClass().getSimpleName(), ex.getMessage(), ex);
            }
        }
    }
}
