package com.booking.business.internal.customer;

public class DuplicateCustomerPhoneException extends RuntimeException {

    private final Long existingCustomerId;
    private final String phone;

    public DuplicateCustomerPhoneException(String message, Long existingCustomerId, String phone) {
        super(message);
        this.existingCustomerId = existingCustomerId;
        this.phone = phone;
    }

    public Long getExistingCustomerId() {
        return existingCustomerId;
    }

    public String getPhone() {
        return phone;
    }
}
