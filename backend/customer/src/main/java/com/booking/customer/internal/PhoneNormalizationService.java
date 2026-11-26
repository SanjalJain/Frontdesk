package com.booking.customer.internal;

import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.Phonenumber;
import org.springframework.stereotype.Service;

@Service
public class PhoneNormalizationService {

    private final PhoneNumberUtil phoneUtil = PhoneNumberUtil.getInstance();

    public String normalize(String rawPhone, String defaultRegion) {
        if (rawPhone == null || rawPhone.isBlank()) {
            throw new IllegalArgumentException("Phone number cannot be blank");
        }

        String region = (defaultRegion != null && !defaultRegion.isBlank()) ? defaultRegion.toUpperCase() : "IN";

        try {
            Phonenumber.PhoneNumber parsed = phoneUtil.parse(rawPhone.trim(), region);
            if (!phoneUtil.isValidNumber(parsed)) {
                throw new IllegalArgumentException("Invalid phone number format: " + rawPhone);
            }
            return phoneUtil.format(parsed, PhoneNumberUtil.PhoneNumberFormat.E164);
        } catch (NumberParseException e) {
            throw new IllegalArgumentException("Could not parse phone number: " + rawPhone, e);
        }
    }
}
