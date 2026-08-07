package com.teuprojecto.tracker.config;

import java.util.concurrent.ExecutorService;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.support.TaskExecutorAdapter;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;

import com.teuprojecto.tracker.issue.domain.event.IssueCreatedEvent;

@Configuration
public class KafkaConfig {

    @Bean
    public NewTopic issueEvents() {
        return new NewTopic("issue-events", 3, (short) 1);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, IssueCreatedEvent> kafkaListenerContainerFactory(
            ConsumerFactory<String, IssueCreatedEvent> consumerFactory,
            ExecutorService virtualThreadExecutor) {
        var factory = new ConcurrentKafkaListenerContainerFactory<String, IssueCreatedEvent>();
        factory.setConsumerFactory(consumerFactory);
        factory.getContainerProperties().setListenerTaskExecutor(
                new TaskExecutorAdapter(virtualThreadExecutor));
        return factory;
    }
}
