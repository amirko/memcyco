package com.memcyco.shortener.redirect;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import org.junit.jupiter.api.Test;

class RedirectTargetTest {
  @Test
  void detectsExpiredTargets() {
    RedirectTarget target = new RedirectTarget(1L, "abc", "https://example.com", Instant.now().minusSeconds(1), null, 0);

    assertThat(target.expired()).isTrue();
    assertThat(target.clickExhausted()).isFalse();
  }

  @Test
  void detectsClickExhaustedTargets() {
    RedirectTarget target = new RedirectTarget(1L, "abc", "https://example.com", null, 2L, 2);

    assertThat(target.expired()).isFalse();
    assertThat(target.clickExhausted()).isTrue();
  }
}
