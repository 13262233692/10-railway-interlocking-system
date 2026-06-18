package com.railway.interlocking.urgent;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
@Component
public class DualAuthManager {

    private static final String HMAC_ALGORITHM = "HmacSHA256";
    private static final String SECRET_KEY = "RAILWAY_URGENT_CONTROL_SECRET_KEY_2024";
    private static final long AUTH_TOKEN_TTL_SECONDS = 300;

    private final AtomicBoolean emergencyModeActive = new AtomicBoolean(false);
    private volatile String currentOperator1 = null;
    private volatile String currentOperator2 = null;
    private volatile String currentAuthHash = null;
    private volatile LocalDateTime authEstablishedTime = null;

    private final Map<String, AuthToken> pendingAuthTokens = new ConcurrentHashMap<>();
    private final Map<String, UrgentOperationLog> operationLog = new ConcurrentHashMap<>();

    public static class AuthToken {
        private String token;
        private String operatorId;
        private String operatorName;
        private LocalDateTime createTime;
        private String challenge;

        public AuthToken(String token, String operatorId, String operatorName, String challenge) {
            this.token = token;
            this.operatorId = operatorId;
            this.operatorName = operatorName;
            this.createTime = LocalDateTime.now();
            this.challenge = challenge;
        }

        public String getToken() { return token; }
        public String getOperatorId() { return operatorId; }
        public String getOperatorName() { return operatorName; }
        public LocalDateTime getCreateTime() { return createTime; }
        public String getChallenge() { return challenge; }
    }

    public static class UrgentOperationLog {
        private String logId;
        private String operationType;
        private String targetDevice;
        private String operator1;
        private String operator2;
        private String authHash;
        private LocalDateTime operateTime;
        private String result;
        private String remark;

        public UrgentOperationLog() {}

        public String getLogId() { return logId; }
        public void setLogId(String logId) { this.logId = logId; }
        public String getOperationType() { return operationType; }
        public void setOperationType(String operationType) { this.operationType = operationType; }
        public String getTargetDevice() { return targetDevice; }
        public void setTargetDevice(String targetDevice) { this.targetDevice = targetDevice; }
        public String getOperator1() { return operator1; }
        public void setOperator1(String operator1) { this.operator1 = operator1; }
        public String getOperator2() { return operator2; }
        public void setOperator2(String operator2) { this.operator2 = operator2; }
        public String getAuthHash() { return authHash; }
        public void setAuthHash(String authHash) { this.authHash = authHash; }
        public LocalDateTime getOperateTime() { return operateTime; }
        public void setOperateTime(LocalDateTime operateTime) { this.operateTime = operateTime; }
        public String getResult() { return result; }
        public void setResult(String result) { this.result = result; }
        public String getRemark() { return remark; }
        public void setRemark(String remark) { this.remark = remark; }
    }

    public String requestAuthChallenge(String operatorId, String operatorName) {
        String challenge = generateChallenge(operatorId);
        String token = "AUTH_" + operatorId + "_" + System.currentTimeMillis();

        pendingAuthTokens.put(token, new AuthToken(token, operatorId, operatorName, challenge));

        log.warn("【双签核】操作员申请鉴权挑战: operatorId={}, operatorName={}", operatorId, operatorName);
        return challenge;
    }

    public boolean submitAuthToken(String token, String signature) {
        AuthToken authToken = pendingAuthTokens.get(token);
        if (authToken == null) {
            log.warn("【双签核】鉴权令牌无效: token={}", token);
            return false;
        }

        if (isTokenExpired(authToken)) {
            pendingAuthTokens.remove(token);
            log.warn("【双签核】鉴权令牌已过期: operator={}", authToken.getOperatorName());
            return false;
        }

        String expectedSignature = computeSignature(authToken.getChallenge());
        boolean valid = MessageDigest.isEqual(
                signature.getBytes(StandardCharsets.UTF_8),
                expectedSignature.getBytes(StandardCharsets.UTF_8)
        );

        if (!valid) {
            log.warn("【双签核】签名验证失败: operator={}", authToken.getOperatorName());
            return false;
        }

        if (currentOperator1 == null) {
            currentOperator1 = authToken.getOperatorName();
            log.warn("【双签核】第1位操作员签核通过: {}", currentOperator1);
        } else if (currentOperator2 == null
                && !authToken.getOperatorId().equals(authToken.getOperatorId())) {
            currentOperator2 = authToken.getOperatorName();
            authEstablishedTime = LocalDateTime.now();
            currentAuthHash = computeDualAuthHash(currentOperator1, currentOperator2);
            emergencyModeActive.set(true);

            log.warn("【双签核】第2位操作员签核通过，非常站控模式已激活");
            log.warn("【双签核】双签核凭据哈希: {}", currentAuthHash);
        } else {
            log.warn("【双签核】无效的签核位置或同一名操作员重复签核");
            return false;
        }

        pendingAuthTokens.remove(token);
        return true;
    }

    public boolean isEmergencyModeActive() {
        return emergencyModeActive.get() && isAuthValid();
    }

    public boolean isAuthValid() {
        if (authEstablishedTime == null) {
            return false;
        }
        long elapsed = java.time.Duration.between(authEstablishedTime, LocalDateTime.now()).getSeconds();
        return elapsed < AUTH_TOKEN_TTL_SECONDS;
    }

    public String getCurrentAuthHash() {
        return currentAuthHash;
    }

    public String getOperator1() {
        return currentOperator1;
    }

    public String getOperator2() {
        return currentOperator2;
    }

    public LocalDateTime getAuthEstablishedTime() {
        return authEstablishedTime;
    }

    public void deactivateEmergencyMode(String operator) {
        log.warn("【双签核】解除非常站控模式: operator={}", operator);
        emergencyModeActive.set(false);
        currentOperator1 = null;
        currentOperator2 = null;
        currentAuthHash = null;
        authEstablishedTime = null;
        pendingAuthTokens.clear();
    }

    public UrgentOperationLog recordOperation(String operationType, String targetDevice,
                                               String result, String remark) {
        UrgentOperationLog logEntry = new UrgentOperationLog();
        logEntry.setLogId("LOG_URG_" + System.currentTimeMillis() + "_" +
                String.format("%04d", (int) (Math.random() * 10000)));
        logEntry.setOperationType(operationType);
        logEntry.setTargetDevice(targetDevice);
        logEntry.setOperator1(currentOperator1);
        logEntry.setOperator2(currentOperator2);
        logEntry.setAuthHash(currentAuthHash);
        logEntry.setOperateTime(LocalDateTime.now());
        logEntry.setResult(result);
        logEntry.setRemark(remark);

        operationLog.put(logEntry.getLogId(), logEntry);

        log.warn("【非常操作日志】记录紧急操作: logId={}, type={}, device={}, result={}",
                logEntry.getLogId(), operationType, targetDevice, result);

        return logEntry;
    }

    private String generateChallenge(String operatorId) {
        String raw = operatorId + "|" + System.currentTimeMillis() + "|" +
                Math.random() + "|" + SECRET_KEY;
        return sha256Hex(raw);
    }

    private String computeSignature(String challenge) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            SecretKeySpec keySpec = new SecretKeySpec(
                    SECRET_KEY.getBytes(StandardCharsets.UTF_8), HMAC_ALGORITHM);
            mac.init(keySpec);
            byte[] rawHmac = mac.doFinal(challenge.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(rawHmac);
        } catch (Exception e) {
            log.error("计算签名失败", e);
            return null;
        }
    }

    private String computeDualAuthHash(String op1, String op2) {
        String raw = op1 + "||" + op2 + "||" +
                LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) +
                "||" + SECRET_KEY;
        return sha256Hex(raw).toUpperCase();
    }

    private String sha256Hex(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception e) {
            log.error("SHA-256计算失败", e);
            return null;
        }
    }

    private boolean isTokenExpired(AuthToken token) {
        long elapsed = java.time.Duration.between(token.getCreateTime(), LocalDateTime.now()).getSeconds();
        return elapsed > 60;
    }

    public Map<String, UrgentOperationLog> getOperationLog() {
        return operationLog;
    }

    public void extendAuthValidity() {
        if (isEmergencyModeActive()) {
            authEstablishedTime = LocalDateTime.now();
            log.debug("【双签核】鉴权有效期已续期");
        }
    }
}
