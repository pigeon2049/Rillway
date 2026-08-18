package com.wegongdu.rillway.runtime.preview;

import com.wegongdu.rillway.core.context.ProcessContext;
import java.io.Serializable;

/**
 * Context for previewing potential workflow paths before initiating.
 */
public record PreviewContext(
        String initiator,
        ProcessContext variables
) implements Serializable {

    public static PreviewContext of(String initiator, ProcessContext variables) {
        return new PreviewContext(initiator, variables);
    }
}
