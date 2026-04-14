package com.hify.common.web.lock;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * 分布式锁工具类
 *
 * @author hify
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DistributedLock {

    private final RedissonClient redissonClient;

    private String buildKey(String key) {
        return "lock:" + key;
    }

    /**
     * 尝试获取锁并执行业务逻辑
     *
     * @param key       锁键
     * @param waitTime  等待时间
     * @param leaseTime 租约时间（-1 启用看门狗自动续期）
     * @param unit      时间单位
     * @param supplier  业务逻辑
     * @return 业务返回值
     */
    public <T> T tryLock(String key, long waitTime, long leaseTime, TimeUnit unit, Supplier<T> supplier) {
        RLock lock = redissonClient.getLock(buildKey(key));
        boolean locked = false;
        try {
            locked = lock.tryLock(waitTime, leaseTime, unit);
            if (!locked) {
                throw new RuntimeException("获取分布式锁失败: " + key);
            }
            log.debug("获取分布式锁成功, key={}", key);
            return supplier.get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("获取分布式锁被中断: " + key, e);
        } finally {
            if (locked && lock.isHeldByCurrentThread()) {
                lock.unlock();
                log.debug("释放分布式锁, key={}", key);
            }
        }
    }

    /**
     * 尝试获取锁并执行业务逻辑（无返回值）
     */
    public void tryLock(String key, long waitTime, long leaseTime, TimeUnit unit, Runnable runnable) {
        tryLock(key, waitTime, leaseTime, unit, () -> {
            runnable.run();
            return null;
        });
    }

    /**
     * 快速加锁（默认等待 3 秒，看门狗自动续期）
     */
    public <T> T lock(String key, Supplier<T> supplier) {
        return tryLock(key, 3, -1, TimeUnit.SECONDS, supplier);
    }

    /**
     * 快速加锁（无返回值）
     */
    public void lock(String key, Runnable runnable) {
        tryLock(key, 3, -1, TimeUnit.SECONDS, runnable);
    }
}
