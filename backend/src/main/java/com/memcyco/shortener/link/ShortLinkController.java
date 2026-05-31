package com.memcyco.shortener.link;

import com.memcyco.shortener.analytics.AnalyticsResponse;
import com.memcyco.shortener.analytics.AnalyticsService;
import com.memcyco.shortener.strategy.StrategyResponse;
import com.memcyco.shortener.strategy.StrategyService;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class ShortLinkController {
  private final ShortLinkService shortLinkService;
  private final StrategyService strategyService;
  private final AnalyticsService analyticsService;

  public ShortLinkController(
      ShortLinkService shortLinkService,
      StrategyService strategyService,
      AnalyticsService analyticsService
  ) {
    this.shortLinkService = shortLinkService;
    this.strategyService = strategyService;
    this.analyticsService = analyticsService;
  }

  @GetMapping("/links")
  List<ShortLinkResponse> list() {
    return shortLinkService.list();
  }

  @PostMapping("/links")
  ResponseEntity<ShortLinkResponse> create(@Valid @RequestBody ShortLinkRequest request) {
    ShortLinkResponse response = shortLinkService.create(request);
    return ResponseEntity.created(URI.create("/api/links/" + response.id())).body(response);
  }

  @GetMapping("/links/{id}")
  ShortLinkResponse get(@PathVariable Long id) {
    return shortLinkService.get(id);
  }

  @PutMapping("/links/{id}")
  ShortLinkResponse update(@PathVariable Long id, @Valid @RequestBody ShortLinkRequest request) {
    return shortLinkService.update(id, request);
  }

  @DeleteMapping("/links/{id}")
  ResponseEntity<Void> delete(@PathVariable Long id) {
    shortLinkService.delete(id);
    return ResponseEntity.noContent().build();
  }

  @GetMapping("/links/{id}/analytics")
  AnalyticsResponse analytics(@PathVariable Long id) {
    return analyticsService.analyticsFor(id);
  }

  @GetMapping("/strategies")
  List<StrategyResponse> strategies() {
    return strategyService.strategies();
  }
}
