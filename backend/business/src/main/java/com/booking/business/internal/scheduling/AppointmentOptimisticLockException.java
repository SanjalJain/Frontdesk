package com.booking.business.internal.scheduling;

public class AppointmentOptimisticLockException extends RuntimeException {
    public AppointmentOptimisticLockException(String message) {
        super(message);
    }
}
