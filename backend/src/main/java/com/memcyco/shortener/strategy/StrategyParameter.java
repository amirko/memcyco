package com.memcyco.shortener.strategy;

public record StrategyParameter(
    String name,
    String type,
    boolean required,
    String description
) {
}
