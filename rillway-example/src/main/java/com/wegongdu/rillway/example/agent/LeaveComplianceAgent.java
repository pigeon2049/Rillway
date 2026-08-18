package com.wegongdu.rillway.example.agent;

import com.wegongdu.rillway.agent.model.AgentContext;
import com.wegongdu.rillway.agent.model.AgentDecision;
import com.wegongdu.rillway.agent.spi.Agent;
import com.wegongdu.rillway.core.actor.Actor;
import com.wegongdu.rillway.core.decision.ApproveDecision;
import com.wegongdu.rillway.core.decision.RejectDecision;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * 示例 AI 智能体：HRM 请假合规初审 Agent
 * 可在流程编排中作为 AgentNode 执行全自动智能前置审查
 */
@Component
public class LeaveComplianceAgent implements Agent {

    private static final Logger log = LoggerFactory.getLogger(LeaveComplianceAgent.class);

    @Override
    public String id() {
        return "leave_compliance_agent";
    }

    @Override
    public AgentDecision decide(AgentContext context) {
        String reason = (String) context.variables().get("reason");
        Object dayObj = context.variables().get("day");
        log.info("🤖 [AI 合规审查 Agent] 正在智能评估请假事由: '{}', 请假天数: '{}'", reason, dayObj);

        // 模拟智能违规拦截规则：理由为空或包含敏感关键词时驳回
        if (reason == null || reason.trim().length() < 2) {
            log.warn("⚠️ [AI 合规审查] 请假事由过于简略，自动驳回");
            return AgentDecision.of(RejectDecision.of(
                    Actor.AgentActor.of(id()),
                    "AI合规拦截：请假事由不得少于2个字符，请补充详细说明"
            ));
        }

        // 智能通过
        return AgentDecision.of(ApproveDecision.of(
                Actor.AgentActor.of(id()),
                "AI智能初审合规：请假类型与事由格式完整合法"
        ));
    }
}
