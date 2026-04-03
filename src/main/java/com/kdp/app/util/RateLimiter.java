// language: java
package com.kdp.app.util;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public final class RateLimiter {

    private final Map<String, UserRequest> userRequestCounts = new ConcurrentHashMap<>();
    private final int limit;
    private final long windowNanos;
    private final ScheduledExecutorService cleaner;

    public RateLimiter(int limit, Duration window, Duration cleanupInterval) {
        if (limit <= 0) throw new IllegalArgumentException("limit must be > 0");
        this.limit = limit;
        this.windowNanos = window.toNanos();
        this.cleaner = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "rate-limiter-cleaner");
            t.setDaemon(true);
            return t;
        });
        // periodic cleanup to avoid memory leak
        long cleanupMs = Math.max(1, cleanupInterval.toMillis());
        cleaner.scheduleAtFixedRate(this::cleanup, cleanupMs, cleanupMs, TimeUnit.MILLISECONDS);
    }

    public boolean isAllowed(String userId) {
        final long now = System.nanoTime();
        final AtomicBoolean allowed = new AtomicBoolean(false);

        userRequestCounts.compute(userId, (key, current) -> {
            if (current == null || (now - current.windowStartNanos) > windowNanos) {
                allowed.set(true);
                return new UserRequest(now, 1);
            }

            if (current.requestCount < limit) {
                allowed.set(true);
                // return a new immutable entry with incremented count
                return new UserRequest(current.windowStartNanos, current.requestCount + 1);
            }

            // over limit -> keep current entry, do not increment
            allowed.set(false);
            return current;
        });

        return allowed.get();
    }

    private void cleanup() {
        final long now = System.nanoTime();
        for (Map.Entry<String, UserRequest> e : userRequestCounts.entrySet()) {
            UserRequest ur = e.getValue();
            if (now - ur.windowStartNanos > windowNanos) {
                userRequestCounts.remove(e.getKey(), ur);
            }
        }
    }

    public void shutdown() {
        cleaner.shutdownNow();
    }

    private static final class UserRequest {
        final long windowStartNanos;
        final long requestCount;

        UserRequest(long windowStartNanos, long requestCount) {
            this.windowStartNanos = windowStartNanos;
            this.requestCount = requestCount;
        }
    }
}
