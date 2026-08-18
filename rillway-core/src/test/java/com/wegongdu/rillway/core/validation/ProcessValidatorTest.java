package com.wegongdu.rillway.core.validation;

import com.wegongdu.rillway.core.definition.ProcessDefinition;
import com.wegongdu.rillway.core.model.AgentAuthority;
import com.wegongdu.rillway.core.model.DecisionType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ProcessValidatorTest {

    private final ProcessValidator validator = new StandardProcessValidator();

    @Test
    @DisplayName("should fail validation when definition has no start node")
    void should_fail_when_no_start_node() {
        ProcessDefinition definition = ProcessDefinition.builder("test-proc")
                .endNode("end")
                .build();

        ValidationResult result = validator.validate(definition);
        assertThat(result.isValid()).isFalse();
        assertThat(result.errors()).anyMatch(e -> e.code().equals("MISSING_START_NODE"));
    }

    @Test
    @DisplayName("should fail validation when definition has no end node")
    void should_fail_when_no_end_node() {
        ProcessDefinition definition = ProcessDefinition.builder("test-proc")
                .startNode("start")
                .build();

        ValidationResult result = validator.validate(definition);
        assertThat(result.isValid()).isFalse();
        assertThat(result.errors()).anyMatch(e -> e.code().equals("MISSING_END_NODE"));
    }

    @Test
    @DisplayName("should fail validation when agent node is missing required configuration")
    void should_fail_when_agent_node_is_invalid() {
        ProcessDefinition definition = ProcessDefinition.builder("test-proc")
                .startNode("start")
                .agentNode("agent-node", b -> b.agentId("").authority(null))
                .endNode("end")
                .edge("start", "agent-node")
                .edge("agent-node", "end")
                .build();

        ValidationResult result = validator.validate(definition);
        assertThat(result.isValid()).isFalse();
        assertThat(result.errors()).anyMatch(e -> e.code().equals("AGENT_ID_EMPTY"));
        assertThat(result.errors()).anyMatch(e -> e.code().equals("AGENT_AUTHORITY_NULL"));
        assertThat(result.errors()).anyMatch(e -> e.code().equals("AGENT_ALLOWED_DECISIONS_EMPTY"));
    }

    @Test
    @DisplayName("should fail validation when node is unreachable")
    void should_fail_when_unreachable_node_exists() {
        ProcessDefinition definition = ProcessDefinition.builder("test-proc")
                .startNode("start")
                .humanNode("isolated-node", b -> b.name("Isolated"))
                .endNode("end")
                .edge("start", "end")
                .build();

        ValidationResult result = validator.validate(definition);
        assertThat(result.isValid()).isFalse();
        assertThat(result.errors()).anyMatch(e -> e.code().equals("UNREACHABLE_NODE") && "isolated-node".equals(e.nodeId()));
    }

    @Test
    @DisplayName("should pass validation for valid workflow definition")
    void should_pass_for_valid_definition() {
        ProcessDefinition definition = ProcessDefinition.builder("purchase-proc")
                .startNode("start")
                .ruleNode("check-amount", b -> b
                        .when(ctx -> true, "agent-review")
                        .otherwise("manager-approval")
                )
                .agentNode("agent-review", b -> b
                        .agentId("procurement-agent")
                        .authority(AgentAuthority.DELEGATED)
                        .allowedDecisions(DecisionType.APPROVE, DecisionType.REJECT)
                        .defaultTargetNodeId("end")
                        .fallbackNodeId("manager-approval")
                )
                .humanNode("manager-approval", b -> b.assigneeRole("MANAGER"))
                .endNode("end")
                .edge("start", "check-amount")
                .edge("manager-approval", "end")
                .build();

        ValidationResult result = validator.validate(definition);
        assertThat(result.isValid()).isTrue();
        assertThat(result.errors()).isEmpty();
    }
}
