package com.todoapp.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory token blacklist for invalidated JWTs (e.g. after logout).
 *
 * <p>Entries are keyed by the JWT's {@code jti} claim and stored with their
 * expiry timestamp (epoch millis). Expired entries are cleaned up every minute
 * by a scheduled task so the map does not grow unboundedly.
 *
 * <p><strong>Note:</strong> This implementation is suitable for single-instance
 * deployments only. For multi-instance deployments, replace with a Redis-backed
 * store.
 */
@Slf4j
@Service
public class TokenBlacklistService {

    /** jti → expiry epoch millis */
    private final ConcurrentHashMap<String, Long> blacklist = new ConcurrentHashMap<>();

    /**
     * Adds a JWT ID to the blacklist until its natural expiry.
     *
     * @param jti         the JWT ID ({@code jti} claim)
     * @param expiryMillis the token's expiry as epoch milliseconds
     */
    public void blacklist(String jti, long expiryMillis) {
        blacklist.put(jti, expiryMillis);
        log.debug("Blacklisted token jti={} until {}", jti, expiryMillis);
    }

    /**
     * Checks whether a JWT ID is currently blacklisted (and not yet expired).
     *
     * @param jti the JWT ID to check
     * @return {@code true} if the token is blacklisted and still within its TTL
     */
    public boolean isBlacklisted(String jti) {
        Long expiry = blacklist.get(jti);
        if (expiry == null) {
            return false;
        }
        if (System.currentTimeMillis() > expiry) {
            // Token has naturally expired; remove it eagerly
            blacklist.remove(jti);
            return false;
        }
        return true;
    }

    /**
     * Removes all entries whose TTL has passed. Runs every 60 seconds.
     */
    @Scheduled(fixedDelay = 60_000)
    public void cleanupExpiredEntries() {
        long now = System.currentTimeMillis();
        int removed = 0;
        Iterator<Map.Entry<String, Long>> it = blacklist.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, Long> entry = it.next();
            if (now > entry.getValue()) {
                it.remove();
                removed++;
            }
        }
        if (removed > 0) {
            log.debug("Cleaned up {} expired blacklist entries", removed);
        }
    }
}
