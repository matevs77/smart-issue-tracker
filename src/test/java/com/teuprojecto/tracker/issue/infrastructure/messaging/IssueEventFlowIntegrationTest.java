package com.teuprojecto.tracker.issue.infrastructure.messaging;

import com.teuprojecto.tracker.issue.domain.event.IssueCreatedEvent;
import com.teuprojecto.tracker.issue.presentation.dto.CreateIssueRequest;
import com.teuprojecto.tracker.security.presentation.dto.AuthResponse;
import com.teuprojecto.tracker.security.presentation.dto.LoginRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.util.UUID;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
class IssueEventFlowIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16")
            .withDatabaseName("testdb")
            .withUsername("test")
            .withPassword("test");

    @Container
    static KafkaContainer kafka = new KafkaContainer(
            DockerImageName.parse("confluentinc/cp-kafka:7.6.0"));

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.flyway.placeholders.admin_password_hash",
                () -> "$2b$10$EX0/eIBKN7WrlNZdwG/g7eHjqlD/0NWnev0ivfq4D1sGmiADgcgNe");
        registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);
    }

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private TestEventCollector testEventCollector;

    @Test
    void createIssuePublishesAndConsumesIssueCreatedEvent() throws Exception {
        var loginRequest = new LoginRequest("admin", "ChangeMe123!");
        var loginResponse = restTemplate.postForEntity("/api/v1/auth/login", loginRequest, AuthResponse.class);

        assertThat(loginResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(loginResponse.getBody()).isNotNull();

        var token = loginResponse.getBody().token();
        var headers = new HttpHeaders();
        headers.setBearerAuth(token);
        headers.set("Content-Type", "application/json");

        var requestBody = """
                {"title": "Integration Test Issue", "description": "Created via integration test"}
                """;
        var response = restTemplate.postForEntity("/api/v1/issues",
                new HttpEntity<>(requestBody, headers), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);

        var receivedEvent = testEventCollector.eventQueue.poll(30, TimeUnit.SECONDS);
        assertThat(receivedEvent).isNotNull();
        assertThat(receivedEvent.eventType()).isEqualTo("ISSUE_CREATED");
        assertThat(receivedEvent.payload().title()).isEqualTo("Integration Test Issue");
        assertThat(receivedEvent.payload().description()).isEqualTo("Created via integration test");
    }

    @Configuration
    static class TestConfig {
        @Bean
        public TestEventCollector testEventCollector() {
            return new TestEventCollector();
        }
    }

    static class TestEventCollector {
        final LinkedBlockingQueue<IssueCreatedEvent> eventQueue = new LinkedBlockingQueue<>();

        @KafkaListener(topics = "issue-events", groupId = "test-collection-group",
                containerFactory = "kafkaListenerContainerFactory")
        public void collect(IssueCreatedEvent event,
                            @Header(KafkaHeaders.RECEIVED_KEY) String key) {
            eventQueue.offer(event);
        }
    }
}
