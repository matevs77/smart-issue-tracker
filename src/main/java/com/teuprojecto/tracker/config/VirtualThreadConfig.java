package com.teuprojecto.tracker.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Bean reutilizável de Virtual Threads para consumidores de mensageria.
 * <p>
 * Expõe um {@link ExecutorService} baseado em Virtual Threads (Project Loom)
 * para ser injetado no {@code ConcurrentKafkaListenerContainerFactory}
 * (Fase 4 — concluída) e no equivalente RabbitMQ (Fase 5), conforme definido em
 * {@code docs/08-messaging.md}, secção 1.5.
 * </p>
 */
@Configuration
public class VirtualThreadConfig {

    @Bean(destroyMethod = "close")
    public ExecutorService virtualThreadExecutor() {
        return Executors.newVirtualThreadPerTaskExecutor();
    }

    // TODO(Fase 5): injetar este bean no equivalente RabbitMQ quando os
    // respetivos consumidores forem implementados
}
