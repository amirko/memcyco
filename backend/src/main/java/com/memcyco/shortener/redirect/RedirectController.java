package com.memcyco.shortener.redirect;

import com.memcyco.shortener.analytics.ClickTrackingService;
import jakarta.servlet.http.HttpServletRequest;
import java.net.URI;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class RedirectController {
  private final RedirectService redirectService;
  private final ClickTrackingService clickTrackingService;
  private final RedirectRateLimiter rateLimiter;

  public RedirectController(
      RedirectService redirectService,
      ClickTrackingService clickTrackingService,
      RedirectRateLimiter rateLimiter
  ) {
    this.redirectService = redirectService;
    this.clickTrackingService = clickTrackingService;
    this.rateLimiter = rateLimiter;
  }

  @GetMapping("/{shortCode:[A-Za-z0-9_-]{3,64}}")
  public ResponseEntity<Void> redirect(@PathVariable String shortCode, HttpServletRequest request) {
    if (!rateLimiter.allow(clientIp(request))) {
      return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
          .header(HttpHeaders.RETRY_AFTER, Long.toString(rateLimiter.retryAfterSeconds()))
          .build();
    }

    return redirectService.resolve(shortCode)
        .map(target -> buildRedirect(target, request))
        .orElseGet(() -> ResponseEntity.notFound().build());
  }

  private ResponseEntity<Void> buildRedirect(RedirectTarget target, HttpServletRequest request) {
    if (target.expired()) {
      return ResponseEntity.status(HttpStatus.GONE).build();
    }
    if (target.clickExhausted()) {
      return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).build();
    }

    clickTrackingService.track(
        target.id(),
        target.shortCode(),
        request.getHeader(HttpHeaders.REFERER),
        request.getHeader(HttpHeaders.USER_AGENT),
        clientIp(request)
    );
    return ResponseEntity.status(HttpStatus.FOUND).location(URI.create(target.originalUrl())).build();
  }

  private String clientIp(HttpServletRequest request) {
    String forwardedFor = request.getHeader("X-Forwarded-For");
    if (forwardedFor != null && !forwardedFor.isBlank()) {
      return forwardedFor.split(",")[0].trim();
    }
    return request.getRemoteAddr();
  }
}
