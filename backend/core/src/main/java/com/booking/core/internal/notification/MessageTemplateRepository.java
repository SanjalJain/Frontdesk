package com.booking.core.internal.notification;

import com.booking.core.api.notification.MessageTemplateDto;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.springframework.stereotype.Repository;

import java.util.*;

import static org.jooq.impl.DSL.*;

@Repository
public class MessageTemplateRepository {

    private final DSLContext dsl;

    private static final Map<String, String> DEFAULT_TEMPLATES = Map.of(
            "confirmation", "Hello {{customer_name}}, your {{appointment_label}} for {{service_name}} at {{clinic_name}} has been booked for {{date}} at {{start_time}}. To reschedule or cancel, please reply to this message.",
            "reminder_24h", "Reminder: Hello {{customer_name}}, you have an upcoming {{appointment_label}} for {{service_name}} tomorrow, {{date}} at {{start_time}} with {{staff_name}} at {{clinic_name}}. Please arrive 10 minutes early.",
            "reminder_2h", "Quick Reminder: Hello {{customer_name}}, your {{appointment_label}} at {{clinic_name}} starts in 2 hours at {{start_time}}. We look forward to seeing you!",
            "custom", "Hello {{customer_name}}, update regarding your {{appointment_label}} at {{clinic_name}} on {{date}} at {{start_time}}."
    );

    public MessageTemplateRepository(DSLContext dsl) {
        this.dsl = dsl;
    }

    public MessageTemplateDto find(Long tenantId, String kind, String channel, String language) {
        String ch = channel != null ? channel : "whatsapp";
        String lang = language != null ? language : "en";

        var record = dsl.select(
                        field(name("id"), Long.class),
                        field(name("tenant_id"), Long.class),
                        field(name("kind"), String.class),
                        field(name("channel"), String.class),
                        field(name("language"), String.class),
                        field(name("body"), String.class),
                        field(name("is_active"), Boolean.class)
                )
                .from(table(name("core", "message_templates")))
                .where(
                        field(name("tenant_id"), Long.class).eq(tenantId),
                        field(name("kind"), String.class).eq(kind),
                        field(name("channel"), String.class).eq(ch),
                        field(name("language"), String.class).eq(lang)
                )
                .fetchOne();

        if (record != null) {
            return mapToDto(record);
        }

        // Return default fallback template
        String defaultBody = DEFAULT_TEMPLATES.getOrDefault(kind, DEFAULT_TEMPLATES.get("confirmation"));
        return new MessageTemplateDto(null, tenantId, kind, ch, lang, defaultBody, true);
    }

    public List<MessageTemplateDto> listByTenant(Long tenantId) {
        var records = dsl.select(
                        field(name("id"), Long.class),
                        field(name("tenant_id"), Long.class),
                        field(name("kind"), String.class),
                        field(name("channel"), String.class),
                        field(name("language"), String.class),
                        field(name("body"), String.class),
                        field(name("is_active"), Boolean.class)
                )
                .from(table(name("core", "message_templates")))
                .where(field(name("tenant_id"), Long.class).eq(tenantId))
                .fetch(this::mapToDto);

        Map<String, MessageTemplateDto> byKind = new HashMap<>();
        for (var t : records) {
            byKind.put(t.kind(), t);
        }

        // Ensure all default kinds exist in list
        List<MessageTemplateDto> result = new ArrayList<>();
        for (String kind : List.of("confirmation", "reminder_24h", "reminder_2h", "custom")) {
            if (byKind.containsKey(kind)) {
                result.add(byKind.get(kind));
            } else {
                result.add(new MessageTemplateDto(
                        null,
                        tenantId,
                        kind,
                        "whatsapp",
                        "en",
                        DEFAULT_TEMPLATES.getOrDefault(kind, ""),
                        true
                ));
            }
        }
        return result;
    }

    public MessageTemplateDto upsert(
            Long tenantId,
            String kind,
            String channel,
            String language,
            String body,
            boolean isActive
    ) {
        String ch = channel != null ? channel : "whatsapp";
        String lang = language != null ? language : "en";

        var record = dsl.insertInto(table(name("core", "message_templates")))
                .columns(
                        field(name("tenant_id"), Long.class),
                        field(name("kind"), String.class),
                        field(name("channel"), String.class),
                        field(name("language"), String.class),
                        field(name("body"), String.class),
                        field(name("is_active"), Boolean.class)
                )
                .values(tenantId, kind, ch, lang, body, isActive)
                .onConflict(
                        field(name("tenant_id"), Long.class),
                        field(name("kind"), String.class),
                        field(name("channel"), String.class),
                        field(name("language"), String.class)
                )
                .doUpdate()
                .set(field(name("body"), String.class), body)
                .set(field(name("is_active"), Boolean.class), isActive)
                .returning(
                        field(name("id"), Long.class),
                        field(name("tenant_id"), Long.class),
                        field(name("kind"), String.class),
                        field(name("channel"), String.class),
                        field(name("language"), String.class),
                        field(name("body"), String.class),
                        field(name("is_active"), Boolean.class)
                )
                .fetchOne();

        return mapToDto(record);
    }

    private MessageTemplateDto mapToDto(Record r) {
        if (r == null) return null;
        return new MessageTemplateDto(
                r.get(field(name("id"), Long.class)),
                r.get(field(name("tenant_id"), Long.class)),
                r.get(field(name("kind"), String.class)),
                r.get(field(name("channel"), String.class)),
                r.get(field(name("language"), String.class)),
                r.get(field(name("body"), String.class)),
                Boolean.TRUE.equals(r.get(field(name("is_active"), Boolean.class)))
        );
    }
}
