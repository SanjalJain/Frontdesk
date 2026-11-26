package com.booking.core.internal.notification;

import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class TemplateEngine {

    public String render(String template, Map<String, String> variables) {
        if (template == null) return "";
        if (variables == null || variables.isEmpty()) return template;

        String result = template;
        for (Map.Entry<String, String> entry : variables.entrySet()) {
            String placeholder = "{{" + entry.getKey() + "}}";
            String value = entry.getValue() != null ? entry.getValue() : "";
            result = result.replace(placeholder, value);
        }
        return result;
    }
}
