package com.memcyco.shortener.analytics;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.memcyco.shortener.link.ShortLink;
import com.memcyco.shortener.link.ShortLinkRepository;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class AnalyticsServiceTest {
  @Autowired
  AnalyticsService analyticsService;

  @Autowired
  ShortLinkRepository shortLinkRepository;

  @Autowired
  ClickEventRepository clickEventRepository;

  @Test
  void groupsClicksByDayRefererAndUserAgent() {
    ShortLink link = saveLink("analytics-link");
    saveEvent(link, "2026-05-30T22:15:00Z", "", "Agent B");
    saveEvent(link, "2026-05-31T01:15:00Z", "https://ref.example", "Agent A");
    saveEvent(link, "2026-05-31T02:15:00Z", "https://ref.example", null);

    AnalyticsResponse response = analyticsService.analyticsFor(link.getId());

    assertThat(response.totalClicks()).isEqualTo(3);
    assertThat(response.timeSeries()).containsExactly(
        new MetricPoint("2026-05-30", 1),
        new MetricPoint("2026-05-31", 2)
    );
    assertThat(response.referers()).containsExactly(
        new MetricPoint("https://ref.example", 2),
        new MetricPoint("direct", 1)
    );
    assertThat(response.userAgents()).containsExactly(
        new MetricPoint("Agent A", 1),
        new MetricPoint("Agent B", 1),
        new MetricPoint("unknown", 1)
    );
  }

  @Test
  void returnsOnlyTopTenReferersAndUserAgents() {
    ShortLink link = saveLink("top-ten-link");
    for (int i = 0; i < 12; i++) {
      saveEvent(link, "2026-05-31T00:00:00Z", "ref-" + i, "agent-" + i);
    }

    AnalyticsResponse response = analyticsService.analyticsFor(link.getId());

    assertThat(response.referers()).hasSize(10);
    assertThat(response.userAgents()).hasSize(10);
  }

  @Test
  void rejectsAnalyticsForMissingLink() {
    assertThatThrownBy(() -> analyticsService.analyticsFor(999999L))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("not found");
  }

  private ShortLink saveLink(String code) {
    ShortLink link = new ShortLink();
    link.setShortCode(code);
    link.setOriginalUrl("https://example.com/" + code);
    link.setStrategy("random_base62");
    link.setTags(List.of("analytics"));
    return shortLinkRepository.save(link);
  }

  private void saveEvent(ShortLink link, String clickedAt, String referer, String userAgent) {
    ClickEvent event = new ClickEvent();
    event.setShortLink(link);
    event.setClickedAt(Instant.parse(clickedAt));
    event.setReferer(referer);
    event.setUserAgent(userAgent);
    event.setIpAddress("127.0.0.1");
    clickEventRepository.save(event);
  }
}
