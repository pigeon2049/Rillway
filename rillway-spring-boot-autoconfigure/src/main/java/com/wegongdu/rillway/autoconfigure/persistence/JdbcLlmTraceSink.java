package com.wegongdu.rillway.autoconfigure.persistence;

import com.wegongdu.rillway.ai.trace.LlmTraceRecord;
import com.wegongdu.rillway.ai.trace.LlmTraceSink;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;

public class JdbcLlmTraceSink implements LlmTraceSink {

    private static final Logger log = LoggerFactory.getLogger(JdbcLlmTraceSink.class);

    private final JdbcTemplate jdbcTemplate;

    public JdbcLlmTraceSink(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void record(LlmTraceRecord record) {
        if (record == null) return;
        String sql = "INSERT INTO rillway_ai_trace (" +
                "id, trace_id, node_id, business_type, model, call_type, " +
                "prompt_text, response_text, tool_name, tool_arguments, tool_result, " +
                "tool_calls_json, prompt_tokens, completion_tokens, total_tokens, " +
                "latency_ms, status, error_message, created_at" +
                ") VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try {
            jdbcTemplate.update(sql,
                    record.id(),
                    record.traceId(),
                    record.nodeId(),
                    record.businessType(),
                    record.model(),
                    record.callType(),
                    record.promptText(),
                    record.responseText(),
                    record.toolName(),
                    record.toolArguments(),
                    record.toolResult(),
                    record.toolCallsJson(),
                    record.promptTokens(),
                    record.completionTokens(),
                    record.totalTokens(),
                    record.latencyMs(),
                    record.status(),
                    record.errorMessage(),
                    record.createdAt() != null ? Timestamp.from(record.createdAt()) : new Timestamp(System.currentTimeMillis())
            );
        } catch (Exception e) {
            log.warn("Failed to insert rillway_ai_trace record [{}]: {}", record.id(), e.getMessage());
        }
    }

    @Override
    public List<LlmTraceRecord> findByTraceId(String traceId) {
        String sql = "SELECT * FROM rillway_ai_trace WHERE trace_id = ? ORDER BY created_at ASC";
        return jdbcTemplate.query(sql, new LlmTraceRowMapper(), traceId);
    }

    public List<LlmTraceRecord> findLatest(int limit) {
        String sql = "SELECT * FROM rillway_ai_trace ORDER BY created_at DESC LIMIT ?";
        return jdbcTemplate.query(sql, new LlmTraceRowMapper(), limit);
    }

    private static class LlmTraceRowMapper implements RowMapper<LlmTraceRecord> {
        @Override
        public LlmTraceRecord mapRow(ResultSet rs, int rowNum) throws SQLException {
            Timestamp ts = rs.getTimestamp("created_at");
            return new LlmTraceRecord(
                    rs.getString("id"),
                    rs.getString("trace_id"),
                    rs.getString("node_id"),
                    rs.getString("business_type"),
                    rs.getString("model"),
                    rs.getString("call_type"),
                    rs.getString("prompt_text"),
                    rs.getString("response_text"),
                    rs.getString("tool_name"),
                    rs.getString("tool_arguments"),
                    rs.getString("tool_result"),
                    rs.getString("tool_calls_json"),
                    rs.getInt("prompt_tokens"),
                    rs.getInt("completion_tokens"),
                    rs.getInt("total_tokens"),
                    rs.getLong("latency_ms"),
                    rs.getString("status"),
                    rs.getString("error_message"),
                    ts != null ? ts.toInstant() : null
            );
        }
    }
}
