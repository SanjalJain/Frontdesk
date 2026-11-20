package com.booking.business.internal.customer;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.*;

class PhoneNormalizationServiceTest {

    private PhoneNormalizationService service;

    @BeforeEach
    void setUp() {
        service = new PhoneNormalizationService();
    }

    @ParameterizedTest
    @CsvSource({
            "9876543210, IN, +919876543210",
            "09876543210, IN, +919876543210",
            "+919876543210, IN, +919876543210",
            "98765 43210, IN, +919876543210",
            "98765-43210, IN, +919876543210",
            "+14155552671, US, +14155552671",
            "4155552671, US, +14155552671"
    })
    void normalizesValidNumbersToE164(String input, String country, String expected) {
        String result = service.normalizeToE164(input, country);
        assertEquals(expected, result);
    }

    @Test
    void rejectsInvalidPhoneNumbers() {
        assertThrows(IllegalArgumentException.class, () -> service.normalizeToE164("123", "IN"));
        assertThrows(IllegalArgumentException.class, () -> service.normalizeToE164("abcdefghij", "IN"));
        assertThrows(IllegalArgumentException.class, () -> service.normalizeToE164("", "IN"));
        assertThrows(IllegalArgumentException.class, () -> service.normalizeToE164(null, "IN"));
    }
}
