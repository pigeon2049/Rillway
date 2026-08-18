package com.wegongdu.rillway.example.listener;

import com.wegongdu.rillway.core.event.ProcessEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * HRM 考勤请假专属业务监听器
 * 演示使用 Spring SpEL 条件过滤表达式精准监听特定单据类型事件
 */
@Component
public class HrmLeaveEventListener {

    private static final Logger log = LoggerFactory.getLogger(HrmLeaveEventListener.class);

    // 🎯 仅监听以 hrm_attendance_leave 开头且终审通过的单据事件
    @EventListener(condition = "#event.businessKey != null && #event.businessKey.startsWith('hrm_attendance_leave') && #event.isSuccess")
    public void onLeaveApproved(ProcessEvent.ProcessCompletedEvent event) {
        log.info("🎉 [HRM 业务监听] 请假单 [{}] 终审批准通过！考勤系统已自动扣减剩余年假并同步打卡排班！", event.businessKey());
    }

    // 🎯 仅监听以 hrm_attendance_leave 开头且被驳回/不通过的单据事件
    @EventListener(condition = "#event.businessKey != null && #event.businessKey.startsWith('hrm_attendance_leave') && !#event.isSuccess")
    public void onLeaveRejected(ProcessEvent.ProcessCompletedEvent event) {
        log.warn("❌ [HRM 业务监听] 请假单 [{}] 审批不通过！已发送即时通知给申请人。", event.businessKey());
    }
}
