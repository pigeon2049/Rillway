package com.wegongdu.rillway.autoconfigure;

import com.wegongdu.rillway.agent.guard.AgentAuthorityGuard;
import com.wegongdu.rillway.agent.registry.InMemoryAgentRegistry;
import com.wegongdu.rillway.agent.spi.AgentRegistry;
import com.wegongdu.rillway.ai.intent.FakeIntentInterpreter;
import com.wegongdu.rillway.ai.intent.IntentInterpreter;
import com.wegongdu.rillway.audit.sink.AuditSink;
import com.wegongdu.rillway.audit.sink.InMemoryAuditSink;
import com.wegongdu.rillway.audit.sink.NoOpAuditSink;
import com.wegongdu.rillway.core.node.Node;
import com.wegongdu.rillway.core.validation.ProcessValidator;
import com.wegongdu.rillway.core.validation.StandardProcessValidator;
import com.wegongdu.rillway.policy.provider.InMemoryPolicyProvider;
import com.wegongdu.rillway.policy.spi.PolicyProvider;
import com.wegongdu.rillway.runtime.engine.ProcessEngine;
import com.wegongdu.rillway.runtime.engine.StandardProcessEngine;
import com.wegongdu.rillway.runtime.executor.NodeExecutor;
import com.wegongdu.rillway.runtime.executor.impl.AgentNodeExecutor;
import com.wegongdu.rillway.runtime.executor.impl.EndNodeExecutor;
import com.wegongdu.rillway.runtime.executor.impl.HumanNodeExecutor;
import com.wegongdu.rillway.runtime.executor.impl.RuleNodeExecutor;
import com.wegongdu.rillway.runtime.executor.impl.StartNodeExecutor;
import com.wegongdu.rillway.runtime.preview.ProcessPreviewer;
import com.wegongdu.rillway.runtime.preview.StaticProcessPreviewer;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

import java.util.List;

/**
 * Spring Boot 3 auto-configuration for Rillway workflow engine.
 */
@AutoConfiguration
@EnableConfigurationProperties(RillwayProperties.class)
@ConditionalOnProperty(prefix = "rillway", name = "enabled", havingValue = "true", matchIfMissing = true)
public class RillwayAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public AgentRegistry agentRegistry() {
        return new InMemoryAgentRegistry();
    }

    @Bean
    @ConditionalOnMissingBean
    public PolicyProvider policyProvider() {
        return new InMemoryPolicyProvider();
    }

    @Bean
    @ConditionalOnMissingBean
    public AgentAuthorityGuard agentAuthorityGuard() {
        return new AgentAuthorityGuard();
    }

    @Bean
    @ConditionalOnMissingBean
    public AuditSink auditSink(RillwayProperties properties) {
        if (!properties.getAudit().isEnabled() || "no-op".equalsIgnoreCase(properties.getAudit().getSink())) {
            return NoOpAuditSink.INSTANCE;
        }
        return new InMemoryAuditSink();
    }

    @Bean
    @ConditionalOnMissingBean
    public ProcessValidator processValidator() {
        return new StandardProcessValidator();
    }

    @Bean
    @ConditionalOnMissingBean
    public ProcessPreviewer processPreviewer() {
        return new StaticProcessPreviewer();
    }

    @Bean
    @ConditionalOnMissingBean
    public IntentInterpreter intentInterpreter() {
        return new FakeIntentInterpreter();
    }

    @Bean
    @ConditionalOnMissingBean
    public StartNodeExecutor startNodeExecutor() {
        return new StartNodeExecutor();
    }

    @Bean
    @ConditionalOnMissingBean
    public EndNodeExecutor endNodeExecutor() {
        return new EndNodeExecutor();
    }

    @Bean
    @ConditionalOnMissingBean
    public HumanNodeExecutor humanNodeExecutor() {
        return new HumanNodeExecutor();
    }

    @Bean
    @ConditionalOnMissingBean
    public RuleNodeExecutor ruleNodeExecutor() {
        return new RuleNodeExecutor();
    }

    @Bean
    @ConditionalOnMissingBean
    public AgentNodeExecutor agentNodeExecutor(
            AgentRegistry agentRegistry,
            PolicyProvider policyProvider,
            AgentAuthorityGuard authorityGuard,
            AuditSink auditSink
    ) {
        return new AgentNodeExecutor(agentRegistry, policyProvider, authorityGuard, auditSink);
    }

    @Bean
    @ConditionalOnMissingBean
    public ProcessEngine processEngine(
            List<NodeExecutor<? extends Node>> executors,
            ProcessValidator validator,
            AuditSink auditSink
    ) {
        return new StandardProcessEngine(executors, validator, auditSink);
    }
}
