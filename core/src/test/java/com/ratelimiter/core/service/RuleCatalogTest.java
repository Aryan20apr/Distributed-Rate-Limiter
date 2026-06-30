package com.ratelimiter.core.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.Test;

import com.ratelimiter.core.config.RateLimitScope;
import com.ratelimiter.core.dtos.RateLimitRule;

class RuleCatalogTest {

    @Test
    void replaceAllReturnsImmutableSnapshot() {
        RuleCatalog catalog = new RuleCatalog();
        RateLimitRule rule = sampleRule("rule-a");

        catalog.replaceAll(List.of(rule));

        List<RateLimitRule> snapshot = catalog.getRules();
        assertThat(snapshot).hasSize(1);
        assertThatCode(() -> snapshot.add(sampleRule("rule-b")))
                .isInstanceOf(UnsupportedOperationException.class);
        assertThat(catalog.getRules()).hasSize(1);
    }

    @Test
    void concurrentReadsDuringReloadDoNotThrow() throws Exception {
        RuleCatalog catalog = new RuleCatalog();
        catalog.replaceAll(List.of(sampleRule("initial")));

        ExecutorService executor = Executors.newFixedThreadPool(8);
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch readersDone = new CountDownLatch(8);
        AtomicBoolean readerFailed = new AtomicBoolean(false);

        for (int i = 0; i < 8; i++) {
            executor.submit(() -> {
                try {
                    start.await();
                    for (int j = 0; j < 500; j++) {
                        catalog.getRules().size();
                    }
                } catch (Exception ex) {
                    readerFailed.set(true);
                } finally {
                    readersDone.countDown();
                }
            });
        }

        start.countDown();
        for (int i = 0; i < 100; i++) {
            catalog.replaceAll(List.of(sampleRule("rule-" + i)));
        }

        assertThat(readersDone.await(5, TimeUnit.SECONDS)).isTrue();
        assertThat(readerFailed).isFalse();
        executor.shutdownNow();
    }

    private static RateLimitRule sampleRule(String name) {
        RateLimitRule rule = new RateLimitRule();
        rule.setName(name);
        rule.setScope(RateLimitScope.USER);
        rule.setAlgorithm("token");
        rule.setCapacity(10);
        rule.setRefillPerSecond(1.0);
        return rule;
    }
}
