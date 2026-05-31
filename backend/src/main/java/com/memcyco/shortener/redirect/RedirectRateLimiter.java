package com.memcyco.shortener.redirect;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class RedirectRateLimiter {
  private final int maxRequests;
  private final Duration window;
  private final Cache<String, AtomicInteger> counters;

  public RedirectRateLimiter(
      @Value("${app.rate-limit.redirects-per-window:120}") int maxRequests,
      @Value("${app.rate-limit.window-seconds:60}") long windowSeconds
  ) {
    this.maxRequests = maxRequests;
    this.window = Duration.ofSeconds(windowSeconds);
    this.counters = Caffeine.newBuilder()
        .expireAfterWrite(window)
        .maximumSize(100_000)
        .build();
  }

  public boolean allow(String clientIp) {
    if (maxRequests <= 0) {
      return true;
    }
    String key = clientIp == null || clientIp.isBlank() ? "unknown" : clientIp;
    AtomicInteger counter = counters.get(key, ignored -> new AtomicInteger());
    return counter.incrementAndGet() <= maxRequests;
  }

  public long retryAfterSeconds() {
    return Math.max(1, window.toSeconds());
  }
}
