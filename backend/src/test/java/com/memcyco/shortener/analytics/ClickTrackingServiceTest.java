package com.memcyco.shortener.analytics;

import static org.assertj.core.api.Assertions.assertThat;

import com.memcyco.shortener.link.ShortLink;
import com.memcyco.shortener.link.ShortLinkRepository;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class ClickTrackingServiceTest {
  @Autowired
  ClickTrackingService clickTrackingService;

  @Autowired
  ClickEventRepository clickEventRepository;

  @Autowired
  ShortLinkRepository shortLinkRepository;

  @Autowired
  CacheManager cacheManager;

  @Test
  void tracksClickTruncatesMetadataIncrementsCountAndKeepsUnlimitedLinkCached() throws Exception {
    ShortLink link = saveLink("track-me");
    Cache cache = cacheManager.getCache("shortLinks");
    assertThat(cache).isNotNull();
    Object cachedTarget = new Object();
    cache.put("track-me", cachedTarget);

    clickTrackingService.track(
        link.getId(),
        "track-me",
        " ",
        "A".repeat(1100),
        "1".repeat(140)
    );

    ClickEvent event = awaitEvent(link.getId());
    ShortLink updated = shortLinkRepository.findById(link.getId()).orElseThrow();

    assertThat(event.getReferer()).isNull();
    assertThat(event.getUserAgent()).hasSize(1024);
    assertThat(event.getIpAddress()).hasSize(128);
    assertThat(updated.getClickCount()).isEqualTo(1);
    assertThat(cache.get("track-me").get()).isSameAs(cachedTarget);
  }

  @Test
  void evictsCacheForClickLimitedLinks() throws Exception {
    ShortLink link = saveLink("limited-track");
    link.setMaxClicks(2L);
    shortLinkRepository.save(link);
    Cache cache = cacheManager.getCache("shortLinks");
    assertThat(cache).isNotNull();
    cache.put("limited-track", "stale-target");

    clickTrackingService.track(link.getId(), "limited-track", null, null, null);

    awaitEvent(link.getId());
    assertThat(cache.get("limited-track")).isNull();
  }

  @Test
  void ignoresMissingLink() throws Exception {
    clickTrackingService.track(999999L, "missing", "ref", "agent", "ip");

    Thread.sleep(200);

    assertThat(clickEventRepository.findAll()).noneMatch(event -> "ref".equals(event.getReferer()));
  }

  private ShortLink saveLink(String code) {
    ShortLink link = new ShortLink();
    link.setShortCode(code);
    link.setOriginalUrl("https://example.com/" + code);
    link.setStrategy("random_base62");
    return shortLinkRepository.save(link);
  }

  private ClickEvent awaitEvent(Long linkId) throws InterruptedException {
    long deadline = System.nanoTime() + Duration.ofSeconds(3).toNanos();
    while (System.nanoTime() < deadline) {
      var events = clickEventRepository.findByShortLinkIdOrderByClickedAtAsc(linkId);
      if (!events.isEmpty()) {
        return events.get(0);
      }
      Thread.sleep(50);
    }
    throw new AssertionError("Timed out waiting for click event");
  }
}
