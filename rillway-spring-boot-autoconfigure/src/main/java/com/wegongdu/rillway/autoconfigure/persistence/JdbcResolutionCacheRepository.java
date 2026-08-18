package com.wegongdu.rillway.autoconfigure.persistence;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wegongdu.rillway.ai.cache.ResolutionCacheRepository;
import com.wegongdu.rillway.core.model.ResolutionCache;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * JDBC implementation of ResolutionCacheRepository.
 */
public class JdbcResolutionCacheRepository implements ResolutionCacheRepository {

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    public JdbcResolutionCacheRepository(JdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper != null ? objectMapper : new ObjectMapper();
    }

    @Override
    public void save(ResolutionCache cache) {
        if (cache == null || cache.id() == null) return;

        // Delete existing entry with same key attributes if any
        String deleteSql = """
            DELETE FROM rillway_resolution_cache
            WHERE definition_id = ? AND node_id = ? AND prompt_hash = ?
            AND ((department_id IS NULL AND ? IS NULL) OR department_id = ?)
            AND ((post_code IS NULL AND ? IS NULL) OR post_code = ?)
        """;
        jdbcTemplate.update(
                deleteSql,
                cache.definitionId(),
                cache.nodeId(),
                cache.promptHash(),
                cache.departmentId(),
                cache.departmentId(),
                cache.postCode(),
                cache.postCode()
        );

        String insertSql = """
            INSERT INTO rillway_resolution_cache (
                id, definition_id, node_id, prompt_hash, department_id, post_code,
                resolved_user_id, resolved_role, candidate_users_json, candidate_roles_json,
                hit_count, created_at, updated_at
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """;
        jdbcTemplate.update(
                insertSql,
                cache.id(),
                cache.definitionId(),
                cache.nodeId(),
                cache.promptHash(),
                cache.departmentId(),
                cache.postCode(),
                cache.resolvedUserId(),
                cache.resolvedRole(),
                serializeList(cache.candidateUsers()),
                serializeList(cache.candidateRoles()),
                cache.hitCount(),
                Timestamp.from(cache.createdAt()),
                Timestamp.from(cache.updatedAt())
        );
    }

    @Override
    public Optional<ResolutionCache> findMatch(String definitionId, String nodeId, String promptHash, String departmentId, String postCode) {
        String sql = """
            SELECT * FROM rillway_resolution_cache
            WHERE definition_id = ? AND node_id = ? AND prompt_hash = ?
            AND ((department_id IS NULL AND ? IS NULL) OR department_id = ?)
            AND ((post_code IS NULL AND ? IS NULL) OR post_code = ?)
        """;
        List<ResolutionCache> list = jdbcTemplate.query(
                sql,
                new CacheRowMapper(),
                definitionId,
                nodeId,
                promptHash,
                departmentId,
                departmentId,
                postCode,
                postCode
        );
        return list.stream().findFirst();
    }

    @Override
    public List<ResolutionCache> findRecentByDefinitionAndNode(String definitionId, String nodeId, int limit) {
        String sql = "SELECT * FROM rillway_resolution_cache WHERE definition_id = ? AND node_id = ? ORDER BY updated_at DESC LIMIT ?";
        return jdbcTemplate.query(sql, new CacheRowMapper(), definitionId, nodeId, limit);
    }

    @Override
    public void delete(String id) {
        if (id != null) {
            jdbcTemplate.update("DELETE FROM rillway_resolution_cache WHERE id = ?", id);
        }
    }

    private String serializeList(List<String> list) {
        if (list == null || list.isEmpty()) return null;
        try {
            return objectMapper.writeValueAsString(list);
        } catch (Exception e) {
            return String.join(",", list);
        }
    }

    private List<String> deserializeList(String json) {
        if (json == null || json.isBlank()) return List.of();
        try {
            return objectMapper.readValue(json, new TypeReference<List<String>>() {});
        } catch (Exception e) {
            return List.of(json.split(","));
        }
    }

    private class CacheRowMapper implements RowMapper<ResolutionCache> {
        @Override
        public ResolutionCache mapRow(ResultSet rs, int rowNum) throws SQLException {
            Timestamp createdAt = rs.getTimestamp("created_at");
            Timestamp updatedAt = rs.getTimestamp("updated_at");

            return new ResolutionCache(
                    rs.getString("id"),
                    rs.getString("definition_id"),
                    rs.getString("node_id"),
                    rs.getString("prompt_hash"),
                    rs.getString("department_id"),
                    rs.getString("post_code"),
                    rs.getString("resolved_user_id"),
                    rs.getString("resolved_role"),
                    deserializeList(rs.getString("candidate_users_json")),
                    deserializeList(rs.getString("candidate_roles_json")),
                    rs.getInt("hit_count"),
                    createdAt != null ? createdAt.toInstant() : Instant.now(),
                    updatedAt != null ? updatedAt.toInstant() : Instant.now()
            );
        }
    }
}
