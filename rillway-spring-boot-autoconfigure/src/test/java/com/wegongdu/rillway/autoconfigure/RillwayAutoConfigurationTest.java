package com.wegongdu.rillway.autoconfigure;

import com.wegongdu.rillway.agent.spi.AgentRegistry;
import com.wegongdu.rillway.ai.intent.IntentInterpreter;
import com.wegongdu.rillway.audit.sink.AuditSink;
import com.wegongdu.rillway.policy.spi.PolicyProvider;
import com.wegongdu.rillway.runtime.engine.ProcessEngine;
import com.wegongdu.rillway.runtime.preview.ProcessPreviewer;
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
        });
    }

    @Test
    @DisplayName("should not configure beans when rillway.enabled=false")
    void should_not_configure_when_disabled() {
        contextRunner.withPropertyValues("rillway.enabled=false")
                .run(context -> {
                    assertThat(context).doesNotHaveBean(ProcessEngine.class);
                });
    }
}
