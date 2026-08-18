package com.wegongdu.rillway.example.hrm;

import com.wegongdu.rillway.core.event.ProcessEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * 监听 HRM 请假单审批全生命周期事件
 */
@Component
public class HrmLeaveEventListener {

    private static final Logger log = LoggerFactory.getLogger(HrmLeaveEventListener.class);

    @EventListener
    public void onProcessStarted(ProcessEvent.ProcessStartedEvent event) {
        log.info("🔔 [HRM 考勤事件] 请假流程发起成功: 实例ID={}, 单据Key={}, 发起人={}",
                event.processInstanceId(), event.businessKey(), event.initiator());
    }

    @EventListener
    public void onNodeCompleted(ProcessEvent.NodeCompletedEvent event) {
        log.info("📝 [HRM 考勤事件] 节点 [{}] 审批推进: 审批人={}, 决策={}, 意见={}",
                event.nodeName(), event.actor(), event.decision().type(), event.decision().reason());
    }

    @EventListener(condition = "#event.businessKey != null && #event.businessKey.startsWith('hrm_attendance_leave') && #event.isSuccess")
    public void onLeaveApproved(ProcessEvent.ProcessCompletedEvent event) {
        log.info("🎉 [HRM 考勤事件] 请假单 [{}] 终审批准通过！考勤系统已自动联动完成扣减！", event.businessKey());
    }

    @EventListener(condition = "#event.businessKey != null && #event.businessKey.startsWith('hrm_attendance_leave') && !#event.isSuccess")
    public void onLeaveRejected(ProcessEvent.ProcessCompletedEvent event) {
        log.warn("❌ [HRM 考勤事件] 请假单 [{}] 审批驳回，请重新发起！", event.businessKey());
    }
}
