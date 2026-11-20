package com.booking.app;

import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;

class ArchitectureTest {

    @Test
    void verifyModularBoundaries() {
        ApplicationModules modules = ApplicationModules.of(BookingApplication.class);
        modules.verify();
    }
}
