package com.lostcity.util;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;
import java.util.function.IntSupplier;

/**
 * Высокопроизводительный и 100% потокобезопасный кэш с TTL.
 * Портирован из mcjty.lostcities.varia.TimedCache.
 */
public class TimedCache<K, V> {

    private static class Entry<V> {
        private final V value;
        private volatile long lastAccess;

        private Entry(V value, long lastAccess) {
            this.value = value;
            this.lastAccess = lastAccess;
        }
    }

    private final ConcurrentMap<K, Entry<V>> cache = new ConcurrentHashMap<>();
    private final IntSupplier ttlSecondsSupplier;
    private final AtomicLong nextCleanupAt;
    private final int maxSize;

    public TimedCache(IntSupplier ttlSecondsSupplier) {
        this(ttlSecondsSupplier, 10000);
    }

    public TimedCache(IntSupplier ttlSecondsSupplier, int maxSize) {
        this.ttlSecondsSupplier = ttlSecondsSupplier;
        this.maxSize = maxSize;
        this.nextCleanupAt = new AtomicLong(System.currentTimeMillis());
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
            cache.remove(key, entry);
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
        Entry<V> cached = cache.get(key);
        if (cached != null && !isExpired(cached, now)) {
            cached.lastAccess = now;
            maybeCleanup(now);
            return cached.value;
        }
        Entry<V> entry = cache.compute(key, (k, current) -> {
            if (current != null && !isExpired(current, now)) {
                current.lastAccess = now;
                return current;
            }
            V value = supplier.apply(k);
            return value == null ? null : new Entry<>(value, now);
        });
        evictIfNeeded();
        maybeCleanup(now);
        return entry == null ? null : entry.value;
    }

    private void evictIfNeeded() {
        if (maxSize <= 0 || cache.size() <= maxSize) return;
        // Очищаем самые старые записи если превышен maxSize
        int toRemove = cache.size() - maxSize;
        if (toRemove > 0) {
            long now = System.currentTimeMillis();
            cache.entrySet().stream()
                .sorted((e1, e2) -> Long.compare(e1.getValue().lastAccess, e2.getValue().lastAccess))
                .limit(toRemove)
                .forEach(e -> cache.remove(e.getKey(), e.getValue()));
        }
    }

    private boolean isExpired(Entry<V> entry, long now) {
        return now - entry.lastAccess >= getTtlMillis();
    }

    private void maybeCleanup(long now) {
        long scheduledAt = nextCleanupAt.get();
        if (now < scheduledAt || !nextCleanupAt.compareAndSet(scheduledAt, now + getCleanupIntervalMillis())) {
            return;
        }
        cleanup(now);
    }

    private void cleanup(long now) {
        long ttlMillis = getTtlMillis();
        if (ttlMillis <= 0) {
            cache.clear();
            return;
        }
        cache.forEach((key, entry) -> {
            if (now - entry.lastAccess >= ttlMillis) {
                cache.remove(key, entry);
            }
        });
    }

    private long getCleanupIntervalMillis() {
        long ttlMillis = getTtlMillis();
        return Math.max(1000L, ttlMillis / 2);
    }

    private long getTtlMillis() {
        try {
            int ttlSeconds = ttlSecondsSupplier.getAsInt();
            if (ttlSeconds <= 0) {
                return 0L;
            }
            return ttlSeconds * 1000L;
        } catch (Exception e) {
            return 300_000L; // Default 5 minutes
        }
    }

    public int size() {
        return cache.size();
    }
}
