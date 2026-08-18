package com.wegongdu.rillway.example.listener;

import com.wegongdu.rillway.core.event.ProcessEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * 全局工作流生命周期监听器
 * 统一捕获流程实例发起、审批流转推进、实例完成及异常，支持对接钉钉/企微消息、监控与审计打点
 */
@Component
public class GlobalWorkflowEventListener {

    private static final Logger log = LoggerFactory.getLogger(GlobalWorkflowEventListener.class);

    @EventListener
    public void onProcessStarted(ProcessEvent.ProcessStartedEvent event) {
        log.info("📢 [全局流程事件] 流程实例启动: 流程ID={}, 实例ID={}, 单据Key={}, 发起人={}",
                event.definitionId(), event.processInstanceId(), event.businessKey(), event.initiator());
    }

    @EventListener
    public void onNodeCompleted(ProcessEvent.NodeCompletedEvent event) {
        log.info("⏩ [全局流程事件] 节点执行完成: 节点ID={}, 节点名称={}, 执行角色={}, 决策={}, 审批意见={}",
                event.nodeId(), event.nodeName(), event.actor(), event.decision().type(), event.decision().reason());
    }

    @EventListener
    public void onProcessCompleted(ProcessEvent.ProcessCompletedEvent event) {
        log.info("🏁 [全局流程事件] 流程全链路结束: 实例ID={}, 单据Key={}, 是否成功通过={}, 最终状态={}",
                event.processInstanceId(), event.businessKey(), event.isSuccess(), event.finalStatus());
    }
}
