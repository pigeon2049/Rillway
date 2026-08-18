package com.wegongdu.rillway.autoconfigure.persistence;

import com.wegongdu.rillway.ai.config.AiModelConfig;
import com.wegongdu.rillway.ai.config.AiModelConfigRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * JDBC implementation of AiModelConfigRepository using Spring JdbcTemplate.
 */
public class JdbcAiModelConfigRepository implements AiModelConfigRepository {

    private final JdbcTemplate jdbcTemplate;

    public JdbcAiModelConfigRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void save(AiModelConfig config) {
        if (config == null || config.id() == null) return;

        // If marked as default, clear other default flags
        if (config.isDefault()) {
            jdbcTemplate.update("UPDATE rillway_ai_config SET is_default = FALSE WHERE id <> ?", config.id());
        }

        jdbcTemplate.update("DELETE FROM rillway_ai_config WHERE id = ?", config.id());

        String sql = """
            INSERT INTO rillway_ai_config (
                id, provider_name, base_url, api_key, model_name,
                temperature, timeout_seconds, is_default, enabled, updated_at
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """;
        jdbcTemplate.update(
                sql,
                config.id(),
                config.providerName(),
                config.baseUrl(),
                config.apiKey(),
                config.modelName(),
                config.temperature(),
                config.timeoutSeconds(),
                config.isDefault(),
                config.enabled(),
                Timestamp.from(config.updatedAt() != null ? config.updatedAt() : Instant.now())
        );
    }

    @Override
    public Optional<AiModelConfig> findById(String id) {
        if (id == null) return Optional.empty();
        String sql = "SELECT * FROM rillway_ai_config WHERE id = ?";
        List<AiModelConfig> list = jdbcTemplate.query(sql, new AiModelConfigRowMapper(), id);
        return list.stream().filter(AiModelConfig::enabled).findFirst();
    }

    @Override
    public Optional<AiModelConfig> findDefault() {
        String sql = "SELECT * FROM rillway_ai_config WHERE is_default = TRUE AND enabled = TRUE ORDER BY updated_at DESC";
        List<AiModelConfig> list = jdbcTemplate.query(sql, new AiModelConfigRowMapper());
        if (!list.isEmpty()) {
            return Optional.of(list.get(0));
        }
        // Fallback: any enabled config
        String fallbackSql = "SELECT * FROM rillway_ai_config WHERE enabled = TRUE ORDER BY updated_at DESC";
        List<AiModelConfig> fallbackList = jdbcTemplate.query(fallbackSql, new AiModelConfigRowMapper());
        return fallbackList.stream().findFirst();
    }

    @Override
    public Optional<AiModelConfig> findByProvider(String providerName) {
        if (providerName == null) return Optional.empty();
        String sql = "SELECT * FROM rillway_ai_config WHERE LOWER(provider_name) = LOWER(?) AND enabled = TRUE ORDER BY updated_at DESC";
        List<AiModelConfig> list = jdbcTemplate.query(sql, new AiModelConfigRowMapper(), providerName);
        return list.stream().findFirst();
    }

    @Override
    public List<AiModelConfig> findAll() {
        String sql = "SELECT * FROM rillway_ai_config ORDER BY updated_at DESC";
        return jdbcTemplate.query(sql, new AiModelConfigRowMapper());
    }

    @Override
    public void deleteById(String id) {
        if (id == null) return;
        jdbcTemplate.update("DELETE FROM rillway_ai_config WHERE id = ?", id);
    }

    private static class AiModelConfigRowMapper implements RowMapper<AiModelConfig> {
        @Override
        public AiModelConfig mapRow(ResultSet rs, int rowNum) throws SQLException {
            Timestamp ts = rs.getTimestamp("updated_at");
            Instant updatedAt = ts != null ? ts.toInstant() : Instant.now();

            return new AiModelConfig(
                    rs.getString("id"),
                    rs.getString("provider_name"),
                    rs.getString("base_url"),
                    rs.getString("api_key"),
                    rs.getString("model_name"),
                    rs.getDouble("temperature"),
                    rs.getInt("timeout_seconds"),
                    rs.getBoolean("is_default"),
                    rs.getBoolean("enabled"),
                    updatedAt
            );
        }
    }
}
