package com.wizz.card_management.ratelimit;

import org.springframework.stereotype.Service;

import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class RateLimitService {

    private static final int MAX_REQUESTS = 100;
    private static final long WINDOW_MILLIS = 60_000;

    private final ConcurrentHashMap<String, RateLimitEntry> clients =
            new ConcurrentHashMap<>();


    private static final Logger log =
            LoggerFactory.getLogger(RateLimitService.class);
    public boolean isAllowed(String partnerId) {

        long now = System.currentTimeMillis();

        RateLimitEntry entry = clients.compute(
                partnerId,
                (key, existing) -> {

                    if (existing == null ||
                            now - existing.windowStart >= WINDOW_MILLIS) {

                        return new RateLimitEntry(now, 1);
                    }

                    existing.requestCount++;
                    return existing;
                }
        );

        boolean allowed = entry.requestCount <= MAX_REQUESTS;

        if (!allowed) {
            log.warn("Rate limit exceeded for partnerId={}", partnerId);
        }

        return allowed;
    }

    private static class RateLimitEntry {

        private final long windowStart;
        private int requestCount;

        private RateLimitEntry(
                long windowStart,
                int requestCount) {

            this.windowStart = windowStart;
            this.requestCount = requestCount;
        }
    }
}