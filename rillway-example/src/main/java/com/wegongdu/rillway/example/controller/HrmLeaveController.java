package com.wegongdu.rillway.example.controller;

import com.wegongdu.rillway.core.instance.ProcessInstance;
import com.wegongdu.rillway.example.model.CommonResult;
import com.wegongdu.rillway.example.model.LeaveApplyRequest;
import com.wegongdu.rillway.example.service.HrmLeaveService;
import com.wegongdu.rillway.runtime.preview.ProcessPreview;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * HRM 考勤请假单业务 API
 */
@Tag(name = "HRM 考勤请假单业务 API", description = "提供提交前审批链路预测预览与零代码请假单发起")
@RestController
@RequestMapping("/api/hrm/leave")
public class HrmLeaveController {

    @Autowired
    private HrmLeaveService hrmLeaveService;

    @Operation(summary = "1. 提交请假申请（自动匹配自然语言制度/流程流转）")
    @PostMapping("/apply")
    public CommonResult<com.wegongdu.rillway.example.model.ProcessInstanceDto> applyLeave(@RequestBody LeaveApplyRequest request) {
        ProcessInstance instance = hrmLeaveService.applyLeave(request);
        return CommonResult.success("请假申请已提交，流程已发起", com.wegongdu.rillway.example.model.ProcessInstanceDto.from(instance));
    }

    @Operation(summary = "2. 提交前预测预览审批流经路径与审批人（ProcessPreviewer）")
    @PostMapping("/preview")
    public CommonResult<ProcessPreview> previewLeave(@RequestBody LeaveApplyRequest request) {
        ProcessPreview preview = hrmLeaveService.previewLeaveWorkflow(request);
        return CommonResult.success("审批链路预测成功", preview);
    }
}
