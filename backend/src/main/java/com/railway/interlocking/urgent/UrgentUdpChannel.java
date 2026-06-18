package com.railway.interlocking.urgent;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
@Component
public class UrgentUdpChannel {

    @Value("${urgent.udp.port:9876}")
    private int udpPort;

    @Value("${urgent.udp.plc-host:127.0.0.1}")
    private String plcHost;

    @Value("${urgent.udp.plc-port:9877}")
    private int plcPort;

    @Value("${urgent.udp.timeout-ms:3000}")
    private int timeoutMs;

    private DatagramSocket sendSocket;
    private DatagramSocket receiveSocket;
    private Thread receiveThread;
    private final AtomicBoolean running = new AtomicBoolean(false);

    private final Map<String, PlcCommandResult> pendingCommands = new ConcurrentHashMap<>();
    private final List<UrgentCommandLog> commandHistory = new ArrayList<>();

    private static final byte[] PLC_HEADER = new byte[]{(byte) 0xAA, (byte) 0x55, (byte) 0xEB, (byte) 0x90};
    private static final byte CMD_SWITCH_THROW = 0x01;
    private static final byte CMD_SIGNAL_FORCE = 0x02;
    private static final byte CMD_EMERGENCY_STOP = 0x03;
    private static final byte CMD_ACK = 0x06;

    @PostConstruct
    public void init() {
        try {
            sendSocket = new DatagramSocket();
            receiveSocket = new DatagramSocket(udpPort);
            running.set(true);

            receiveThread = new Thread(this::receiveLoop, "UrgentUDP-Receiver");
            receiveThread.setDaemon(true);
            receiveThread.start();

            log.warn("【非常站控】UDP紧急通信通道已启动: 本地端口={}, PLC目标={}:{}",
                    udpPort, plcHost, plcPort);
        } catch (Exception e) {
            log.error("【非常站控】UDP通道初始化失败", e);
        }
    }

    @PreDestroy
    public void destroy() {
        running.set(false);
        if (receiveThread != null) {
            receiveThread.interrupt();
        }
        if (sendSocket != null) {
            sendSocket.close();
        }
        if (receiveSocket != null) {
            receiveSocket.close();
        }
        log.info("【非常站控】UDP紧急通道已关闭");
    }

    public PlcCommandResult throwSwitch(String switchId, boolean isNormal, String authHash) {
        String cmdId = generateCommandId();
        log.warn("【非常站控】下发道岔强行拨转指令: switchId={}, 目标位置={}, cmdId={}",
                switchId, isNormal ? "定位" : "反位", cmdId);

        try {
            byte[] payload = buildSwitchThrowPayload(switchId, isNormal, authHash);
            byte[] packet = buildPacket(CMD_SWITCH_THROW, cmdId, payload);

            PlcCommandResult result = new PlcCommandResult();
            result.setCommandId(cmdId);
            result.setCommandType("道岔拨转");
            result.setTargetDevice(switchId);
            result.setSentTime(LocalDateTime.now());
            result.setStatus("PENDING");

            pendingCommands.put(cmdId, result);

            DatagramPacket datagram = new DatagramPacket(
                    packet, packet.length,
                    InetAddress.getByName(plcHost), plcPort
            );
            sendSocket.send(datagram);

            synchronized (result) {
                result.wait(timeoutMs);
            }

            pendingCommands.remove(cmdId);

            if ("ACK".equals(result.getStatus())) {
                result.setSuccess(true);
                result.setMessage("PLC已确认道岔拨转指令");
            } else if ("PENDING".equals(result.getStatus())) {
                result.setSuccess(false);
                result.setMessage("PLC响应超时");
                result.setStatus("TIMEOUT");
            }

            recordCommand(result);
            return result;

        } catch (Exception e) {
            log.error("【非常站控】道岔拨转指令发送失败: switchId={}", switchId, e);
            PlcCommandResult result = new PlcCommandResult();
            result.setCommandId(cmdId);
            result.setCommandType("道岔拨转");
            result.setTargetDevice(switchId);
            result.setSuccess(false);
            result.setStatus("ERROR");
            result.setMessage("发送失败: " + e.getMessage());
            result.setSentTime(LocalDateTime.now());
            recordCommand(result);
            return result;
        }
    }

    public PlcCommandResult forceSignal(String signalId, byte signalCode, String authHash) {
        String cmdId = generateCommandId();
        log.warn("【非常站控】下发信号机强开指令: signalId={}, 信号码={}, cmdId={}",
                signalId, signalCode, cmdId);

        try {
            byte[] payload = buildSignalForcePayload(signalId, signalCode, authHash);
            byte[] packet = buildPacket(CMD_SIGNAL_FORCE, cmdId, payload);

            PlcCommandResult result = new PlcCommandResult();
            result.setCommandId(cmdId);
            result.setCommandType("信号强开");
            result.setTargetDevice(signalId);
            result.setSentTime(LocalDateTime.now());
            result.setStatus("PENDING");

            pendingCommands.put(cmdId, result);

            DatagramPacket datagram = new DatagramPacket(
                    packet, packet.length,
                    InetAddress.getByName(plcHost), plcPort
            );
            sendSocket.send(datagram);

            synchronized (result) {
                result.wait(timeoutMs);
            }

            pendingCommands.remove(cmdId);

            if ("ACK".equals(result.getStatus())) {
                result.setSuccess(true);
                result.setMessage("PLC已确认信号强开指令");
            } else {
                result.setSuccess(false);
                result.setMessage("PLC响应超时或拒绝");
            }

            recordCommand(result);
            return result;

        } catch (Exception e) {
            log.error("【非常站控】信号强开指令发送失败: signalId={}", signalId, e);
            PlcCommandResult result = new PlcCommandResult();
            result.setCommandId(cmdId);
            result.setCommandType("信号强开");
            result.setTargetDevice(signalId);
            result.setSuccess(false);
            result.setStatus("ERROR");
            result.setMessage("发送失败: " + e.getMessage());
            result.setSentTime(LocalDateTime.now());
            recordCommand(result);
            return result;
        }
    }

    public PlcCommandResult emergencyStopAll(String authHash) {
        String cmdId = generateCommandId();
        log.warn("【非常站控】下发全站紧急停车指令: cmdId={}", cmdId);

        try {
            byte[] payload = authHash.getBytes("UTF-8");
            byte[] packet = buildPacket(CMD_EMERGENCY_STOP, cmdId, payload);

            PlcCommandResult result = new PlcCommandResult();
            result.setCommandId(cmdId);
            result.setCommandType("全站紧急停车");
            result.setTargetDevice("ALL");
            result.setSentTime(LocalDateTime.now());
            result.setStatus("PENDING");

            pendingCommands.put(cmdId, result);

            DatagramPacket datagram = new DatagramPacket(
                    packet, packet.length,
                    InetAddress.getByName(plcHost), plcPort
            );
            sendSocket.send(datagram);

            synchronized (result) {
                result.wait(timeoutMs * 2);
            }

            pendingCommands.remove(cmdId);

            if ("ACK".equals(result.getStatus())) {
                result.setSuccess(true);
                result.setMessage("PLC已确认全站紧急停车");
            } else {
                result.setSuccess(false);
                result.setMessage("紧急停车指令响应超时");
            }

            recordCommand(result);
            return result;

        } catch (Exception e) {
            log.error("【非常站控】紧急停车指令发送失败", e);
            PlcCommandResult result = new PlcCommandResult();
            result.setCommandId(cmdId);
            result.setCommandType("全站紧急停车");
            result.setSuccess(false);
            result.setStatus("ERROR");
            result.setMessage("发送失败: " + e.getMessage());
            result.setSentTime(LocalDateTime.now());
            recordCommand(result);
            return result;
        }
    }

    private void receiveLoop() {
        byte[] buffer = new byte[1024];
        while (running.get()) {
            try {
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                receiveSocket.receive(packet);

                byte[] data = new byte[packet.getLength()];
                System.arraycopy(packet.getData(), 0, data, 0, packet.getLength());

                processReceivedPacket(data, packet.getAddress().getHostAddress());

            } catch (Exception e) {
                if (running.get()) {
                    log.error("【非常站控】UDP接收异常", e);
                }
            }
        }
    }

    private void processReceivedPacket(byte[] data, String fromHost) {
        if (data.length < 8) {
            return;
        }

        if (!checkHeader(data)) {
            return;
        }

        byte cmdCode = data[4];
        if (cmdCode != CMD_ACK) {
            return;
        }

        byte cmdIdLen = data[5];
        String cmdId = new String(data, 6, cmdIdLen);

        PlcCommandResult pending = pendingCommands.get(cmdId);
        if (pending != null) {
            byte status = data[6 + cmdIdLen];
            pending.setStatus(status == 0 ? "ACK" : "NAK");
            pending.setAckTime(LocalDateTime.now());

            synchronized (pending) {
                pending.notifyAll();
            }

            log.info("【非常站控】收到PLC应答: cmdId={}, status={}", cmdId, pending.getStatus());
        }
    }

    private boolean checkHeader(byte[] data) {
        if (data.length < PLC_HEADER.length) {
            return false;
        }
        for (int i = 0; i < PLC_HEADER.length; i++) {
            if (data[i] != PLC_HEADER[i]) {
                return false;
            }
        }
        return true;
    }

    private byte[] buildPacket(byte cmdCode, String cmdId, byte[] payload) {
        byte[] cmdIdBytes = cmdId.getBytes();
        int totalLen = PLC_HEADER.length + 2 + cmdIdBytes.length + payload.length;

        byte[] packet = new byte[totalLen];
        System.arraycopy(PLC_HEADER, 0, packet, 0, PLC_HEADER.length);

        int offset = PLC_HEADER.length;
        packet[offset++] = cmdCode;
        packet[offset++] = (byte) cmdIdBytes.length;
        System.arraycopy(cmdIdBytes, 0, packet, offset, cmdIdBytes.length);
        offset += cmdIdBytes.length;

        System.arraycopy(payload, 0, packet, offset, payload.length);

        return packet;
    }

    private byte[] buildSwitchThrowPayload(String switchId, boolean isNormal, String authHash) {
        byte[] idBytes = switchId.getBytes();
        byte[] authBytes = authHash.getBytes();
        byte[] payload = new byte[2 + idBytes.length + authBytes.length];

        payload[0] = (byte) idBytes.length;
        System.arraycopy(idBytes, 0, payload, 1, idBytes.length);

        int offset = 1 + idBytes.length;
        payload[offset++] = (byte) (isNormal ? 0x01 : 0x02);
        System.arraycopy(authBytes, 0, payload, offset, authBytes.length);

        return payload;
    }

    private byte[] buildSignalForcePayload(String signalId, byte signalCode, String authHash) {
        byte[] idBytes = signalId.getBytes();
        byte[] authBytes = authHash.getBytes();
        byte[] payload = new byte[2 + idBytes.length + authBytes.length];

        payload[0] = (byte) idBytes.length;
        System.arraycopy(idBytes, 0, payload, 1, idBytes.length);

        int offset = 1 + idBytes.length;
        payload[offset++] = signalCode;
        System.arraycopy(authBytes, 0, payload, offset, authBytes.length);

        return payload;
    }

    private String generateCommandId() {
        return "URG_" + System.currentTimeMillis() + "_" +
                String.format("%04d", (int) (Math.random() * 10000));
    }

    private synchronized void recordCommand(PlcCommandResult result) {
        commandHistory.add(0, result);
        if (commandHistory.size() > 500) {
            commandHistory.remove(commandHistory.size() - 1);
        }
    }

    public synchronized List<UrgentCommandLog> getCommandHistory() {
        return new ArrayList<>(commandHistory);
    }

    public static class PlcCommandResult {
        private String commandId;
        private String commandType;
        private String targetDevice;
        private boolean success;
        private String status;
        private String message;
        private LocalDateTime sentTime;
        private LocalDateTime ackTime;

        public String getCommandId() { return commandId; }
        public void setCommandId(String commandId) { this.commandId = commandId; }
        public String getCommandType() { return commandType; }
        public void setCommandType(String commandType) { this.commandType = commandType; }
        public String getTargetDevice() { return targetDevice; }
        public void setTargetDevice(String targetDevice) { this.targetDevice = targetDevice; }
        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
        public LocalDateTime getSentTime() { return sentTime; }
        public void setSentTime(LocalDateTime sentTime) { this.sentTime = sentTime; }
        public LocalDateTime getAckTime() { return ackTime; }
        public void setAckTime(LocalDateTime ackTime) { this.ackTime = ackTime; }
    }

    public static class UrgentCommandLog {
        private String commandId;
        private String commandType;
        private String targetDevice;
        private boolean success;
        private String status;
        private String message;
        private LocalDateTime sentTime;
        private LocalDateTime ackTime;
        private String operator1;
        private String operator2;

        public UrgentCommandLog() {}

        public String getCommandId() { return commandId; }
        public void setCommandId(String commandId) { this.commandId = commandId; }
        public String getCommandType() { return commandType; }
        public void setCommandType(String commandType) { this.commandType = commandType; }
        public String getTargetDevice() { return targetDevice; }
        public void setTargetDevice(String targetDevice) { this.targetDevice = targetDevice; }
        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
        public LocalDateTime getSentTime() { return sentTime; }
        public void setSentTime(LocalDateTime sentTime) { this.sentTime = sentTime; }
        public LocalDateTime getAckTime() { return ackTime; }
        public void setAckTime(LocalDateTime ackTime) { this.ackTime = ackTime; }
        public String getOperator1() { return operator1; }
        public void setOperator1(String operator1) { this.operator1 = operator1; }
        public String getOperator2() { return operator2; }
        public void setOperator2(String operator2) { this.operator2 = operator2; }
    }
}
