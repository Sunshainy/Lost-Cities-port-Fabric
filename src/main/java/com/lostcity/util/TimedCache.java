package com.lostcity.util;

import java.util.*;
import java.util.function.Function;
import java.util.function.IntSupplier;

/**
 * Кэш с TTL и ограничением размера: записи удаляются после N секунд неиспользования
 * или при превышении maxSize (LRU eviction).
 * Портировано из mcjty.lostcities.varia.TimedCache с оптимизациями.
 */
public class TimedCache<K, V> {

    private static final class Entry<V> {
        final V value;
        long lastAccess;

        Entry(V value, long lastAccess) {
            this.value = value;
            this.lastAccess = lastAccess;
        }
    }

    private final Map<K, Entry<V>> cache = new LinkedHashMap<>();  // LinkedHashMap для LRU
    private final IntSupplier ttlSecondsSupplier;
    private final int maxSize;  // Максимальный размер кэша (0 = без ограничений)
    private long nextCleanupAt;

    public TimedCache(IntSupplier ttlSecondsSupplier) {
        this(ttlSecondsSupplier, 10000);  // По умолчанию 10000 записей
    }
    
    public TimedCache(IntSupplier ttlSecondsSupplier, int maxSize) {
        this.ttlSecondsSupplier = ttlSecondsSupplier;
        this.maxSize = maxSize;
        this.nextCleanupAt = System.currentTimeMillis();
    }

    public void clear() {
        cache.clear();
    }

    public V get(K key) {
        long now = System.currentTimeMillis();
        Entry<V> entry = cache.get(key);
        if (entry == null) {
            maybeCleanup(now);
            return null;
        }
        if (isExpired(entry, now)) {
            cache.remove(key);
            maybeCleanup(now);
            return null;
        }
        entry.lastAccess = now;
        maybeCleanup(now);
        return entry.value;
    }

    public void put(K key, V value) {
        long now = System.currentTimeMillis();
        cache.put(key, new Entry<>(value, now));
        evictIfNeeded();
        maybeCleanup(now);
    }

    public V computeIfAbsent(K key, Function<K, V> supplier) {
        long now = System.currentTimeMillis();
        Entry<V> entry = cache.get(key);
        if (entry != null) {
            if (isExpired(entry, now)) {
                cache.remove(key);
            } else {
                entry.lastAccess = now;
                maybeCleanup(now);
                return entry.value;
            }
        }
        V value = supplier.apply(key);
        if (value != null) {
            cache.put(key, new Entry<>(value, now));
            evictIfNeeded();
        }
        maybeCleanup(now);
        return value;
    }
    
    /**
     * Удаляет старые записи если размер превышает maxSize (LRU eviction)
     */
    private void evictIfNeeded() {
        if (maxSize <= 0 || cache.size() <= maxSize) return;
        
        int toRemove = cache.size() - maxSize;
        Iterator<K> it = cache.keySet().iterator();
        for (int i = 0; i < toRemove && it.hasNext(); i++) {
            it.next();
            it.remove();
        }
    }

    private boolean isExpired(Entry<V> entry, long now) {
        return now - entry.lastAccess >= getTtlMillis();
    }

    private void maybeCleanup(long now) {
        if (now < nextCleanupAt) {
            return;
        }
        cleanup(now);
        nextCleanupAt = now + getCleanupIntervalMillis();
    }

    private void cleanup(long now) {
        long ttl = getTtlMillis();
        if (ttl <= 0) {
            cache.clear();
            return;
        }
        Iterator<Map.Entry<K, Entry<V>>> it = cache.entrySet().iterator();
        while (it.hasNext()) {
            if (now - it.next().getValue().lastAccess >= ttl) {
                it.remove();
            }
        }
    }

    private long getTtlMillis() {
        try {
            int ttlSeconds = ttlSecondsSupplier.getAsInt();
            if (ttlSeconds <= 0) {
                return 0L;
            }
            return ttlSeconds * 1000L;
        } catch (IllegalStateException e) {
            // Config not loaded yet (e.g., during GUI rendering), use default value
            return 300_000L; // Default 5 minutes (300 seconds)
        } catch (Exception e) {
            // Any other error, use default value
            return 300_000L; // Default 5 minutes (300 seconds)
        }
    }
    
    private long getCleanupIntervalMillis() {
        long ttlMillis = getTtlMillis();
        return Math.max(1000L, ttlMillis / 2);
    }
    
    /** Размер кэша (для отладки/мониторинга) */
    public int size() {
        return cache.size();
    }
}
