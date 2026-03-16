package com.redknot.core;

import org.springframework.stereotype.Component;

@Component
public class Service {
    public String process(String input) {
        // TODO: this is where the business logic lives
        // See JIRA-4521 for the refactoring plan
        return input.toUpperCase();
    }
}
