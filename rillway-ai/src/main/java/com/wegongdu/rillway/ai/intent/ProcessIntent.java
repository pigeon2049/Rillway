package com.wegongdu.rillway.ai.intent;

import com.wegongdu.rillway.core.context.ProcessContext;
import java.io.Serializable;
import java.util.Objects;

/**
 * Natural language statement of business intent.
 */
public record ProcessIntent(
        String naturalLanguage,
        String initiator,
        ProcessContext exampleContext
) implements Serializable {

    public ProcessIntent {
        Objects.requireNonNull(naturalLanguage, "naturalLanguage must not be null");
        if (exampleContext == null) {
            exampleContext = ProcessContext.empty();
        }
    }

    public static ProcessIntent of(String naturalLanguage) {
        return new ProcessIntent(naturalLanguage, null, ProcessContext.empty());
    }

    public static ProcessIntent of(String naturalLanguage, String initiator, ProcessContext exampleContext) {
        return new ProcessIntent(naturalLanguage, initiator, exampleContext);
    }
}
