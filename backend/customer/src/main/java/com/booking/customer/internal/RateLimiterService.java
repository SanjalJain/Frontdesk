package com.booking.customer.internal;

import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class RateLimiterService {

    private static final int MAX_REQUESTS_PER_MINUTE = 15;
    private static final long WINDOW_MS = 60_000L;

    private static class RequestCounter {
        long windowStart;
        AtomicInteger count;

        RequestCounter(long windowStart) {
            this.windowStart = windowStart;
            this.count = new AtomicInteger(1);
        }
    }

    private final Map<String, RequestCounter> counters = new ConcurrentHashMap<>();

    public boolean tryAcquire(String ipAddress) {
        if (ipAddress == null || ipAddress.isBlank()) {
            return true;
        }

        long now = System.currentTimeMillis();

        RequestCounter counter = counters.compute(ipAddress, (ip, current) -> {
            if (current == null || (now - current.windowStart) > WINDOW_MS) {
                return new RequestCounter(now);
            } else {
                current.count.incrementAndGet();
                return current;
            }
        });

        // Periodic cleanup if map grows
        if (counters.size() > 5000) {
            counters.entrySet().removeIf(entry -> (now - entry.getValue().windowStart) > WINDOW_MS * 2);
        }

        return counter.count.get() <= MAX_REQUESTS_PER_MINUTE;
    }
}
