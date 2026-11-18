package com.smartwater.cache;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.concurrent.TimeUnit;

/**
 * Centralized cache manager using Caffeine for in-memory caching.
 * Improves performance by reducing database queries and file I/O operations.
 */
public class CacheManager {
    private static final Logger logger = LoggerFactory.getLogger(CacheManager.class);
    private static CacheManager instance;

    // Cache for household profiles (key: profile ID or name)
    private final Cache<String, Object> profileCache;

    // Cache for water records (key: date)
    private final Cache<String, Object> recordCache;

    // Cache for computed analytics (key: "weekly", "monthly", etc.)
    private final Cache<String, Object> analyticsCache;

    /**
     * Private constructor for singleton pattern.
     */
    private CacheManager() {
        // Profile cache: 1 hour expiry, max 100 entries
        this.profileCache = Caffeine.newBuilder()
                .expireAfterWrite(1, TimeUnit.HOURS)
                .maximumSize(100)
                .recordStats()
                .build();

        // Record cache: 30 minutes expiry, max 1000 entries
        this.recordCache = Caffeine.newBuilder()
                .expireAfterWrite(30, TimeUnit.MINUTES)
                .maximumSize(1000)
                .recordStats()
                .build();

        // Analytics cache: 15 minutes expiry, max 50 entries
        this.analyticsCache = Caffeine.newBuilder()
                .expireAfterWrite(15, TimeUnit.MINUTES)
                .maximumSize(50)
                .recordStats()
                .build();

        logger.info("CacheManager initialized with Caffeine");
    }

    /**
     * Gets the singleton instance of CacheManager.
     *
     * @return the CacheManager instance
     */
    public static synchronized CacheManager getInstance() {
        if (instance == null) {
            instance = new CacheManager();
        }
        return instance;
    }

    /**
     * Gets a value from the profile cache.
     *
     * @param key the cache key
     * @return the cached value, or null if not found
     */
    public Object getProfileCache(String key) {
        logger.debug("Getting profile cache for key: {}", key);
        return profileCache.getIfPresent(key);
    }

    /**
     * Puts a value into the profile cache.
     *
     * @param key   the cache key
     * @param value the value to cache
     */
    public void putProfileCache(String key, Object value) {
        logger.debug("Putting profile cache for key: {}", key);
        profileCache.put(key, value);
    }

    /**
     * Gets a value from the record cache.
     *
     * @param key the cache key
     * @return the cached value, or null if not found
     */
    public Object getRecordCache(String key) {
        logger.debug("Getting record cache for key: {}", key);
        return recordCache.getIfPresent(key);
    }

    /**
     * Puts a value into the record cache.
     *
     * @param key   the cache key
     * @param value the value to cache
     */
    public void putRecordCache(String key, Object value) {
        logger.debug("Putting record cache for key: {}", key);
        recordCache.put(key, value);
    }

    /**
     * Gets a value from the analytics cache.
     *
     * @param key the cache key
     * @return the cached value, or null if not found
     */
    public Object getAnalyticsCache(String key) {
        logger.debug("Getting analytics cache for key: {}", key);
        return analyticsCache.getIfPresent(key);
    }

    /**
     * Puts a value into the analytics cache.
     *
     * @param key   the cache key
     * @param value the value to cache
     */
    public void putAnalyticsCache(String key, Object value) {
        logger.debug("Putting analytics cache for key: {}", key);
        analyticsCache.put(key, value);
    }

    /**
     * Invalidates all entries in the profile cache.
     */
    public void invalidateProfileCache() {
        profileCache.invalidateAll();
        logger.info("Profile cache invalidated");
    }

    /**
     * Invalidates all entries in the record cache.
     */
    public void invalidateRecordCache() {
        recordCache.invalidateAll();
        logger.info("Record cache invalidated");
    }

    /**
     * Invalidates all entries in the analytics cache.
     */
    public void invalidateAnalyticsCache() {
        analyticsCache.invalidateAll();
        logger.info("Analytics cache invalidated");
    }

    /**
     * Invalidates all caches.
     */
    public void invalidateAll() {
        profileCache.invalidateAll();
        recordCache.invalidateAll();
        analyticsCache.invalidateAll();
        logger.info("All caches invalidated");
    }

    /**
     * Gets cache statistics for monitoring and debugging.
     *
     * @return cache statistics as a string
     */
    public String getCacheStats() {
        return String.format(
                "Profile Cache: %s | Record Cache: %s | Analytics Cache: %s",
                profileCache.stats(),
                recordCache.stats(),
                analyticsCache.stats());
    }

    /**
     * Clears all caches and logs statistics.
     */
    public void clearAll() {
        invalidateAll();
        logger.info("All caches cleared. Stats: {}", getCacheStats());
    }
}
