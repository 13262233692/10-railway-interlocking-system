package com.railway.interlocking.urgent;

import com.railway.interlocking.dto.WebSocketMessage;
import com.railway.interlocking.model.*;
import com.railway.interlocking.model.enums.*;
import com.railway.interlocking.service.InterlockingService;
import com.railway.interlocking.service.WebSocketService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@Service
public class EmergencyControlService {

    private final DualAuthManager dualAuthManager;
    private final UrgentUdpChannel urgentUdpChannel;
    private final InterlockingService interlockingService;
    private final WebSocketService webSocketService;

    private static final String STATION_ID = "STATION_001";

    public EmergencyControlService(DualAuthManager dualAuthManager,
                                    UrgentUdpChannel urgentUdpChannel,
                                    InterlockingService interlockingService,
                                    WebSocketService webSocketService) {
        this.dualAuthManager = dualAuthManager;
        this.urgentUdpChannel = urgentUdpChannel;
        this.interlockingService = interlockingService;
        this.webSocketService = webSocketService;
    }

    public Map<String, Object> requestAuthChallenge(String operatorId, String operatorName) {
        Map<String, Object> result = new HashMap<>();
        String challenge = dualAuthManager.requestAuthChallenge(operatorId, operatorName);
        result.put("success", true);
        result.put("challenge", challenge);
        result.put("operatorName", operatorName);
        result.put("timestamp", LocalDateTime.now());
        return result;
    }

    public Map<String, Object> submitAuthToken(String token, String signature) {
        Map<String, Object> result = new HashMap<>();
        boolean success = dualAuthManager.submitAuthToken(token, signature);
        result.put("success", success);

        if (success) {
            result.put("authPosition",
                    dualAuthManager.getOperator2() != null ? "OPERATOR_2" : "OPERATOR_1");
            result.put("emergencyModeActive", dualAuthManager.isEmergencyModeActive());
            result.put("operator1", dualAuthManager.getOperator1());
            result.put("operator2", dualAuthManager.getOperator2());

            if (dualAuthManager.isEmergencyModeActive()) {
                broadcastEmergencyModeActivation();
            }
        } else {
            result.put("error", "鉴权失败，签名验证不通过");
        }

        return result;
    }

    public Map<String, Object> getEmergencyStatus() {
        Map<String, Object> status = new HashMap<>();
        status.put("emergencyModeActive", dualAuthManager.isEmergencyModeActive());
        status.put("operator1", dualAuthManager.getOperator1());
        status.put("operator2", dualAuthManager.getOperator2());
        status.put("authEstablishedTime", dualAuthManager.getAuthEstablishedTime());
        status.put("authHash", dualAuthManager.getCurrentAuthHash());
        status.put("authValid", dualAuthManager.isAuthValid());

        long remainingSeconds = 0;
        if (dualAuthManager.getAuthEstablishedTime() != null) {
            long elapsed = java.time.Duration.between(
                    dualAuthManager.getAuthEstablishedTime(), LocalDateTime.now()).getSeconds();
            remainingSeconds = Math.max(0, 300 - elapsed);
        }
        status.put("remainingValidSeconds", remainingSeconds);

        return status;
    }

    public Map<String, Object> emergencyThrowSwitch(String switchId, boolean toNormal, String remark) {
        Map<String, Object> result = new HashMap<>();

        if (!dualAuthManager.isEmergencyModeActive()) {
            result.put("success", false);
            result.put("error", "非常站控模式未激活，无法执行紧急操作");
            return result;
        }

        if (!dualAuthManager.isAuthValid()) {
            result.put("success", false);
            result.put("error", "双签核凭据已过期，请重新签核");
            return result;
        }

        Switch sw = interlockingService.getSwitch(switchId);
        if (sw == null) {
            result.put("success", false);
            result.put("error", "道岔不存在: " + switchId);
            return result;
        }

        log.warn("【非常站控】紧急拨转道岔: switchId={}, 目标位置={}, 备注={}",
                switchId, toNormal ? "定位" : "反位", remark);

        String authHash = dualAuthManager.getCurrentAuthHash();
        UrgentUdpChannel.PlcCommandResult plcResult =
                urgentUdpChannel.throwSwitch(switchId, toNormal, authHash);

        if (plcResult.isSuccess()) {
            try {
                SwitchPosition targetPos = toNormal ? SwitchPosition.NORMAL : SwitchPosition.REVERSE;
                sw.setPosition(targetPos);
                sw.setLastUpdateTime(LocalDateTime.now());
                sw.setStatusRemark("非常站控模式下强制拨转: " + remark);

                log.warn("【非常站控】道岔状态已更新: switchId={}, newPos={}", switchId, targetPos);
            } catch (Exception e) {
                log.error("【非常站控】更新道岔内存状态失败", e);
            }
        }

        dualAuthManager.recordOperation(
                "EMERGENCY_SWITCH_THROW",
                switchId,
                plcResult.isSuccess() ? "SUCCESS" : "FAILED",
                remark
        );

        dualAuthManager.extendAuthValidity();

        result.put("success", plcResult.isSuccess());
        result.put("commandId", plcResult.getCommandId());
        result.put("switchId", switchId);
        result.put("targetPosition", toNormal ? "NORMAL" : "REVERSE");
        result.put("status", plcResult.getStatus());
        result.put("message", plcResult.getMessage());
        result.put("sentTime", plcResult.getSentTime());
        result.put("ackTime", plcResult.getAckTime());
        result.put("authHash", authHash);

        broadcastEmergencyOperation("道岔紧急拨转", switchId,
                (toNormal ? "定位" : "反位") + " - " + (plcResult.isSuccess() ? "成功" : "失败"));

        return result;
    }

    public Map<String, Object> emergencyForceSignal(String signalId, SignalAspect aspect, String remark) {
        Map<String, Object> result = new HashMap<>();

        if (!dualAuthManager.isEmergencyModeActive()) {
            result.put("success", false);
            result.put("error", "非常站控模式未激活，无法执行紧急操作");
            return result;
        }

        Signal signal = interlockingService.getSignal(signalId);
        if (signal == null) {
            result.put("success", false);
            result.put("error", "信号机不存在: " + signalId);
            return result;
        }

        byte signalCode = aspectToCode(aspect);
        String authHash = dualAuthManager.getCurrentAuthHash();

        log.warn("【非常站控】紧急强开信号: signalId={}, aspect={}, remark={}",
                signalId, aspect, remark);

        UrgentUdpChannel.PlcCommandResult plcResult =
                urgentUdpChannel.forceSignal(signalId, signalCode, authHash);

        if (plcResult.isSuccess()) {
            try {
                signal.setAspect(aspect);
                signal.setLastUpdateTime(LocalDateTime.now());
                signal.setStatusRemark("非常站控模式下强制设置: " + remark);
            } catch (Exception e) {
                log.error("【非常站控】更新信号机内存状态失败", e);
            }
        }

        dualAuthManager.recordOperation(
                "EMERGENCY_SIGNAL_FORCE",
                signalId,
                plcResult.isSuccess() ? "SUCCESS" : "FAILED",
                remark
        );

        dualAuthManager.extendAuthValidity();

        result.put("success", plcResult.isSuccess());
        result.put("commandId", plcResult.getCommandId());
        result.put("signalId", signalId);
        result.put("aspect", aspect);
        result.put("status", plcResult.getStatus());
        result.put("message", plcResult.getMessage());
        result.put("authHash", authHash);

        broadcastEmergencyOperation("信号机强制设置", signalId,
                aspect.getDescription() + " - " + (plcResult.isSuccess() ? "成功" : "失败"));

        return result;
    }

    public Map<String, Object> emergencyStopAll(String remark) {
        Map<String, Object> result = new HashMap<>();

        if (!dualAuthManager.isEmergencyModeActive()) {
            result.put("success", false);
            result.put("error", "非常站控模式未激活，无法执行紧急操作");
            return result;
        }

        log.warn("【非常站控】全站紧急停车: remark={}", remark);

        String authHash = dualAuthManager.getCurrentAuthHash();
        UrgentUdpChannel.PlcCommandResult plcResult = urgentUdpChannel.emergencyStopAll(authHash);

        dualAuthManager.recordOperation(
                "EMERGENCY_STOP_ALL",
                "ALL_STATION",
                plcResult.isSuccess() ? "SUCCESS" : "FAILED",
                remark
        );

        result.put("success", plcResult.isSuccess());
        result.put("commandId", plcResult.getCommandId());
        result.put("status", plcResult.getStatus());
        result.put("message", plcResult.getMessage());
        result.put("authHash", authHash);

        broadcastEmergencyOperation("全站紧急停车", "全站",
                plcResult.isSuccess() ? "已执行" : "执行失败");

        return result;
    }

    public Map<String, Object> deactivateEmergencyMode(String operator, String reason) {
        Map<String, Object> result = new HashMap<>();

        log.warn("【非常站控】解除非常站控模式: operator={}, reason={}", operator, reason);

        dualAuthManager.recordOperation(
                "DEACTIVATE_EMERGENCY_MODE",
                "SYSTEM",
                "SUCCESS",
                "解除原因: " + reason
        );

        dualAuthManager.deactivateEmergencyMode(operator);

        result.put("success", true);
        result.put("message", "非常站控模式已解除");
        result.put("operator", operator);
        result.put("reason", reason);

        broadcastEmergencyModeDeactivation(reason);

        return result;
    }

    public List<DualAuthManager.UrgentOperationLog> getEmergencyOperationLogs() {
        return new ArrayList<>(dualAuthManager.getOperationLog().values());
    }

    public List<UrgentUdpChannel.UrgentCommandLog> getUrgentCommandHistory() {
        return urgentUdpChannel.getCommandHistory();
    }

    private byte aspectToCode(SignalAspect aspect) {
        switch (aspect) {
            case RED: return 0x01;
            case YELLOW: return 0x02;
            case GREEN: return 0x03;
            case DOUBLE_YELLOW: return 0x04;
            default: return 0x00;
        }
    }

    private void broadcastEmergencyModeActivation() {
        try {
            Map<String, Object> data = getEmergencyStatus();
            WebSocketMessage<Map<String, Object>> message = WebSocketMessage.<Map<String, Object>>builder()
                    .type("EMERGENCY_MODE_ACTIVATED")
                    .title("【紧急警报】非常站控模式已激活")
                    .content("双操作员签核完成，非常站控模式已启动。所有联锁校验已绕过，" +
                            "直接通过 UDP 通道下发 PLC 物理层指令。请谨慎操作！")
                    .data(data)
                    .level("WARNING")
                    .timestamp(LocalDateTime.now())
                    .stationId(STATION_ID)
                    .build();
            webSocketService.broadcastMessage(message);
        } catch (Exception e) {
            log.error("广播非常站控激活消息失败", e);
        }
    }

    private void broadcastEmergencyModeDeactivation(String reason) {
        try {
            Map<String, Object> data = new HashMap<>();
            data.put("reason", reason);
            WebSocketMessage<Map<String, Object>> message = WebSocketMessage.<Map<String, Object>>builder()
                    .type("EMERGENCY_MODE_DEACTIVATED")
                    .title("非常站控模式已解除")
                    .content("解除原因: " + reason)
                    .data(data)
                    .level("INFO")
                    .timestamp(LocalDateTime.now())
                    .stationId(STATION_ID)
                    .build();
            webSocketService.broadcastMessage(message);
        } catch (Exception e) {
            log.error("广播非常站控解除消息失败", e);
        }
    }

    private void broadcastEmergencyOperation(String operation, String target, String result) {
        try {
            Map<String, Object> data = new HashMap<>();
            data.put("operation", operation);
            data.put("target", target);
            data.put("result", result);
            data.put("operator1", dualAuthManager.getOperator1());
            data.put("operator2", dualAuthManager.getOperator2());
            data.put("authHash", dualAuthManager.getCurrentAuthHash());

            WebSocketMessage<Map<String, Object>> message = WebSocketMessage.<Map<String, Object>>builder()
                    .type("EMERGENCY_OPERATION")
                    .title("【非常站控】紧急操作执行")
                    .content(operation + ": " + target + " -> " + result)
                    .data(data)
                    .level("WARNING")
                    .timestamp(LocalDateTime.now())
                    .stationId(STATION_ID)
                    .build();
            webSocketService.broadcastMessage(message);
        } catch (Exception e) {
            log.error("广播紧急操作消息失败", e);
        }
    }
}
