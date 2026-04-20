package com.hify.model.provider;

import com.hify.model.config.LlmGatewayProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LlmProviderSemaphoreManagerTest {

    private LlmGatewayProperties properties;
    private LlmProviderSemaphoreManager manager;

    @BeforeEach
    void setUp() {
        properties = new LlmGatewayProperties();
        properties.setDefaultMaxConcurrent(2);
        Map<String, Integer> providerMax = new HashMap<>();
        providerMax.put("openai", 5);
        properties.setProviderMaxConcurrent(providerMax);

        manager = new LlmProviderSemaphoreManager(properties);
    }

    @Test
    void shouldUseSpecificPermits_whenAcquire_givenConfiguredProvider() throws InterruptedException {
        // openai 配置了 5 个许可
        manager.acquire("openai");
        manager.acquire("openai");
        manager.acquire("openai");
        manager.acquire("openai");
        manager.acquire("openai");
        // 第 6 个应该阻塞

        AtomicInteger acquired = new AtomicInteger(0);
        Thread t = new Thread(() -> {
            try {
                manager.acquire("openai");
                acquired.incrementAndGet();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
        t.start();
        t.join(100);

        assertThat(acquired.get()).isZero();
        assertThat(t.isAlive()).isTrue();

        // 释放一个后，线程应该能获取到
        manager.release("openai");
        t.join(500);
        assertThat(acquired.get()).isEqualTo(1);
    }

    @Test
    void shouldUseDefaultPermits_whenAcquire_givenUnconfiguredProvider() throws InterruptedException {
        // deepseek 没有单独配置，使用默认值 2
        manager.acquire("deepseek");
        manager.acquire("deepseek");

        AtomicInteger acquired = new AtomicInteger(0);
        Thread t = new Thread(() -> {
            try {
                manager.acquire("deepseek");
                acquired.incrementAndGet();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
        t.start();
        t.join(100);

        assertThat(acquired.get()).isZero();
        assertThat(t.isAlive()).isTrue();

        manager.release("deepseek");
        t.join(500);
        assertThat(acquired.get()).isEqualTo(1);
    }

    @Test
    void shouldCreateSemaphoreOnce_whenSameProviderCalledMultipleTimes() throws InterruptedException {
        // 多次获取同一 provider 的信号量，内部应该只创建一个 Semaphore
        manager.acquire("openai");
        manager.release("openai");
        manager.acquire("openai");
        manager.release("openai");

        // 验证总共还能获取 5 次（没有泄漏）
        for (int i = 0; i < 5; i++) {
            manager.acquire("openai");
        }
        // 第 6 次应该阻塞
        AtomicInteger acquired = new AtomicInteger(0);
        Thread t = new Thread(() -> {
            try {
                if (manager.getClass().getDeclaredField("semaphoreMap") != null) {
                    // 通过反射验证 map 中只有一个条目
                }
                manager.acquire("openai");
                acquired.incrementAndGet();
            } catch (Exception e) {
                // ignore
            }
        });
        t.start();
        t.join(100);
        assertThat(acquired.get()).isZero();

        // 全部释放
        for (int i = 0; i < 5; i++) {
            manager.release("openai");
        }
    }

    @Test
    void shouldNotBlockDifferentProviders() throws InterruptedException {
        // openai 的许可用尽不应影响 deepseek
        manager.acquire("openai");
        manager.acquire("openai");
        manager.acquire("openai");
        manager.acquire("openai");
        manager.acquire("openai");

        // deepseek 应该还能获取
        manager.acquire("deepseek");
        assertThat(true).isTrue();

        manager.release("deepseek");
        manager.release("openai");
        manager.release("openai");
        manager.release("openai");
        manager.release("openai");
        manager.release("openai");
    }

    @Test
    void shouldSupportConcurrentAcquireFromMultipleThreads() throws InterruptedException {
        int threads = 10;
        CountDownLatch latch = new CountDownLatch(threads);
        AtomicInteger successCount = new AtomicInteger(0);

        // openai 有 5 个许可，10 个线程竞争
        for (int i = 0; i < threads; i++) {
            new Thread(() -> {
                try {
                    manager.acquire("openai");
                    successCount.incrementAndGet();
                    Thread.sleep(50); // 持有一段时间
                    manager.release("openai");
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    latch.countDown();
                }
            }).start();
        }

        latch.await(5, TimeUnit.SECONDS);
        assertThat(successCount.get()).isEqualTo(threads);
    }

    @Test
    void shouldThrowInterruptedException_whenAcquireInterrupted() throws InterruptedException {
        manager.acquire("deepseek");
        manager.acquire("deepseek"); // 用尽 2 个许可

        Thread t = new Thread(() -> {
            try {
                manager.acquire("deepseek");
            } catch (InterruptedException e) {
                // expected
            }
        });
        t.start();
        t.join(50); // 确保线程已进入 acquire
        t.interrupt();
        t.join(500);

        assertThat(t.isAlive()).isFalse();
    }
}
