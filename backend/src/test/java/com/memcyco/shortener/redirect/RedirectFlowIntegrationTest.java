package com.memcyco.shortener.redirect;

import static org.assertj.core.api.Assertions.assertThat;

import com.memcyco.shortener.analytics.AnalyticsService;
import com.memcyco.shortener.link.ShortLinkRequest;
import com.memcyco.shortener.link.ShortLinkResponse;
import com.memcyco.shortener.link.ShortLinkService;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class RedirectFlowIntegrationTest {
  @LocalServerPort
  int port;

  @Autowired
  ShortLinkService shortLinkService;

  @Autowired
  AnalyticsService analyticsService;

  @Autowired
  TestRestTemplate restTemplate;

  @Test
  void redirectsAndTracksClickAsynchronously() throws Exception {
    ShortLinkResponse link = shortLinkService.create(new ShortLinkRequest(
        "https://example.com/landing",
        "redir",
        "random_base62",
        null,
        null,
        null,
        null
    ));

    ResponseEntity<Void> response = restTemplate.exchange(
        "http://localhost:" + port + "/redir",
        HttpMethod.GET,
        null,
        Void.class
    );

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FOUND);
    assertThat(response.getHeaders().getFirst(HttpHeaders.LOCATION)).isEqualTo("https://example.com/landing");

    awaitClick(link.id());
    assertThat(analyticsService.analyticsFor(link.id()).totalClicks()).isEqualTo(1);
  }

  private void awaitClick(Long linkId) throws InterruptedException {
    long deadline = System.nanoTime() + Duration.ofSeconds(3).toNanos();
    while (System.nanoTime() < deadline) {
      if (analyticsService.analyticsFor(linkId).totalClicks() == 1) {
        return;
      }
      Thread.sleep(100);
    }
  }
}
