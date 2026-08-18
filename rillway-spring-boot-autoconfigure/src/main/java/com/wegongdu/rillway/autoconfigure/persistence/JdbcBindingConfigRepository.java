package com.wegongdu.rillway.autoconfigure.persistence;

import com.wegongdu.rillway.core.model.BindingConfig;
import com.wegongdu.rillway.runtime.repository.BindingConfigRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

/**
 * JDBC implementation of BindingConfigRepository using Spring JdbcTemplate.
 */
public class JdbcBindingConfigRepository implements BindingConfigRepository {

    private final JdbcTemplate jdbcTemplate;

    public JdbcBindingConfigRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void save(BindingConfig config) {
        if (config == null || config.id() == null) return;
        jdbcTemplate.update("DELETE FROM rillway_binding_config WHERE id = ? OR LOWER(business_type) = LOWER(?)",
                config.id(), config.businessType());

        String sql = """
            INSERT INTO rillway_binding_config (
                id, business_type, process_definition_id, process_prompt, table_name, primary_key_column,
                status_column, approved_value, rejected_value, running_value, enabled
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """;
        jdbcTemplate.update(
                sql,
                config.id(),
                config.businessType(),
                config.processDefinitionId(),
                config.processPrompt(),
                config.tableName(),
                config.primaryKeyColumn(),
                config.statusColumn(),
                config.approvedValue(),
                config.rejectedValue(),
                config.runningValue(),
                config.enabled()
        );
    }

    @Override
    public Optional<BindingConfig> findByBusinessType(String businessType) {
        if (businessType == null) return Optional.empty();
        String sql = "SELECT * FROM rillway_binding_config WHERE LOWER(business_type) = LOWER(?)";
        List<BindingConfig> list = jdbcTemplate.query(sql, new BindingConfigRowMapper(), businessType);
        return list.stream().filter(BindingConfig::enabled).findFirst();
    }

    @Override
    public Optional<BindingConfig> findByTableName(String tableName) {
        if (tableName == null) return Optional.empty();
        String sql = "SELECT * FROM rillway_binding_config WHERE LOWER(table_name) = LOWER(?)";
        List<BindingConfig> list = jdbcTemplate.query(sql, new BindingConfigRowMapper(), tableName);
        return list.stream().filter(BindingConfig::enabled).findFirst();
    }

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(JdbcBindingConfigRepository.class);

    @Override
    public Optional<BindingConfig> findMatching(String identifier) {
        if (identifier == null) return Optional.empty();
        String sql = "SELECT * FROM rillway_binding_config WHERE LOWER(business_type) = LOWER(?) OR LOWER(table_name) = LOWER(?)";
        List<BindingConfig> list = jdbcTemplate.query(sql, new BindingConfigRowMapper(), identifier, identifier);
        log.info("[JdbcBindingConfigRepository] findMatching identifier='{}', found count={}, list={}", identifier, list.size(), list);
        return list.stream().filter(BindingConfig::enabled).findFirst();
    }

    @Override
    public Optional<BindingConfig> findByProcessDefinitionId(String processDefinitionId) {
        if (processDefinitionId == null) return Optional.empty();
        String sql = "SELECT * FROM rillway_binding_config WHERE process_definition_id = ?";
        List<BindingConfig> list = jdbcTemplate.query(sql, new BindingConfigRowMapper(), processDefinitionId);
        return list.stream().filter(BindingConfig::enabled).findFirst();
    }

    @Override
    public List<BindingConfig> listAll() {
        String sql = "SELECT * FROM rillway_binding_config ORDER BY business_type ASC";
        return jdbcTemplate.query(sql, new BindingConfigRowMapper());
    }

    private static class BindingConfigRowMapper implements RowMapper<BindingConfig> {
        @Override
        public BindingConfig mapRow(ResultSet rs, int rowNum) throws SQLException {
            String prompt = null;
            try {
                prompt = rs.getString("process_prompt");
            } catch (Exception ignored) {}

            boolean enabled = true;
            try {
                Object enabledObj = rs.getObject("enabled");
                if (enabledObj instanceof Boolean b) {
                    enabled = b;
                } else if (enabledObj instanceof Number n) {
                    enabled = n.intValue() != 0;
                } else if (enabledObj != null) {
                    String str = enabledObj.toString().trim();
                    enabled = "1".equals(str) || "true".equalsIgnoreCase(str);
                }
            } catch (Exception ignored) {}

            return new BindingConfig(
                    rs.getString("id"),
                    rs.getString("business_type"),
                    rs.getString("process_definition_id"),
                    prompt,
                    rs.getString("table_name"),
                    rs.getString("primary_key_column"),
                    rs.getString("status_column"),
                    rs.getString("approved_value"),
                    rs.getString("rejected_value"),
                    rs.getString("running_value"),
                    enabled
            );
        }
    }
}
