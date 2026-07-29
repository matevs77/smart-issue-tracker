package com.teuprojecto.tracker.config;

import org.junit.jupiter.api.Test;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;

import static org.assertj.core.api.Assertions.assertThat;

class VirtualThreadConfigTest {

    @Test
    void virtualThreadExecutorRunsTasksOnVirtualThreads() throws ExecutionException, InterruptedException {
        var config = new VirtualThreadConfig();
        ExecutorService executor = config.virtualThreadExecutor();

        try (executor) {
            var isVirtual = executor.submit(() -> Thread.currentThread().isVirtual()).get();
            assertThat(isVirtual).isTrue();
        }
    }
}
