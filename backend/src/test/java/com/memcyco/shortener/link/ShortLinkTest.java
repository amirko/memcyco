package com.memcyco.shortener.link;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class ShortLinkTest {
  @Test
  void statusPrefersExpiredBeforeClickExhausted() {
    Instant now = Instant.parse("2026-05-31T10:00:00Z");
    ShortLink link = new ShortLink();
    link.setExpiresAt(now);
    link.setMaxClicks(10L);
    link.setClickCount(10);

    assertThat(link.isExpired(now)).isTrue();
    assertThat(link.isClickExhausted()).isTrue();
    assertThat(link.status(now)).isEqualTo("expired");
  }

  @Test
  void statusReportsClickExhaustedWhenMaxClicksReached() {
    ShortLink link = new ShortLink();
    link.setMaxClicks(3L);
    link.setClickCount(3);

    assertThat(link.status(Instant.now())).isEqualTo("click_exhausted");
  }

  @Test
  void statusReportsActiveWhenNoLimitsAreReached() {
    ShortLink link = new ShortLink();
    link.setExpiresAt(Instant.now().plusSeconds(60));
    link.setMaxClicks(2L);
    link.setClickCount(1);

    assertThat(link.status(Instant.now())).isEqualTo("active");
  }

  @Test
  void setTagsDefensivelyCopiesInputAndHandlesNull() {
    ShortLink link = new ShortLink();
    List<String> tags = new ArrayList<>(List.of("one"));

    link.setTags(tags);
    tags.add("two");

    assertThat(link.getTags()).containsExactly("one");

    link.setTags(null);

    assertThat(link.getTags()).isEmpty();
  }

  @Test
  void lifecycleCallbacksSetCreateAndUpdateTimestamps() throws InterruptedException {
    ShortLink link = new ShortLink();

    link.beforeCreate();
    Instant createdAt = link.getCreatedAt();
    Instant firstUpdatedAt = link.getUpdatedAt();
    Thread.sleep(2);
    link.beforeUpdate();

    assertThat(createdAt).isNotNull();
    assertThat(firstUpdatedAt).isNotNull();
    assertThat(link.getUpdatedAt()).isAfterOrEqualTo(firstUpdatedAt);
  }
}
