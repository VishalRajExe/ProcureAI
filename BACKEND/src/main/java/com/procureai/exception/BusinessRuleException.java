package com.procureai.exception;

/** Thrown when an action would violate a backend-enforced business rule (e.g. AI
 * recommendation exceeding max approved price, invalid state transition, etc). */
public class BusinessRuleException extends RuntimeException {
    public BusinessRuleException(String message) { super(message); }
}
