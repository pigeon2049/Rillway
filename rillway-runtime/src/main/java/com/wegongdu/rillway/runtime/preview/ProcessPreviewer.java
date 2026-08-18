package com.wegongdu.rillway.runtime.preview;

import com.wegongdu.rillway.core.definition.ProcessDefinition;

/**
 * Previewer interface for analyzing potential workflow execution paths.
 */
public interface ProcessPreviewer {

    ProcessPreview preview(ProcessDefinition definition, PreviewContext context);
}
