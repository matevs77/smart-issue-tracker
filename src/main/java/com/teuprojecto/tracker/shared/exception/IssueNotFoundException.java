package com.teuprojecto.tracker.shared.exception;

import java.util.UUID;

public class IssueNotFoundException extends RuntimeException {

    public IssueNotFoundException(UUID id) {
        super("Issue not found with id: " + id);
    }
}
