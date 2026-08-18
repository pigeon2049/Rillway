package com.wegongdu.rillway.autoconfigure.persistence;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wegongdu.rillway.core.actor.Actor;
import com.wegongdu.rillway.core.decision.ApproveDecision;
import com.wegongdu.rillway.core.decision.Decision;
import com.wegongdu.rillway.core.decision.RejectDecision;
import com.wegongdu.rillway.core.decision.RouteDecision;
import com.wegongdu.rillway.core.instance.ExecutionRecord;
import com.wegongdu.rillway.core.model.NodeType;
import com.wegongdu.rillway.runtime.repository.ExecutionHistoryRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * JDBC implementation of ExecutionHistoryRepository.
 */
public class JdbcExecutionHistoryRepository implements ExecutionHistoryRepository {

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    public JdbcExecutionHistoryRepository(JdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper != null ? objectMapper : new ObjectMapper();
    }

    @Override
    public void save(String processInstanceId, ExecutionRecord record) {
        String sql = """
            INSERT INTO rillway_history (
                id, process_instance_id, node_id, node_name, node_type, actor_json, decision_json, entered_at, completed_at, error_message
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """;
        jdbcTemplate.update(
                sql,
                UUID.randomUUID().toString(),
                processInstanceId,
                record.nodeId(),
                record.nodeName(),
                record.nodeType().name(),
                record.actor() != null ? record.actor().identifier() : null,
                serializeDecision(record.decision()),
                Timestamp.from(record.enteredAt()),
                record.completedAt() != null ? Timestamp.from(record.completedAt()) : null,
                record.errorMessage()
        );
    }

    @Override
    public List<ExecutionRecord> findByProcessInstanceId(String processInstanceId) {
        String sql = "SELECT * FROM rillway_history WHERE process_instance_id = ? ORDER BY entered_at ASC";
        return jdbcTemplate.query(sql, new HistoryRowMapper(), processInstanceId);
    }

    private String serializeDecision(Decision decision) {
        if (decision == null) return null;
        try {
            return objectMapper.writeValueAsString(decision);
        } catch (Exception e) {
            return decision.type() + ":" + decision.reason();
        }
    }

    private class HistoryRowMapper implements RowMapper<ExecutionRecord> {
        @Override
        public ExecutionRecord mapRow(ResultSet rs, int rowNum) throws SQLException {
            String nodeId = rs.getString("node_id");
            String nodeName = rs.getString("node_name");
            NodeType nodeType = NodeType.valueOf(rs.getString("node_type"));
            String actorStr = rs.getString("actor_json");
            Timestamp enteredAt = rs.getTimestamp("entered_at");
            Timestamp completedAt = rs.getTimestamp("completed_at");
            String errorMessage = rs.getString("error_message");

            Actor actor = null;
            if (actorStr != null) {
                if (actorStr.startsWith("human:")) {
                    actor = Actor.HumanActor.of(actorStr.substring(6));
                } else if (actorStr.startsWith("rule:")) {
                    actor = Actor.RuleActor.of(actorStr.substring(5));
                } else if (actorStr.startsWith("agent:")) {
                    actor = Actor.AgentActor.of(actorStr.substring(6));
                }
            }

            return new ExecutionRecord(
                    nodeId,
                    nodeName,
                    nodeType,
                    actor,
                    null, // lightweight decision summary
                    enteredAt != null ? enteredAt.toInstant() : Instant.now(),
                    completedAt != null ? completedAt.toInstant() : null,
                    errorMessage
            );
        }
    }
}
