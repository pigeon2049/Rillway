package com.wegongdu.rillway.ai.trace;

import java.util.List;

/**
 * SPI for recording and querying LLM/Tool execution traces.
 */
public interface LlmTraceSink {

    /**
     * Records a trace record asynchronously or synchronously.
     */
    void record(LlmTraceRecord record);

    /**
     * Finds trace records by trace ID (e.g. process instance ID).
     */
    List<LlmTraceRecord> findByTraceId(String traceId);
}
