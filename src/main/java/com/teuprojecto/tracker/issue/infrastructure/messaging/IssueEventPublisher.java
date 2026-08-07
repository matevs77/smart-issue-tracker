package com.teuprojecto.tracker.issue.infrastructure.messaging;

import com.teuprojecto.tracker.issue.domain.Issue;
import com.teuprojecto.tracker.issue.domain.event.IssueCreatedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class IssueEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(IssueEventPublisher.class);

    private final KafkaTemplate<String, IssueCreatedEvent> kafkaTemplate;
    private final String topic;

    public IssueEventPublisher(KafkaTemplate<String, IssueCreatedEvent> kafkaTemplate,
                               @Value("${spring.kafka.topic.issue-events:issue-events}") String topic) {
        this.kafkaTemplate = kafkaTemplate;
        this.topic = topic;
    }

    public void publishIssueCreated(Issue issue) {
        var event = IssueCreatedEvent.from(issue);
        try {
            kafkaTemplate.send(topic, issue.getId().toString(), event);
            log.debug("Published IssueCreatedEvent for issueId={}", issue.getId());
        } catch (Exception e) {
            log.error("Failed to publish IssueCreatedEvent for issueId={}", issue.getId(), e);
        }
    }
}
