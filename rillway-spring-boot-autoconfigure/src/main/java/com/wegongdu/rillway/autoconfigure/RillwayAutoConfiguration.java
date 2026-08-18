package com.wegongdu.rillway.autoconfigure;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wegongdu.rillway.agent.guard.AgentAuthorityGuard;
import com.wegongdu.rillway.agent.registry.InMemoryAgentRegistry;
import com.wegongdu.rillway.agent.spi.AgentRegistry;
import com.wegongdu.rillway.ai.cache.InMemoryResolutionCacheRepository;
import com.wegongdu.rillway.ai.cache.ResolutionCacheManager;
import com.wegongdu.rillway.ai.cache.ResolutionCacheRepository;
import com.wegongdu.rillway.ai.config.AiModelConfig;
import com.wegongdu.rillway.ai.config.AiModelConfigRepository;
import com.wegongdu.rillway.ai.identity.AiAssigneeResolver;
import com.wegongdu.rillway.ai.intent.FakeIntentInterpreter;
import com.wegongdu.rillway.ai.intent.IntentInterpreter;
import com.wegongdu.rillway.ai.intent.LlmIntentInterpreter;
import com.wegongdu.rillway.ai.llm.FakeLlmClient;
import com.wegongdu.rillway.ai.llm.LlmClient;
import com.wegongdu.rillway.ai.llm.OpenAiCompatibleLlmClient;
import com.wegongdu.rillway.audit.sink.AuditSink;
import com.wegongdu.rillway.audit.sink.InMemoryAuditSink;
import com.wegongdu.rillway.audit.sink.NoOpAuditSink;
import com.wegongdu.rillway.autoconfigure.binding.EntityStatusAutoUpdater;
import com.wegongdu.rillway.autoconfigure.event.SpringApplicationEventPublisherBridge;
import com.wegongdu.rillway.autoconfigure.persistence.JdbcAiModelConfigRepository;
import com.wegongdu.rillway.autoconfigure.persistence.JdbcBindingConfigRepository;
import com.wegongdu.rillway.autoconfigure.persistence.JdbcExecutionHistoryRepository;
import com.wegongdu.rillway.autoconfigure.persistence.JdbcProcessInstanceRepository;
import com.wegongdu.rillway.autoconfigure.persistence.JdbcResolutionCacheRepository;
import com.wegongdu.rillway.autoconfigure.persistence.JdbcTaskRepository;
import com.wegongdu.rillway.autoconfigure.persistence.JdbcLlmTraceSink;
import com.wegongdu.rillway.autoconfigure.persistence.InMemoryAiModelConfigRepository;
import com.wegongdu.rillway.autoconfigure.persistence.InMemoryLlmTraceSink;
import com.wegongdu.rillway.autoconfigure.persistence.RillwayDatabaseInitializer;
import com.wegongdu.rillway.core.identity.HumanAssigneeResolver;
import com.wegongdu.rillway.core.identity.IdentityService;
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
import com.wegongdu.rillway.runtime.identity.DefaultIdentityService;
import com.wegongdu.rillway.runtime.preview.ProcessPreviewer;
import com.wegongdu.rillway.runtime.preview.StaticProcessPreviewer;
import com.wegongdu.rillway.runtime.repository.BindingConfigRepository;
import com.wegongdu.rillway.runtime.repository.ExecutionHistoryRepository;
import com.wegongdu.rillway.runtime.repository.ProcessInstanceRepository;
import com.wegongdu.rillway.runtime.repository.TaskRepository;
import com.wegongdu.rillway.runtime.repository.memory.InMemoryBindingConfigRepository;
import com.wegongdu.rillway.runtime.repository.memory.InMemoryExecutionHistoryRepository;
import com.wegongdu.rillway.runtime.repository.memory.InMemoryProcessInstanceRepository;
import com.wegongdu.rillway.runtime.repository.memory.InMemoryTaskRepository;
import com.wegongdu.rillway.runtime.task.StandardTaskService;
import com.wegongdu.rillway.runtime.task.TaskService;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.JdbcTemplateAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;
import java.util.List;
import java.util.Optional;

/**
 * Spring Boot 3 auto-configuration for Rillway workflow engine.
 */
@AutoConfiguration(after = {
        DataSourceAutoConfiguration.class,
        JdbcTemplateAutoConfiguration.class
})
@EnableConfigurationProperties(RillwayProperties.class)
@ConditionalOnProperty(prefix = "rillway", name = "enabled", havingValue = "true", matchIfMissing = true)
public class RillwayAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public ObjectMapper rillwayObjectMapper() {
        return new ObjectMapper().findAndRegisterModules();
    }

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
    public IdentityService identityService() {
        return new DefaultIdentityService();
    }

    @Bean
    @ConditionalOnMissingBean
    public LlmClient llmClient(
            RillwayProperties properties,
            AiModelConfigRepository modelConfigRepository,
            ObjectProvider<com.wegongdu.rillway.ai.trace.LlmTraceSink> traceSinkProvider
    ) {
        com.wegongdu.rillway.ai.trace.LlmTraceSink traceSink = (properties.getAi().getTrace().isEnabled())
                ? traceSinkProvider.getIfAvailable()
                : null;

        // 1. Try database config repository
        if (modelConfigRepository != null) {
            Optional<AiModelConfig> defaultConfig = modelConfigRepository.findDefault();
            if (defaultConfig.isPresent() && defaultConfig.get().enabled()) {
                AiModelConfig config = defaultConfig.get();
                return new OpenAiCompatibleLlmClient(
                        config.baseUrl(),
                        config.apiKey(),
                        config.modelName(),
                        config.temperature(),
                        java.time.Duration.ofSeconds(config.timeoutSeconds()),
                        traceSink
                );
            }
        }

        // 2. Try application.yml rillway.ai.openai
        RillwayProperties.OpenAi openAi = properties.getAi().getOpenai();
        if (openAi.isEnabled() && openAi.getApiKey() != null && !openAi.getApiKey().isBlank()) {
            return new OpenAiCompatibleLlmClient(
                    openAi.getBaseUrl(),
                    openAi.getApiKey(),
                    openAi.getModel(),
                    openAi.getTemperature() != null ? openAi.getTemperature() : 0.1,
                    openAi.getTimeoutSeconds() != null ? java.time.Duration.ofSeconds(openAi.getTimeoutSeconds()) : java.time.Duration.ofSeconds(30),
                    traceSink
            );
        }

        // 3. Fallback to offline test fake client
        return new FakeLlmClient();
    }

    @Bean
    @ConditionalOnMissingBean
    public ResolutionCacheManager resolutionCacheManager(ResolutionCacheRepository resolutionCacheRepository) {
        return new ResolutionCacheManager(resolutionCacheRepository);
    }

    @Bean
    @ConditionalOnMissingBean
    public HumanAssigneeResolver humanAssigneeResolver(
            LlmClient llmClient,
            IdentityService identityService,
            ResolutionCacheManager resolutionCacheManager
    ) {
        return new AiAssigneeResolver(llmClient, identityService, resolutionCacheManager);
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
    public IntentInterpreter intentInterpreter(
            LlmClient llmClient,
            ObjectProvider<com.wegongdu.rillway.core.identity.OrgEntityRegistry> orgEntityRegistryProvider
    ) {
        return new LlmIntentInterpreter(llmClient, orgEntityRegistryProvider.getIfAvailable());
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

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass({JdbcTemplate.class, DataSource.class})
    @ConditionalOnBean(DataSource.class)
    static class JdbcPersistenceConfiguration {

        @Bean
        @ConditionalOnMissingBean
        public RillwayDatabaseInitializer rillwayDatabaseInitializer(DataSource dataSource) {
            return new RillwayDatabaseInitializer(new JdbcTemplate(dataSource), dataSource);
        }

        @Bean
        @ConditionalOnMissingBean
        public ExecutionHistoryRepository executionHistoryRepository(
                DataSource dataSource,
                ObjectMapper objectMapper,
                RillwayDatabaseInitializer initializer
        ) {
            return new JdbcExecutionHistoryRepository(new JdbcTemplate(dataSource), objectMapper);
        }

        @Bean
        @ConditionalOnMissingBean
        public ProcessInstanceRepository processInstanceRepository(
                DataSource dataSource,
                ExecutionHistoryRepository historyRepository,
                ObjectMapper objectMapper,
                RillwayDatabaseInitializer initializer
        ) {
            return new JdbcProcessInstanceRepository(new JdbcTemplate(dataSource), historyRepository, objectMapper);
        }

        @Bean
        @ConditionalOnMissingBean
        public TaskRepository taskRepository(
                DataSource dataSource,
                ObjectMapper objectMapper,
                RillwayDatabaseInitializer initializer
        ) {
            return new JdbcTaskRepository(new JdbcTemplate(dataSource), objectMapper);
        }

        @Bean
        @ConditionalOnMissingBean
        public BindingConfigRepository bindingConfigRepository(
                DataSource dataSource,
                RillwayDatabaseInitializer initializer
        ) {
            return new JdbcBindingConfigRepository(new JdbcTemplate(dataSource));
        }

        @Bean
        @ConditionalOnMissingBean
        public ResolutionCacheRepository resolutionCacheRepository(
                DataSource dataSource,
                ObjectMapper objectMapper,
                RillwayDatabaseInitializer initializer
        ) {
            return new JdbcResolutionCacheRepository(new JdbcTemplate(dataSource), objectMapper);
        }

        @Bean
        @ConditionalOnMissingBean
        public AiModelConfigRepository aiModelConfigRepository(
                DataSource dataSource,
                RillwayDatabaseInitializer initializer
        ) {
            return new JdbcAiModelConfigRepository(new JdbcTemplate(dataSource));
        }

        @Bean
        @ConditionalOnMissingBean
        public com.wegongdu.rillway.ai.trace.LlmTraceSink llmTraceSink(
                DataSource dataSource,
                RillwayDatabaseInitializer initializer
        ) {
            return new JdbcLlmTraceSink(new JdbcTemplate(dataSource));
        }
    }

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnMissingBean(DataSource.class)
    static class FallbackPersistenceConfiguration {

        @Bean
        @ConditionalOnMissingBean
        public ExecutionHistoryRepository executionHistoryRepository() {
            return new InMemoryExecutionHistoryRepository();
        }

        @Bean
        @ConditionalOnMissingBean
        public ProcessInstanceRepository processInstanceRepository() {
            return new InMemoryProcessInstanceRepository();
        }

        @Bean
        @ConditionalOnMissingBean
        public TaskRepository taskRepository() {
            return new InMemoryTaskRepository();
        }

        @Bean
        @ConditionalOnMissingBean
        public BindingConfigRepository bindingConfigRepository() {
            return new InMemoryBindingConfigRepository();
        }

        @Bean
        @ConditionalOnMissingBean
        public ResolutionCacheRepository resolutionCacheRepository() {
            return new InMemoryResolutionCacheRepository();
        }

        @Bean
        @ConditionalOnMissingBean
        public AiModelConfigRepository aiModelConfigRepository() {
            return new InMemoryAiModelConfigRepository();
        }

        @Bean
        @ConditionalOnMissingBean
        public com.wegongdu.rillway.ai.trace.LlmTraceSink llmTraceSink() {
            return new InMemoryLlmTraceSink();
        }
    }

    @Bean
    @ConditionalOnMissingBean
    public AuditSink auditSink(
            ObjectProvider<DataSource> dataSourceProvider,
            ObjectProvider<BindingConfigRepository> bindingConfigProvider,
            ObjectProvider<ProcessInstanceRepository> instanceRepoProvider,
            RillwayProperties properties
    ) {
        AuditSink rawSink = (properties.getAudit().isEnabled() && !"no-op".equalsIgnoreCase(properties.getAudit().getSink()))
                ? new InMemoryAuditSink()
                : NoOpAuditSink.INSTANCE;

        DataSource dataSource = dataSourceProvider.getIfAvailable();
        BindingConfigRepository bindingConfigRepository = bindingConfigProvider.getIfAvailable();
        ProcessInstanceRepository instanceRepository = instanceRepoProvider.getIfAvailable();

        if (dataSource != null && bindingConfigRepository != null && instanceRepository != null) {
            return new EntityStatusAutoUpdater(
                    new JdbcTemplate(dataSource),
                    bindingConfigRepository,
                    instanceRepository,
                    rawSink
            );
        }
        return rawSink;
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
    public TaskService taskService(
            TaskRepository taskRepository,
            ProcessInstanceRepository instanceRepository,
            ProcessEngine processEngine
    ) {
        return new StandardTaskService(taskRepository, instanceRepository, processEngine);
    }

    @Bean
    @ConditionalOnMissingBean
    public SpringApplicationEventPublisherBridge springApplicationEventPublisherBridge(
            org.springframework.context.ApplicationEventPublisher applicationEventPublisher
    ) {
        return new SpringApplicationEventPublisherBridge(applicationEventPublisher);
    }

    @Bean
    @ConditionalOnMissingBean
    public com.wegongdu.rillway.runtime.event.ProcessEventPublisher processEventPublisher(
            List<com.wegongdu.rillway.core.event.ProcessEventListener> listeners
    ) {
        return new com.wegongdu.rillway.runtime.event.CompositeProcessEventPublisher(listeners);
    }

    @Bean
    @ConditionalOnMissingBean
    public ProcessEngine processEngine(
            List<NodeExecutor<? extends Node>> executors,
            ProcessValidator validator,
            AuditSink auditSink,
            com.wegongdu.rillway.runtime.event.ProcessEventPublisher processEventPublisher,
            ProcessInstanceRepository instanceRepository,
            TaskRepository taskRepository,
            ExecutionHistoryRepository historyRepository,
            HumanAssigneeResolver assigneeResolver,
            BindingConfigRepository bindingConfigRepository,
            ObjectProvider<IntentInterpreter> intentInterpreterProvider
    ) {
        IntentInterpreter intentInterpreter = intentInterpreterProvider.getIfAvailable();
        java.util.function.Function<String, com.wegongdu.rillway.core.definition.ProcessDefinition> promptCompiler = (intentInterpreter != null)
                ? prompt -> intentInterpreter.interpret(com.wegongdu.rillway.ai.intent.ProcessIntent.of(prompt))
                : null;

        return new StandardProcessEngine(
                executors,
                validator,
                auditSink,
                processEventPublisher,
                instanceRepository,
                taskRepository,
                historyRepository,
                assigneeResolver,
                bindingConfigRepository,
                promptCompiler
        );
    }
}
