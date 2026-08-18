package com.wegongdu.rillway.autoconfigure.persistence;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wegongdu.rillway.core.model.Task;
import com.wegongdu.rillway.core.model.TaskStatus;
import com.wegongdu.rillway.runtime.repository.TaskRepository;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * JDBC implementation of TaskRepository using Spring JdbcTemplate.
 */
public class JdbcTaskRepository implements TaskRepository {

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    public JdbcTaskRepository(JdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper != null ? objectMapper : new ObjectMapper();
    }

    @Override
    public void save(Task task) {
        String sql = """
            INSERT INTO rillway_task (
                id, process_instance_id, business_key, definition_id, node_id, node_name,
                assignee_user, assignee_role, candidate_users_json, candidate_roles_json,
                status, created_at, completed_at
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """;
        jdbcTemplate.update(
                sql,
                task.id(),
                task.processInstanceId(),
                task.businessKey(),
                task.definitionId(),
                task.nodeId(),
                task.nodeName(),
                task.assigneeUser(),
                task.assigneeRole(),
                serializeList(task.candidateUsers()),
                serializeList(task.candidateRoles()),
                task.status().name(),
                Timestamp.from(task.createdAt()),
                task.completedAt() != null ? Timestamp.from(task.completedAt()) : null
        );
    }

    @Override
    public void update(Task task) {
        String sql = """
            UPDATE rillway_task
            SET assignee_user = ?, assignee_role = ?, candidate_users_json = ?, candidate_roles_json = ?, status = ?, completed_at = ?
            WHERE id = ?
        """;
        jdbcTemplate.update(
                sql,
                task.assigneeUser(),
                task.assigneeRole(),
                serializeList(task.candidateUsers()),
                serializeList(task.candidateRoles()),
                task.status().name(),
                task.completedAt() != null ? Timestamp.from(task.completedAt()) : null,
                task.id()
        );
    }

    @Override
    public Optional<Task> findById(String id) {
        String sql = "SELECT * FROM rillway_task WHERE id = ?";
        try {
            Task task = jdbcTemplate.queryForObject(sql, new TaskRowMapper(), id);
            return Optional.ofNullable(task);
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    @Override
    public List<Task> findByProcessInstanceId(String processInstanceId) {
        String sql = "SELECT * FROM rillway_task WHERE process_instance_id = ? ORDER BY created_at ASC";
        return jdbcTemplate.query(sql, new TaskRowMapper(), processInstanceId);
    }

    @Override
    public List<Task> findByBusinessKey(String businessKey) {
        String sql = "SELECT * FROM rillway_task WHERE business_key = ? ORDER BY created_at ASC";
        return jdbcTemplate.query(sql, new TaskRowMapper(), businessKey);
    }

    @Override
    public List<Task> findPendingTasksForUser(String userId, List<String> roles) {
        String sql = "SELECT * FROM rillway_task WHERE status = 'PENDING' ORDER BY created_at DESC";
        List<Task> allPending = jdbcTemplate.query(sql, new TaskRowMapper());

        List<String> safeRoles = roles != null ? roles : Collections.emptyList();
        return allPending.stream()
                .filter(t -> {
                    if (userId != null && userId.equals(t.assigneeUser())) {
                        return true;
                    }
                    if (userId != null && t.candidateUsers().contains(userId)) {
                        return true;
                    }
                    if (t.assigneeRole() != null && safeRoles.contains(t.assigneeRole())) {
                        return true;
                    }
                    if (t.candidateRoles().stream().anyMatch(safeRoles::contains)) {
                        return true;
                    }
                    return t.assigneeUser() == null && t.assigneeRole() == null
                            && t.candidateUsers().isEmpty() && t.candidateRoles().isEmpty();
                })
                .toList();
    }

    @Override
    public List<Task> findTasksByStatus(TaskStatus status) {
        String sql = "SELECT * FROM rillway_task WHERE status = ? ORDER BY created_at DESC";
        return jdbcTemplate.query(sql, new TaskRowMapper(), status.name());
    }

    private String serializeList(List<String> list) {
        try {
            return objectMapper.writeValueAsString(list != null ? list : List.of());
        } catch (Exception e) {
            return "[]";
        }
    }

    private List<String> deserializeList(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<List<String>>() {});
        } catch (Exception e) {
            return List.of();
        }
    }

    private class TaskRowMapper implements RowMapper<Task> {
        @Override
        public Task mapRow(ResultSet rs, int rowNum) throws SQLException {
            String id = rs.getString("id");
            String processInstanceId = rs.getString("process_instance_id");
            String businessKey = rs.getString("business_key");
            String definitionId = rs.getString("definition_id");
            String nodeId = rs.getString("node_id");
            String nodeName = rs.getString("node_name");
            String assigneeUser = rs.getString("assignee_user");
            String assigneeRole = rs.getString("assignee_role");
            String candidateUsersJson = rs.getString("candidate_users_json");
            String candidateRolesJson = rs.getString("candidate_roles_json");
            TaskStatus status = TaskStatus.valueOf(rs.getString("status"));
            Timestamp createdAt = rs.getTimestamp("created_at");
            Timestamp completedAt = rs.getTimestamp("completed_at");

            return new Task(
                    id,
                    processInstanceId,
                    businessKey,
                    definitionId,
                    nodeId,
                    nodeName,
                    assigneeUser,
                    assigneeRole,
                    deserializeList(candidateUsersJson),
                    deserializeList(candidateRolesJson),
                    status,
                    createdAt != null ? createdAt.toInstant() : Instant.now(),
                    completedAt != null ? completedAt.toInstant() : null
            );
        }
    }
}
