package com.railway.interlocking.controller;

import com.railway.interlocking.dto.response.ApiResponse;
import com.railway.interlocking.urgent.DualAuthManager;
import com.railway.interlocking.urgent.EmergencyControlService;
import com.railway.interlocking.urgent.UrgentUdpChannel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/interlocking/emergency")
public class EmergencyControlController {

    private final EmergencyControlService emergencyControlService;

    public EmergencyControlController(EmergencyControlService emergencyControlService) {
        this.emergencyControlService = emergencyControlService;
    }

    /**
     * 请求鉴权挑战（第一步）
     */
    @PostMapping("/auth/challenge")
    public ApiResponse<Map<String, Object>> requestAuthChallenge(
            @RequestParam String operatorId,
            @RequestParam String operatorName) {
        log.warn("【非常站控】操作员请求鉴权挑战: operatorId={}, operatorName={}", operatorId, operatorName);
        Map<String, Object> result = emergencyControlService.requestAuthChallenge(operatorId, operatorName);
        return ApiResponse.success("获取鉴权挑战成功", result);
    }

    /**
     * 提交鉴权令牌（第二步）
     */
    @PostMapping("/auth/token")
    public ApiResponse<Map<String, Object>> submitAuthToken(
            @RequestParam String token,
            @RequestParam String signature) {
        log.warn("【非常站控】提交鉴权令牌: token={}", token);
        Map<String, Object> result = emergencyControlService.submitAuthToken(token, signature);
        boolean success = (Boolean) result.get("success");
        return success
                ? ApiResponse.success("鉴权成功", result)
                : ApiResponse.error("AUTH_FAILED", "鉴权失败", result);
    }

    /**
     * 获取非常站控模式状态
     */
    @GetMapping("/status")
    public ApiResponse<Map<String, Object>> getEmergencyStatus() {
        Map<String, Object> status = emergencyControlService.getEmergencyStatus();
        return ApiResponse.success("获取成功", status);
    }

    /**
     * 紧急拨转道岔（绕过联锁校验）
     */
    @PostMapping("/switch/throw")
    public ApiResponse<Map<String, Object>> emergencyThrowSwitch(
            @RequestParam String switchId,
            @RequestParam boolean toNormal,
            @RequestParam(defaultValue = "") String remark) {
        log.warn("【非常站控】紧急拨转道岔请求: switchId={}, toNormal={}, remark={}", switchId, toNormal, remark);
        Map<String, Object> result = emergencyControlService.emergencyThrowSwitch(switchId, toNormal, remark);
        boolean success = (Boolean) result.get("success");
        return success
                ? ApiResponse.success("道岔拨转指令已下发", result)
                : ApiResponse.error("EMERGENCY_OPERATION_FAILED", (String) result.get("error"), result);
    }

    /**
     * 紧急设置信号机（绕过联锁校验）
     */
    @PostMapping("/signal/force")
    public ApiResponse<Map<String, Object>> emergencyForceSignal(
            @RequestParam String signalId,
            @RequestParam String aspect,
            @RequestParam(defaultValue = "") String remark) {
        log.warn("【非常站控】紧急设置信号机: signalId={}, aspect={}", signalId, aspect);
        try {
            com.railway.interlocking.model.enums.SignalAspect signalAspect =
                    com.railway.interlocking.model.enums.SignalAspect.valueOf(aspect);
            Map<String, Object> result = emergencyControlService.emergencyForceSignal(
                    signalId, signalAspect, remark);
            boolean success = (Boolean) result.get("success");
            return success
                    ? ApiResponse.success("信号机强开指令已下发", result)
                    : ApiResponse.error("EMERGENCY_OPERATION_FAILED", (String) result.get("error"), result);
        } catch (IllegalArgumentException e) {
            return ApiResponse.error("INVALID_ASPECT", "无效的信号显示: " + aspect, null);
        }
    }

    /**
     * 全站紧急停车
     */
    @PostMapping("/stop-all")
    public ApiResponse<Map<String, Object>> emergencyStopAll(
            @RequestParam(defaultValue = "") String remark) {
        log.warn("【非常站控】全站紧急停车请求: remark={}", remark);
        Map<String, Object> result = emergencyControlService.emergencyStopAll(remark);
        boolean success = (Boolean) result.get("success");
        return success
                ? ApiResponse.success("全站紧急停车指令已下发", result)
                : ApiResponse.error("EMERGENCY_STOP_FAILED", (String) result.get("error"), result);
    }

    /**
     * 解除非常站控模式
     */
    @PostMapping("/deactivate")
    public ApiResponse<Map<String, Object>> deactivateEmergencyMode(
            @RequestParam String operator,
            @RequestParam(defaultValue = "正常恢复") String reason) {
        log.warn("【非常站控】解除非常站控模式: operator={}, reason={}", operator, reason);
        Map<String, Object> result = emergencyControlService.deactivateEmergencyMode(operator, reason);
        return ApiResponse.success("非常站控模式已解除", result);
    }

    /**
     * 获取紧急操作日志
     */
    @GetMapping("/logs")
    public ApiResponse<List<DualAuthManager.UrgentOperationLog>> getEmergencyLogs() {
        List<DualAuthManager.UrgentOperationLog> logs = emergencyControlService.getEmergencyOperationLogs();
        return ApiResponse.success("获取成功", logs);
    }

    /**
     * 获取 UDP 紧急命令历史
     */
    @GetMapping("/udp-history")
    public ApiResponse<List<UrgentUdpChannel.UrgentCommandLog>> getUrgentCommandHistory() {
        List<UrgentUdpChannel.UrgentCommandLog> history = emergencyControlService.getUrgentCommandHistory();
        return ApiResponse.success("获取成功", history);
    }
}
