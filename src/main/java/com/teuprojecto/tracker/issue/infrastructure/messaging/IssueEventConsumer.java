package com.teuprojecto.tracker.issue.infrastructure.messaging;

import com.teuprojecto.tracker.issue.domain.event.IssueCreatedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.RetryableTopic;
import org.springframework.retry.annotation.Backoff;
import org.springframework.stereotype.Component;

@Component
public class IssueEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(IssueEventConsumer.class);

    @RetryableTopic(attempts = "4", backoff = @Backoff(delay = 1000, multiplier = 2.0))
    @KafkaListener(topics = "issue-events", groupId = "issue-classification-group",
            containerFactory = "kafkaListenerContainerFactory")
    public void onIssueCreated(IssueCreatedEvent event) {
        log.info("IssueCreatedEvent received — eventId={}, issueId={}; classification pending (Fase 6)",
                event.eventId(), event.payload().issueId());
        // TODO(Fase 6): invocar AIPriorityService, classificar a prioridade da issue e publicar IssuePrioritizedEvent
    }
}
