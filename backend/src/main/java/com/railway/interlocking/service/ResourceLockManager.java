package com.railway.interlocking.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

@Slf4j
@Component
public class ResourceLockManager {

    private final Map<String, ReentrantLock> resourceLocks = new ConcurrentHashMap<>();
    private final ThreadLocal<Set<String>> heldLocks = ThreadLocal.withInitial(HashSet::new);
    private static final long LOCK_TIMEOUT_MS = 5000;

    public boolean tryLockAll(List<String> resourceIds, String routeId) {
        List<String> sorted = new ArrayList<>(resourceIds);
        Collections.sort(sorted);

        List<ReentrantLock> acquiredLocks = new ArrayList<>();

        for (String resourceId : sorted) {
            ReentrantLock lock = resourceLocks.computeIfAbsent(resourceId, k -> new ReentrantLock(true));

            try {
                boolean got = lock.tryLock(LOCK_TIMEOUT_MS, java.util.concurrent.TimeUnit.MILLISECONDS);
                if (!got) {
                    log.warn("获取资源锁超时: resourceId={}, routeId={}, holder={}",
                            resourceId, routeId, lock.isHeldByCurrentThread());
                    rollbackAcquiredLocks(acquiredLocks, sorted);
                    return false;
                }
                acquiredLocks.add(lock);
                heldLocks.get().add(resourceId);
                log.debug("资源锁已获取: resourceId={}, routeId={}", resourceId, routeId);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.warn("获取资源锁被中断: resourceId={}, routeId={}", resourceId, routeId);
                rollbackAcquiredLocks(acquiredLocks, sorted);
                return false;
            }
        }

        log.info("所有资源锁获取成功: resources={}, routeId={}", sorted, routeId);
        return true;
    }

    public void unlockAll(List<String> resourceIds, String routeId) {
        List<String> sorted = new ArrayList<>(resourceIds);
        Collections.sort(sorted);
        Collections.reverse(sorted);

        for (String resourceId : sorted) {
            ReentrantLock lock = resourceLocks.get(resourceId);
            if (lock != null && lock.isHeldByCurrentThread()) {
                try {
                    lock.unlock();
                    heldLocks.get().remove(resourceId);
                    log.debug("资源锁已释放: resourceId={}, routeId={}", resourceId, routeId);
                } catch (Exception e) {
                    log.error("释放资源锁异常: resourceId={}, routeId={}", resourceId, routeId, e);
                }
            }
        }

        log.info("所有资源锁已释放: count={}, routeId={}", resourceIds.size(), routeId);
    }

    public void unlockHeldLocks(String routeId) {
        Set<String> held = heldLocks.get();
        for (String resourceId : new ArrayList<>(held)) {
            ReentrantLock lock = resourceLocks.get(resourceId);
            if (lock != null && lock.isHeldByCurrentThread()) {
                try {
                    lock.unlock();
                    log.debug("释放线程持有锁: resourceId={}, routeId={}", resourceId, routeId);
                } catch (Exception e) {
                    log.error("释放线程持有锁异常: resourceId={}", resourceId, e);
                }
            }
        }
        held.clear();
    }

    public List<String> detectDeadlockedResources(List<String> resourceIds) {
        List<String> deadlocked = new ArrayList<>();
        for (String resourceId : resourceIds) {
            ReentrantLock lock = resourceLocks.get(resourceId);
            if (lock != null && lock.isLocked() && !lock.isHeldByCurrentThread()) {
                deadlocked.add(resourceId);
            }
        }
        return deadlocked;
    }

    public boolean isResourceLocked(String resourceId) {
        ReentrantLock lock = resourceLocks.get(resourceId);
        return lock != null && lock.isLocked();
    }

    public Map<String, Object> getLockStatus() {
        Map<String, Object> status = new HashMap<>();
        int total = 0;
        int locked = 0;
        for (Map.Entry<String, ReentrantLock> entry : resourceLocks.entrySet()) {
            total++;
            if (entry.getValue().isLocked()) {
                locked++;
            }
        }
        status.put("totalResources", total);
        status.put("lockedResources", locked);
        return status;
    }

    private void rollbackAcquiredLocks(List<ReentrantLock> acquiredLocks, List<String> sorted) {
        for (int i = acquiredLocks.size() - 1; i >= 0; i--) {
            try {
                acquiredLocks.get(i).unlock();
            } catch (Exception e) {
                log.error("回滚释放锁异常", e);
            }
        }
        heldLocks.get().removeAll(sorted);
    }
}
