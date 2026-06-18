package com.railway.interlocking.service;

import com.railway.interlocking.model.*;
import com.railway.interlocking.model.enums.*;
import com.railway.interlocking.statemachine.InterlockingStateMachine;
import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
public class TransactionCoordinator {

    private final InterlockingStateMachine stateMachine;
    private final ResourceLockManager resourceLockManager;
    private final Map<String, TransactionContext> activeTransactions = new ConcurrentHashMap<>();

    private static final long TRANSACTION_TIMEOUT_MS = 10000;

    public enum TransactionPhase {
        INITIALIZED,
        PREPARING,
        PREPARED,
        COMMITTING,
        COMMITTED,
        COMPENSATING,
        COMPENSATED,
        FAILED
    }

    @Data
    @Builder
    public static class TransactionContext {
        private String transactionId;
        private String routeId;
        private String routeName;
        private TransactionPhase phase;
        private LocalDateTime startTime;
        private List<CompensationAction> compensationStack;
        private String operator;
        private String errorMessage;
    }

    @FunctionalInterface
    public interface CompensationAction {
        void compensate() throws Exception;
    }

    public TransactionCoordinator(InterlockingStateMachine stateMachine,
                                   ResourceLockManager resourceLockManager) {
        this.stateMachine = stateMachine;
        this.resourceLockManager = resourceLockManager;
    }

    public TransactionContext beginTransaction(String routeId, String routeName, String operator) {
        String txId = "TX_" + routeId + "_" + System.currentTimeMillis();
        TransactionContext ctx = TransactionContext.builder()
                .transactionId(txId)
                .routeId(routeId)
                .routeName(routeName)
                .phase(TransactionPhase.INITIALIZED)
                .startTime(LocalDateTime.now())
                .compensationStack(new ArrayList<>())
                .operator(operator)
                .build();

        activeTransactions.put(txId, ctx);
        log.info("事务已开始: txId={}, routeId={}, operator={}", txId, routeId, operator);
        return ctx;
    }

    public void setPhase(TransactionContext ctx, TransactionPhase phase) {
        if (ctx != null) {
            ctx.setPhase(phase);
            log.debug("事务阶段变更: txId={}, phase={}", ctx.getTransactionId(), phase);
        }
    }

    public void pushCompensation(TransactionContext ctx, CompensationAction action) {
        if (ctx != null && ctx.getCompensationStack() != null) {
            ctx.getCompensationStack().add(action);
        }
    }

    public boolean prepareResources(TransactionContext ctx, Route route,
                                     Map<String, TrackSection> trackSections,
                                     Map<String, Switch> switches,
                                     Map<String, Signal> signals) {
        setPhase(ctx, TransactionPhase.PREPARING);

        List<String> allResourceIds = new ArrayList<>();
        allResourceIds.addAll(route.getSectionIds());
        allResourceIds.addAll(route.getSwitchIds());
        allResourceIds.add(route.getStartSignalId());
        if (route.getHostileSignalIds() != null) {
            allResourceIds.addAll(route.getHostileSignalIds());
        }

        boolean locked = resourceLockManager.tryLockAll(allResourceIds, route.getId());
        if (!locked) {
            ctx.setErrorMessage("资源锁获取失败，可能存在并发冲突");
            setPhase(ctx, TransactionPhase.FAILED);
            return false;
        }

        pushCompensation(ctx, () -> {
            resourceLockManager.unlockAll(allResourceIds, route.getId());
            log.info("补偿: 释放资源锁, routeId={}", route.getId());
        });

        setPhase(ctx, TransactionPhase.PREPARED);
        return true;
    }

    public void commit(TransactionContext ctx) {
        if (ctx == null) return;
        setPhase(ctx, TransactionPhase.COMMITTED);
        ctx.getCompensationStack().clear();
        activeTransactions.remove(ctx.getTransactionId());
        log.info("事务已提交: txId={}, routeId={}", ctx.getTransactionId(), ctx.getRouteId());
    }

    public void rollback(TransactionContext ctx) {
        if (ctx == null) return;
        setPhase(ctx, TransactionPhase.COMPENSATING);

        List<CompensationAction> stack = ctx.getCompensationStack();
        for (int i = stack.size() - 1; i >= 0; i--) {
            try {
                stack.get(i).compensate();
            } catch (Exception e) {
                log.error("补偿动作执行失败: txId={}, step={}", ctx.getTransactionId(), i, e);
            }
        }

        setPhase(ctx, TransactionPhase.COMPENSATED);
        activeTransactions.remove(ctx.getTransactionId());
        log.info("事务已回滚: txId={}, routeId={}", ctx.getTransactionId(), ctx.getRouteId());
    }

    public void forceCleanupStaleTransactions() {
        long now = System.currentTimeMillis();
        for (Map.Entry<String, TransactionContext> entry : activeTransactions.entrySet()) {
            TransactionContext ctx = entry.getValue();
            if (ctx.getStartTime() != null) {
                long elapsed = now - java.time.Duration.between(ctx.getStartTime(),
                        LocalDateTime.now()).toMillis();
                if (elapsed > TRANSACTION_TIMEOUT_MS) {
                    log.warn("检测到超时事务，强制清理: txId={}, routeId={}, phase={}",
                            ctx.getTransactionId(), ctx.getRouteId(), ctx.getPhase());
                    rollback(ctx);
                }
            }
        }
    }

    public List<Map<String, Object>> getActiveTransactionStatus() {
        List<Map<String, Object>> result = new ArrayList<>();
        for (TransactionContext ctx : activeTransactions.values()) {
            Map<String, Object> status = new HashMap<>();
            status.put("transactionId", ctx.getTransactionId());
            status.put("routeId", ctx.getRouteId());
            status.put("routeName", ctx.getRouteName());
            status.put("phase", ctx.getPhase());
            status.put("startTime", ctx.getStartTime());
            status.put("compensationStackSize", ctx.getCompensationStack().size());
            status.put("errorMessage", ctx.getErrorMessage());
            result.add(status);
        }
        return result;
    }
}
