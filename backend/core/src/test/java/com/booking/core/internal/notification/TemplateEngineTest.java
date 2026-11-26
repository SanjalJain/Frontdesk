package com.booking.core.internal.notification;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TemplateEngineTest {

    private TemplateEngine engine;

    @BeforeEach
    void setUp() {
        engine = new TemplateEngine();
    }

    @Test
    @DisplayName("Should replace all declared merge placeholders")
    void shouldReplaceVariables() {
        String template = "Hello {{customer_name}}, your {{appointment_label}} for {{service_name}} is at {{start_time}} on {{date}} at {{clinic_name}}.";
        Map<String, String> vars = Map.of(
                "customer_name", "Rahul Sharma",
                "appointment_label", "Appointment",
                "service_name", "Dental Cleaning",
                "start_time", "10:30 AM",
                "date", "25 Nov 2026",
                "clinic_name", "Apex Dental"
        );

        String result = engine.render(template, vars);
        assertEquals("Hello Rahul Sharma, your Appointment for Dental Cleaning is at 10:30 AM on 25 Nov 2026 at Apex Dental.", result);
    }

    @Test
    @DisplayName("Should leave text intact if no variables match")
    void shouldHandleNoVariables() {
        String template = "Standard text with no placeholders.";
        String result = engine.render(template, Map.of("foo", "bar"));
        assertEquals("Standard text with no placeholders.", result);
    }

    @Test
    @DisplayName("Should handle empty and null inputs safely")
    void shouldHandleNullAndEmpty() {
        assertEquals("", engine.render(null, Map.of()));
        assertEquals("Hello {{name}}", engine.render("Hello {{name}}", null));
    }
}
