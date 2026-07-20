package com.teuprojecto.tracker.shared.exception;

import com.teuprojecto.tracker.user.domain.DuplicateUserException;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.net.URI;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    private HttpServletRequest request() {
        return new MockHttpServletRequest();
    }

    @Test
    void handleValidationReturns400() {
        var bindingResult = new BeanPropertyBindingResult(new Object(), "request");
        bindingResult.addError(new FieldError("request", "field", "must not be blank"));
        var ex = new MethodArgumentNotValidException(null, bindingResult);
        var problem = handler.handleValidation(ex, request());

        assertThat(problem.getStatus()).isEqualTo(400);
        assertThat(problem.getType()).isEqualTo(URI.create("https://api.issuetracker.dev/errors/invalid-request"));
        assertThat(problem.getProperties().get("field-errors")).isNotNull();
    }

    @Test
    void handleIssueNotFoundReturns404() {
        var ex = new IssueNotFoundException(java.util.UUID.randomUUID());
        var problem = handler.handleNotFound(ex, request());

        assertThat(problem.getStatus()).isEqualTo(404);
        assertThat(problem.getType()).isEqualTo(URI.create("https://api.issuetracker.dev/errors/issue-not-found"));
    }

    @Test
    void handleDomainRuleViolationReturns422() {
        var problem = handler.handleDomainRuleViolation(
                new IllegalArgumentException("boom"), request());

        assertThat(problem.getStatus()).isEqualTo(422);
        assertThat(problem.getType()).isEqualTo(URI.create("https://api.issuetracker.dev/errors/unprocessable-entity"));
    }

    @Test
    void handleIllegalStateExceptionReturns422() {
        var problem = handler.handleDomainRuleViolation(
                new IllegalStateException("closed"), request());

        assertThat(problem.getStatus()).isEqualTo(422);
        assertThat(problem.getType()).isEqualTo(URI.create("https://api.issuetracker.dev/errors/unprocessable-entity"));
    }

    @Test
    void handleDuplicateUserReturns409() {
        var problem = handler.handleDuplicateUser(
                new DuplicateUserException("exists"), request());

        assertThat(problem.getStatus()).isEqualTo(409);
        assertThat(problem.getType()).isEqualTo(URI.create("https://api.issuetracker.dev/errors/duplicate-user"));
    }

    @Test
    void handleNoResourceFoundReturns404() {
        var problem = handler.handleNotFound(
                new NoResourceFoundException(HttpMethod.GET, "missing"), request());

        assertThat(problem.getStatus()).isEqualTo(404);
        assertThat(problem.getType()).isEqualTo(URI.create("https://api.issuetracker.dev/errors/not-found"));
    }

    @Test
    void handleGenericReturns500() {
        var problem = handler.handleGeneric(new RuntimeException("oops"), request());

        assertThat(problem.getStatus()).isEqualTo(500);
        assertThat(problem.getType()).isEqualTo(URI.create("https://api.issuetracker.dev/errors/internal-error"));
    }
}
