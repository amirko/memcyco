package com.memcyco.shortener.redirect;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class RedirectRateLimiterTest {
  @Test
  void allowsRequestsUntilLimitIsReached() {
    RedirectRateLimiter limiter = new RedirectRateLimiter(2, 60);

    assertThat(limiter.allow("203.0.113.20")).isTrue();
    assertThat(limiter.allow("203.0.113.20")).isTrue();
    assertThat(limiter.allow("203.0.113.20")).isFalse();
  }

  @Test
  void tracksDifferentClientsIndependently() {
    RedirectRateLimiter limiter = new RedirectRateLimiter(1, 60);

    assertThat(limiter.allow("203.0.113.20")).isTrue();
    assertThat(limiter.allow("203.0.113.21")).isTrue();
    assertThat(limiter.allow("203.0.113.20")).isFalse();
  }

  @Test
  void nonPositiveLimitDisablesRateLimiting() {
    RedirectRateLimiter limiter = new RedirectRateLimiter(0, 60);

    assertThat(limiter.allow("203.0.113.20")).isTrue();
    assertThat(limiter.allow("203.0.113.20")).isTrue();
  }
}
