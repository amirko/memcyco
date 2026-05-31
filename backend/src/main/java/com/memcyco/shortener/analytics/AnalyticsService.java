package com.memcyco.shortener.analytics;

import com.memcyco.shortener.link.ShortLinkRepository;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AnalyticsService {
  private static final DateTimeFormatter DAY_FORMAT = DateTimeFormatter.ISO_LOCAL_DATE.withZone(ZoneOffset.UTC);

  private final ClickEventRepository clickEventRepository;
  private final ShortLinkRepository shortLinkRepository;

  public AnalyticsService(ClickEventRepository clickEventRepository, ShortLinkRepository shortLinkRepository) {
    this.clickEventRepository = clickEventRepository;
    this.shortLinkRepository = shortLinkRepository;
  }

  @Transactional(readOnly = true)
  public AnalyticsResponse analyticsFor(Long shortLinkId) {
    if (!shortLinkRepository.existsById(shortLinkId)) {
      throw new IllegalArgumentException("Short link not found");
    }
    List<ClickEvent> events = clickEventRepository.findByShortLinkIdOrderByClickedAtAsc(shortLinkId);
    return new AnalyticsResponse(
        events.size(),
        byDay(events),
        top(events, event -> emptyAsDirect(event.getReferer())),
        top(events, event -> emptyAsUnknown(event.getUserAgent()))
    );
  }

  private List<MetricPoint> byDay(List<ClickEvent> events) {
    Map<String, Long> counts = events.stream()
        .collect(Collectors.groupingBy(event -> DAY_FORMAT.format(event.getClickedAt()), LinkedHashMap::new, Collectors.counting()));
    return counts.entrySet().stream()
        .map(entry -> new MetricPoint(entry.getKey(), entry.getValue()))
        .toList();
  }

  private List<MetricPoint> top(List<ClickEvent> events, Function<ClickEvent, String> classifier) {
    return events.stream()
        .collect(Collectors.groupingBy(classifier, Collectors.counting()))
        .entrySet()
        .stream()
        .sorted(Map.Entry.<String, Long>comparingByValue(Comparator.reverseOrder()).thenComparing(Map.Entry.comparingByKey()))
        .limit(10)
        .map(entry -> new MetricPoint(entry.getKey(), entry.getValue()))
        .toList();
  }

  private String emptyAsDirect(String value) {
    return value == null || value.isBlank() ? "direct" : value;
  }

  private String emptyAsUnknown(String value) {
    return value == null || value.isBlank() ? "unknown" : value;
  }
}
