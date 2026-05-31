package com.memcyco.shortener.analytics;

import java.util.List;

public record AnalyticsResponse(
    long totalClicks,
    List<MetricPoint> timeSeries,
    List<MetricPoint> referers,
    List<MetricPoint> userAgents
) {
}
