package com.memcyco.shortener.strategy;

import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class StrategyService {
  private final Map<String, ShortCodeStrategy> strategies;

  public StrategyService(List<ShortCodeStrategy> strategies) {
    this.strategies = strategies.stream().collect(Collectors.toMap(ShortCodeStrategy::name, strategy -> strategy));
  }

  public List<StrategyResponse> strategies() {
    List<StrategyParameter> commonParameters = List.of(
        new StrategyParameter("expiresAt", "date", false, "Deactivate the link after this UTC timestamp."),
        new StrategyParameter("maxClicks", "number", false, "Deactivate the link after this many successful redirects."),
        new StrategyParameter("tags", "string[]", false, "Labels for filtering and organization.")
    );
    return strategies.values().stream()
        .map(strategy -> new StrategyResponse(
            strategy.name(),
            strategy.label(),
            strategy.description(),
            true,
            commonParameters
        ))
        .sorted((left, right) -> left.label().compareToIgnoreCase(right.label()))
        .toList();
  }

  public String generate(String strategyName, String originalUrl, Predicate<String> exists) {
    return requireStrategy(strategyName).generate(originalUrl, exists);
  }

  public ShortCodeStrategy requireStrategy(String strategyName) {
    ShortCodeStrategy strategy = strategies.get(strategyName);
    if (strategy == null) {
      throw new IllegalArgumentException("Unknown generation strategy: " + strategyName);
    }
    return strategy;
  }
}
