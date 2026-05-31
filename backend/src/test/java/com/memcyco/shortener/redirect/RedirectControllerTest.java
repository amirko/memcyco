package com.memcyco.shortener.redirect;

import static org.assertj.core.api.Assertions.assertThat;

import com.memcyco.shortener.analytics.ClickTrackingService;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;

class RedirectControllerTest {
  @Test
  void activeTargetRedirectsAndTracksRequestMetadata() {
    CapturingClickTrackingService tracking = new CapturingClickTrackingService();
    RedirectController controller = new RedirectController(
        resolving(new RedirectTarget(9L, "go", "https://example.com/target", null, 10L, 3)),
        tracking,
        allowingRateLimiter()
    );
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.addHeader(HttpHeaders.REFERER, "https://referer.example");
    request.addHeader(HttpHeaders.USER_AGENT, "JUnit");
    request.addHeader("X-Forwarded-For", "203.0.113.8, 198.51.100.4");

    ResponseEntity<Void> response = controller.redirect("go", request);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FOUND);
    assertThat(response.getHeaders().getLocation()).hasToString("https://example.com/target");
    assertThat(tracking.shortLinkId).isEqualTo(9L);
    assertThat(tracking.shortCode).isEqualTo("go");
    assertThat(tracking.referer).isEqualTo("https://referer.example");
    assertThat(tracking.userAgent).isEqualTo("JUnit");
    assertThat(tracking.ipAddress).isEqualTo("203.0.113.8");
  }

  @Test
  void missingTargetReturnsNotFoundWithoutTracking() {
    CapturingClickTrackingService tracking = new CapturingClickTrackingService();
    RedirectController controller = new RedirectController(resolving(null), tracking, allowingRateLimiter());

    ResponseEntity<Void> response = controller.redirect("missing", new MockHttpServletRequest());

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    assertThat(tracking.shortLinkId).isNull();
  }

  @Test
  void expiredTargetReturnsGoneWithoutTracking() {
    CapturingClickTrackingService tracking = new CapturingClickTrackingService();
    RedirectController controller = new RedirectController(
        resolving(new RedirectTarget(1L, "old", "https://example.com", Instant.now().minusSeconds(1), null, 0)),
        tracking,
        allowingRateLimiter()
    );

    ResponseEntity<Void> response = controller.redirect("old", new MockHttpServletRequest());

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.GONE);
    assertThat(tracking.shortLinkId).isNull();
  }

  @Test
  void clickExhaustedTargetReturnsTooManyRequestsWithoutTracking() {
    CapturingClickTrackingService tracking = new CapturingClickTrackingService();
    RedirectController controller = new RedirectController(
        resolving(new RedirectTarget(1L, "maxed", "https://example.com", null, 1L, 1)),
        tracking,
        allowingRateLimiter()
    );

    ResponseEntity<Void> response = controller.redirect("maxed", new MockHttpServletRequest());

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
    assertThat(tracking.shortLinkId).isNull();
  }

  @Test
  void rateLimitedRequestReturnsTooManyRequestsWithoutResolvingOrTracking() {
    CapturingClickTrackingService tracking = new CapturingClickTrackingService();
    CountingRedirectService redirectService = new CountingRedirectService();
    RedirectController controller = new RedirectController(redirectService, tracking, new RedirectRateLimiter(1, 60));
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.setRemoteAddr("203.0.113.10");

    assertThat(controller.redirect("go", request).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    ResponseEntity<Void> limited = controller.redirect("go", request);

    assertThat(limited.getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
    assertThat(limited.getHeaders().getFirst(HttpHeaders.RETRY_AFTER)).isEqualTo("60");
    assertThat(redirectService.calls).isEqualTo(1);
    assertThat(tracking.shortLinkId).isNull();
  }

  private RedirectService resolving(RedirectTarget target) {
    return new RedirectService(null) {
      @Override
      public Optional<RedirectTarget> resolve(String shortCode) {
        return Optional.ofNullable(target);
      }
    };
  }

  private RedirectRateLimiter allowingRateLimiter() {
    return new RedirectRateLimiter(0, 60);
  }

  private static class CountingRedirectService extends RedirectService {
    int calls;

    CountingRedirectService() {
      super(null);
    }

    @Override
    public Optional<RedirectTarget> resolve(String shortCode) {
      calls++;
      return Optional.empty();
    }
  }

  private static class CapturingClickTrackingService extends ClickTrackingService {
    Long shortLinkId;
    String shortCode;
    String referer;
    String userAgent;
    String ipAddress;

    CapturingClickTrackingService() {
      super(null, null, null);
    }

    @Override
    public void track(Long shortLinkId, String shortCode, String referer, String userAgent, String ipAddress) {
      this.shortLinkId = shortLinkId;
      this.shortCode = shortCode;
      this.referer = referer;
      this.userAgent = userAgent;
      this.ipAddress = ipAddress;
    }
  }
}
