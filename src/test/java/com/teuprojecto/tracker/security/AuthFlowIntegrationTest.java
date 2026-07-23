package com.teuprojecto.tracker.security;

import com.teuprojecto.tracker.security.presentation.dto.AuthResponse;
import com.teuprojecto.tracker.security.presentation.dto.LoginRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
class AuthFlowIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16")
            .withDatabaseName("testdb")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.flyway.placeholders.admin_password_hash",
                () -> "$2b$10$EX0/eIBKN7WrlNZdwG/g7eHjqlD/0NWnev0ivfq4D1sGmiADgcgNe");
    }

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void loginWithAdminThenAccessProtectedEndpoint() {
        var loginRequest = new LoginRequest("admin", "ChangeMe123!");
        var loginResponse = restTemplate.postForEntity("/api/v1/auth/login", loginRequest, AuthResponse.class);

        assertThat(loginResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(loginResponse.getBody()).isNotNull();
        assertThat(loginResponse.getBody().token()).isNotBlank();
        assertThat(loginResponse.getBody().tokenType()).isEqualTo("Bearer");
        assertThat(loginResponse.getBody().expiresIn()).isEqualTo(3600L);

        var headers = new HttpHeaders();
        headers.setBearerAuth(loginResponse.getBody().token());
        var issuesResponse = restTemplate.exchange(
                "/api/v1/issues",
                HttpMethod.GET,
                new HttpEntity<>(headers),
                String.class
        );

        assertThat(issuesResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
    }
}
