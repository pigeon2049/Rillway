package com.wegongdu.rillway.ai.intent;

import com.wegongdu.rillway.core.definition.ProcessDefinition;

/**
 * SPI for converting natural language business intents into structured, executable ProcessDefinitions.
 */
public interface IntentInterpreter {

    ProcessDefinition interpret(ProcessIntent intent);
}
