package com.guimard.system;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class SimpleCache {
	private final ConcurrentHashMap<String, CacheEntry> cache;
	private final long timeoutMillis;
	private final ScheduledExecutorService scheduler;

	public SimpleCache(long timeoutMillis) {
		this.cache = new ConcurrentHashMap<>();
		this.timeoutMillis = timeoutMillis;
		this.scheduler = Executors.newScheduledThreadPool(1);
		startCleanupTask();
	}

	public String get(String key) {
		CacheEntry entry = cache.get(key);
		if (entry != null) {
			entry.setLastAccessed(System.currentTimeMillis());
			return entry.getValue();
		}
		return null;
	}

	public void set(String key, String value) {
		cache.put(key, new CacheEntry(value));
	}

	public void delete(String key) {
		cache.remove(key);
	}

	private void startCleanupTask() {
		scheduler.scheduleAtFixedRate(this::cleanup, timeoutMillis, timeoutMillis, TimeUnit.MILLISECONDS);
	}

	private void cleanup() {
		long currentTime = System.currentTimeMillis();
		cache.entrySet().removeIf(entry -> (currentTime - entry.getValue().getLastAccessed()) > timeoutMillis);
	}

	private static class CacheEntry {
		private final String value;
		private long lastAccessed;

		public CacheEntry(String value) {
			this.value = value;
			this.lastAccessed = System.currentTimeMillis();
		}

		public String getValue() {
			return value;
		}

		public long getLastAccessed() {
			return lastAccessed;
		}

		public void setLastAccessed(long lastAccessed) {
			this.lastAccessed = lastAccessed;
		}
	}
}
