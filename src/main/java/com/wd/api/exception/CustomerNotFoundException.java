package com.wd.api.exception;

/**
 * Exception thrown when a customer is not found by ID.
 * Results in HTTP 404 Not Found response.
 */
public class CustomerNotFoundException extends RuntimeException {

    private final Long customerId;

    public CustomerNotFoundException(Long id) {
        super(String.format("Customer not found with ID: %d", id));
        this.customerId = id;
    }

    public Long getCustomerId() {
        return customerId;
    }
}
