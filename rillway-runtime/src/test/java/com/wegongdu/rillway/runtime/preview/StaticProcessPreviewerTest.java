package com.wegongdu.rillway.runtime.preview;

import com.wegongdu.rillway.core.context.ProcessContext;
import com.wegongdu.rillway.core.definition.ProcessDefinition;
import com.wegongdu.rillway.core.model.AgentAuthority;
import com.wegongdu.rillway.core.model.DecisionType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class StaticProcessPreviewerTest {

    private final ProcessPreviewer previewer = new StaticProcessPreviewer();

    @Test
    @DisplayName("should accurately preview path and approvers based on preview context")
    void should_preview_correct_path() {
        ProcessDefinition definition = ProcessDefinition.builder("preview-proc")
                .startNode("start")
                .ruleNode("amount-check", r -> r
                        .when("Below 5000", ctx -> ctx.getDecimal("amount").compareTo(new BigDecimal("5000")) < 0, "manager-approval")
                        .otherwise("agent-review")
                )
                .humanNode("manager-approval", h -> h.assigneeRole("DEPARTMENT_MANAGER"))
                .agentNode("agent-review", a -> a
                        .agentId("proc-agent")
                        .authority(AgentAuthority.DELEGATED)
                        .allowedDecisions(DecisionType.APPROVE)
                        .defaultTargetNodeId("end")
                )
                .endNode("end")
                .edge("start", "amount-check")
                .edge("manager-approval", "end")
                .build();

        // 1. Preview for small amount
        PreviewContext smallAmountCtx = PreviewContext.of("alice", ProcessContext.builder().variable("amount", new BigDecimal("2000")).build());
        ProcessPreview preview1 = previewer.preview(definition, smallAmountCtx);

        assertThat(preview1.potentialPath()).contains("start", "amount-check", "manager-approval", "end");
        assertThat(preview1.humanApprovers()).contains("Role: DEPARTMENT_MANAGER");

        // 2. Preview for large amount
        PreviewContext largeAmountCtx = PreviewContext.of("bob", ProcessContext.builder().variable("amount", new BigDecimal("10000")).build());
        ProcessPreview preview2 = previewer.preview(definition, largeAmountCtx);

        assertThat(preview2.potentialPath()).contains("start", "amount-check", "agent-review", "end");
        assertThat(preview2.agentNodes()).anyMatch(a -> a.contains("proc-agent"));
    }
}
