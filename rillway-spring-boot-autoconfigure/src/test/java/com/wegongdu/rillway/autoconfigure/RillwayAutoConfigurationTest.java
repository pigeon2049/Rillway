package com.wegongdu.rillway.autoconfigure;

import com.wegongdu.rillway.agent.spi.AgentRegistry;
import com.wegongdu.rillway.ai.intent.IntentInterpreter;
import com.wegongdu.rillway.ai.llm.LlmClient;
import com.wegongdu.rillway.audit.sink.AuditSink;
import com.wegongdu.rillway.core.identity.HumanAssigneeResolver;
import com.wegongdu.rillway.core.identity.IdentityService;
import com.wegongdu.rillway.policy.spi.PolicyProvider;
import com.wegongdu.rillway.runtime.engine.ProcessEngine;
import com.wegongdu.rillway.runtime.preview.ProcessPreviewer;
import com.wegongdu.rillway.runtime.repository.BindingConfigRepository;
import com.wegongdu.rillway.runtime.repository.ProcessInstanceRepository;
import com.wegongdu.rillway.runtime.repository.TaskRepository;
import com.wegongdu.rillway.runtime.task.TaskService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

class RillwayAutoConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(RillwayAutoConfiguration.class));

    @Test
    @DisplayName("should auto-configure all core beans when enabled")
    void should_configure_core_beans() {
        contextRunner.run(context -> {
            assertThat(context).hasSingleBean(ProcessEngine.class);
            assertThat(context).hasSingleBean(AgentRegistry.class);
            assertThat(context).hasSingleBean(PolicyProvider.class);
            assertThat(context).hasSingleBean(AuditSink.class);
            assertThat(context).hasSingleBean(ProcessPreviewer.class);
            assertThat(context).hasSingleBean(IntentInterpreter.class);
            assertThat(context).hasSingleBean(ProcessInstanceRepository.class);
            assertThat(context).hasSingleBean(TaskRepository.class);
            assertThat(context).hasSingleBean(BindingConfigRepository.class);
            assertThat(context).hasSingleBean(IdentityService.class);
            assertThat(context).hasSingleBean(LlmClient.class);
            assertThat(context).hasSingleBean(com.wegongdu.rillway.ai.cache.ResolutionCacheRepository.class);
            assertThat(context).hasSingleBean(com.wegongdu.rillway.ai.cache.ResolutionCacheManager.class);
            assertThat(context).hasSingleBean(HumanAssigneeResolver.class);
            assertThat(context).hasSingleBean(TaskService.class);
            assertThat(context).hasSingleBean(com.wegongdu.rillway.ai.config.AiModelConfigRepository.class);
            assertThat(context.getBean(LlmClient.class)).isInstanceOf(com.wegongdu.rillway.ai.llm.FakeLlmClient.class);
        });
    }

    @Test
    @DisplayName("should configure OpenAiCompatibleLlmClient when rillway.ai.openai.api-key is configured")
    void should_configure_openai_llm_client_when_properties_provided() {
        contextRunner.withPropertyValues(
                "rillway.ai.openai.enabled=true",
                "rillway.ai.openai.api-key=sk-test-key-123456",
                "rillway.ai.openai.base-url=https://api.deepseek.com/v1",
                "rillway.ai.openai.model=deepseek-chat"
        ).run(context -> {
            assertThat(context).hasSingleBean(LlmClient.class);
            assertThat(context.getBean(LlmClient.class)).isInstanceOf(com.wegongdu.rillway.ai.llm.OpenAiCompatibleLlmClient.class);
        });
    }

    @Test
    @DisplayName("should configure OpenAiCompatibleLlmClient when database has active default config")
    void should_configure_openai_llm_client_when_db_config_present() {
        contextRunner.withBean(com.wegongdu.rillway.ai.config.AiModelConfigRepository.class, () -> {
            var repo = new com.wegongdu.rillway.autoconfigure.persistence.InMemoryAiModelConfigRepository();
            repo.save(com.wegongdu.rillway.ai.config.AiModelConfig.builder("cfg_01")
                    .providerName("deepseek")
                    .baseUrl("https://api.deepseek.com/v1")
                    .apiKey("sk-db-key")
                    .modelName("deepseek-chat")
                    .isDefault(true)
                    .enabled(true)
                    .build());
            return repo;
        }).run(context -> {
            assertThat(context).hasSingleBean(LlmClient.class);
            assertThat(context.getBean(LlmClient.class)).isInstanceOf(com.wegongdu.rillway.ai.llm.OpenAiCompatibleLlmClient.class);
        });
    }

    @Test
    @DisplayName("should not configure beans when rillway.enabled=false")
    void should_not_configure_when_disabled() {
        contextRunner.withPropertyValues("rillway.enabled=false")
                .run(context -> {
                    assertThat(context).doesNotHaveBean(ProcessEngine.class);
                    assertThat(context).doesNotHaveBean(TaskService.class);
                });
    }
}
