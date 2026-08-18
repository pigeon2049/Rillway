package com.wegongdu.rillway.autoconfigure.persistence;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;

/**
 * Automatically initializes Rillway tables on startup if they do not exist.
 */
public class RillwayDatabaseInitializer implements InitializingBean {

    private static final Logger log = LoggerFactory.getLogger(RillwayDatabaseInitializer.class);

    private final JdbcTemplate jdbcTemplate;

    public RillwayDatabaseInitializer(JdbcTemplate jdbcTemplate, DataSource dataSource) {
        this.jdbcTemplate = jdbcTemplate != null ? jdbcTemplate : new JdbcTemplate(dataSource);
    }

    @Override
    public void afterPropertiesSet() {
        initialize();
    }

    public synchronized void initialize() {
        if (!tableExists("rillway_instance")) {
            log.info("Rillway table [rillway_instance] not found. Creating table automatically...");
            jdbcTemplate.execute("""
                CREATE TABLE rillway_instance (
                    id VARCHAR(64) PRIMARY KEY,
                    business_key VARCHAR(128),
                    definition_id VARCHAR(64) NOT NULL,
                    status VARCHAR(32) NOT NULL,
                    current_node_id VARCHAR(64),
                    context_json TEXT,
                    started_at TIMESTAMP NOT NULL,
                    completed_at TIMESTAMP,
                    error_message VARCHAR(512)
                )
            """);
        }

        if (!tableExists("rillway_task")) {
            log.info("Rillway table [rillway_task] not found. Creating table automatically...");
            jdbcTemplate.execute("""
                CREATE TABLE rillway_task (
                    id VARCHAR(64) PRIMARY KEY,
                    process_instance_id VARCHAR(64) NOT NULL,
                    business_key VARCHAR(128),
                    definition_id VARCHAR(64) NOT NULL,
                    node_id VARCHAR(64) NOT NULL,
                    node_name VARCHAR(128),
                    assignee_user VARCHAR(64),
                    assignee_role VARCHAR(64),
                    candidate_users_json VARCHAR(512),
                    candidate_roles_json VARCHAR(512),
                    status VARCHAR(32) NOT NULL,
                    created_at TIMESTAMP NOT NULL,
                    completed_at TIMESTAMP
                )
            """);
        }

        if (!tableExists("rillway_history")) {
            log.info("Rillway table [rillway_history] not found. Creating table automatically...");
            jdbcTemplate.execute("""
                CREATE TABLE rillway_history (
                    id VARCHAR(64) PRIMARY KEY,
                    process_instance_id VARCHAR(64) NOT NULL,
                    node_id VARCHAR(64) NOT NULL,
                    node_name VARCHAR(128),
                    node_type VARCHAR(32) NOT NULL,
                    actor_json VARCHAR(512),
                    decision_json TEXT,
                    entered_at TIMESTAMP NOT NULL,
                    completed_at TIMESTAMP,
                    error_message VARCHAR(512)
                )
            """);
        }

        if (!tableExists("rillway_binding_config")) {
            log.info("Rillway table [rillway_binding_config] not found. Creating table automatically...");
            jdbcTemplate.execute("""
                CREATE TABLE rillway_binding_config (
                    id VARCHAR(64) PRIMARY KEY,
                    business_type VARCHAR(64) NOT NULL,
                    process_definition_id VARCHAR(64) NOT NULL,
                    table_name VARCHAR(128) NOT NULL,
                    primary_key_column VARCHAR(64) NOT NULL,
                    status_column VARCHAR(64) NOT NULL,
                    approved_value VARCHAR(64) NOT NULL,
                    rejected_value VARCHAR(64) NOT NULL,
                    running_value VARCHAR(64),
                    enabled BOOLEAN NOT NULL
                )
            """);
        }

        if (!tableExists("rillway_resolution_cache")) {
            log.info("Rillway table [rillway_resolution_cache] not found. Creating table automatically...");
            jdbcTemplate.execute("""
                CREATE TABLE rillway_resolution_cache (
                    id VARCHAR(64) PRIMARY KEY,
                    definition_id VARCHAR(64) NOT NULL,
                    node_id VARCHAR(64) NOT NULL,
                    prompt_hash VARCHAR(64) NOT NULL,
                    initiator_user_id VARCHAR(64),
                    initiator_dept_id VARCHAR(64),
                    initiator_post_code VARCHAR(64),
                    resolved_user_id VARCHAR(64),
                    resolved_dept_id VARCHAR(64),
                    resolved_post_code VARCHAR(64),
                    resolved_role VARCHAR(64),
                    candidate_users_json VARCHAR(512),
                    candidate_roles_json VARCHAR(512),
                    hit_count INT DEFAULT 0,
                    expires_at TIMESTAMP NOT NULL,
                    created_at TIMESTAMP NOT NULL,
                    updated_at TIMESTAMP NOT NULL
                )
            """);
        }
    }

    private boolean tableExists(String tableName) {
        try {
            jdbcTemplate.queryForObject("SELECT 1 FROM " + tableName + " WHERE 1 = 0", Integer.class);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
