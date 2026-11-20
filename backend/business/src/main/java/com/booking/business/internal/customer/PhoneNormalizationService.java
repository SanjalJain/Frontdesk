package com.booking.business.internal.customer;

import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.Phonenumber.PhoneNumber;
import org.springframework.stereotype.Service;

@Service
public class PhoneNormalizationService {

    private final PhoneNumberUtil phoneUtil = PhoneNumberUtil.getInstance();

    /**
     * Normalizes a phone string into E.164 format (e.g. +919876543210).
     *
     * @param rawPhone       the user-supplied phone string
     * @param defaultCountry the default 2-letter ISO country code (e.g. "IN")
     * @return normalized E.164 phone string
     * @throws IllegalArgumentException if the phone number is invalid
     */
    public String normalizeToE164(String rawPhone, String defaultCountry) {
        if (rawPhone == null || rawPhone.isBlank()) {
            throw new IllegalArgumentException("Phone number cannot be blank");
        }

        String country = (defaultCountry != null && !defaultCountry.isBlank())
                ? defaultCountry.trim().toUpperCase()
                : "IN";

        // If phone already starts with '+', country code is self-contained
        String cleaned = rawPhone.trim().replaceAll("[\\s\\-\\(\\)]", "");

        try {
            PhoneNumber parsedNumber = phoneUtil.parse(cleaned, country);
            if (!phoneUtil.isValidNumber(parsedNumber)) {
                throw new IllegalArgumentException("Invalid phone number format: " + rawPhone);
            }
            return phoneUtil.format(parsedNumber, PhoneNumberUtil.PhoneNumberFormat.E164);
        } catch (NumberParseException e) {
            throw new IllegalArgumentException("Could not parse phone number '" + rawPhone + "': " + e.getMessage(), e);
        }
    }
}
