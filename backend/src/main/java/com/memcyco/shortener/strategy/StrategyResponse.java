package com.memcyco.shortener.strategy;

import java.util.List;

public record StrategyResponse(
    String name,
    String label,
    String description,
    boolean readOnly,
    List<StrategyParameter> parameters
) {
}
