package com.wegongdu.rillway.autoconfigure.persistence;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wegongdu.rillway.core.context.ProcessContext;
import com.wegongdu.rillway.core.instance.ExecutionRecord;
import com.wegongdu.rillway.core.instance.ProcessInstance;
import com.wegongdu.rillway.core.model.ProcessStatus;
import com.wegongdu.rillway.runtime.repository.ExecutionHistoryRepository;
import com.wegongdu.rillway.runtime.repository.ProcessInstanceRepository;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * JDBC implementation of ProcessInstanceRepository using Spring JdbcTemplate.
 */
public class JdbcProcessInstanceRepository implements ProcessInstanceRepository {

    private final JdbcTemplate jdbcTemplate;
    private final ExecutionHistoryRepository historyRepository;
    private final ObjectMapper objectMapper;

    public JdbcProcessInstanceRepository(
            JdbcTemplate jdbcTemplate,
            ExecutionHistoryRepository historyRepository,
            ObjectMapper objectMapper
    ) {
        this.jdbcTemplate = jdbcTemplate;
        this.historyRepository = historyRepository;
        this.objectMapper = objectMapper != null ? objectMapper : new ObjectMapper();
    }

    @Override
    public void save(ProcessInstance instance) {
        String sql = """
            INSERT INTO rillway_instance (id, business_key, definition_id, status, current_node_id, context_json, started_at, completed_at, error_message)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
        """;
        jdbcTemplate.update(
                sql,
                instance.id(),
                instance.businessKey(),
                instance.definitionId(),
                instance.status().name(),
                instance.currentNodeId(),
                serializeContext(instance.context()),
                Timestamp.from(instance.startedAt()),
                instance.completedAt() != null ? Timestamp.from(instance.completedAt()) : null,
                instance.errorMessage()
        );
    }

    @Override
    public void update(ProcessInstance instance) {
        String sql = """
            UPDATE rillway_instance
            SET status = ?, current_node_id = ?, context_json = ?, completed_at = ?, error_message = ?
            WHERE id = ?
        """;
        jdbcTemplate.update(
                sql,
                instance.status().name(),
                instance.currentNodeId(),
                serializeContext(instance.context()),
                instance.completedAt() != null ? Timestamp.from(instance.completedAt()) : null,
                instance.errorMessage(),
                instance.id()
        );
    }

    @Override
    public Optional<ProcessInstance> findById(String id) {
        String sql = "SELECT * FROM rillway_instance WHERE id = ?";
        try {
            ProcessInstance instance = jdbcTemplate.queryForObject(sql, new InstanceRowMapper(), id);
            return Optional.ofNullable(instance);
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    @Override
    public Optional<ProcessInstance> findByBusinessKey(String businessKey) {
        String sql = "SELECT * FROM rillway_instance WHERE business_key = ?";
        try {
            ProcessInstance instance = jdbcTemplate.queryForObject(sql, new InstanceRowMapper(), businessKey);
            return Optional.ofNullable(instance);
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    @Override
    public List<ProcessInstance> findByDefinitionId(String definitionId) {
        String sql = "SELECT * FROM rillway_instance WHERE definition_id = ? ORDER BY started_at DESC";
        return jdbcTemplate.query(sql, new InstanceRowMapper(), definitionId);
    }

    private String serializeContext(ProcessContext context) {
        try {
            return objectMapper.writeValueAsString(context != null ? context.variables() : Map.of());
        } catch (Exception e) {
            return "{}";
        }
    }

    private ProcessContext deserializeContext(String json) {
        if (json == null || json.isBlank()) {
            return ProcessContext.empty();
        }
        try {
            Map<String, Object> vars = objectMapper.readValue(json, new TypeReference<Map<String, Object>>() {});
            return ProcessContext.builder().variables(vars).build();
        } catch (Exception e) {
            return ProcessContext.empty();
        }
    }

    private class InstanceRowMapper implements RowMapper<ProcessInstance> {
        @Override
        public ProcessInstance mapRow(ResultSet rs, int rowNum) throws SQLException {
            String id = rs.getString("id");
            String businessKey = rs.getString("business_key");
            String definitionId = rs.getString("definition_id");
            ProcessStatus status = ProcessStatus.valueOf(rs.getString("status"));
            String currentNodeId = rs.getString("current_node_id");
            String contextJson = rs.getString("context_json");
            Timestamp startedAt = rs.getTimestamp("started_at");
            Timestamp completedAt = rs.getTimestamp("completed_at");
            String errorMessage = rs.getString("error_message");

            List<ExecutionRecord> history = historyRepository != null ? historyRepository.findByProcessInstanceId(id) : List.of();

            return new ProcessInstance(
                    id,
                    businessKey,
                    definitionId,
                    status,
                    currentNodeId,
                    deserializeContext(contextJson),
                    history,
                    startedAt != null ? startedAt.toInstant() : Instant.now(),
                    completedAt != null ? completedAt.toInstant() : null,
                    errorMessage
            );
        }
    }
}
