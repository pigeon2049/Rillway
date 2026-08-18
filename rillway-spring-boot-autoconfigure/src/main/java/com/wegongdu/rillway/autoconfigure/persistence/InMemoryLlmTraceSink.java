package com.wegongdu.rillway.autoconfigure.persistence;

import com.wegongdu.rillway.ai.trace.LlmTraceRecord;
import com.wegongdu.rillway.ai.trace.LlmTraceSink;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class InMemoryLlmTraceSink implements LlmTraceSink {

    private final List<LlmTraceRecord> records = new CopyOnWriteArrayList<>();

    @Override
    public void record(LlmTraceRecord record) {
        if (record != null) {
            records.add(record);
        }
    }

    @Override
    public List<LlmTraceRecord> findByTraceId(String traceId) {
        if (traceId == null) return Collections.emptyList();
        List<LlmTraceRecord> result = new ArrayList<>();
        for (LlmTraceRecord r : records) {
            if (traceId.equals(r.traceId())) {
                result.add(r);
            }
        }
        return result;
    }

    public List<LlmTraceRecord> findAll() {
        return List.copyOf(records);
    }

    public void clear() {
        records.clear();
    }
}
