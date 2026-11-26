package com.booking.business.internal.scheduling;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

@RestControllerAdvice
public class SchedulingExceptionHandler {

    @ExceptionHandler(DoubleBookingException.class)
    public ResponseEntity<Map<String, String>> handleDoubleBooking(DoubleBookingException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of(
                "error", "DOUBLE_BOOKING_CONFLICT",
                "message", ex.getMessage()
        ));
    }

    @ExceptionHandler(AppointmentOptimisticLockException.class)
    public ResponseEntity<Map<String, String>> handleOptimisticLock(AppointmentOptimisticLockException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of(
                "error", "CONCURRENT_MODIFICATION_CONFLICT",
                "message", ex.getMessage()
        ));
    }
}
