package com.memcyco.shortener.strategy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.function.Predicate;
import org.junit.jupiter.api.Test;

class StrategyServiceTest {
  @Test
  void returnsSortedReadOnlyStrategiesWithCommonParameterSchema() {
    StrategyService service = new StrategyService(List.of(
        new NamedStrategy("zeta", "Zeta"),
        new NamedStrategy("alpha", "Alpha")
    ));

    List<StrategyResponse> strategies = service.strategies();

    assertThat(strategies).extracting(StrategyResponse::label).containsExactly("Alpha", "Zeta");
    assertThat(strategies).allSatisfy(strategy -> {
      assertThat(strategy.readOnly()).isTrue();
      assertThat(strategy.parameters()).extracting(StrategyParameter::name)
          .containsExactly("expiresAt", "maxClicks", "tags");
    });
  }

  @Test
  void delegatesGenerationToNamedStrategy() {
    StrategyService service = new StrategyService(List.of(new NamedStrategy("fixed", "Fixed")));

    assertThat(service.generate("fixed", "https://example.com", ignored -> false)).isEqualTo("fixed-code");
  }

  @Test
  void rejectsUnknownStrategy() {
    StrategyService service = new StrategyService(List.of(new NamedStrategy("known", "Known")));

    assertThatThrownBy(() -> service.requireStrategy("missing"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Unknown generation strategy");
  }

  private record NamedStrategy(String name, String label) implements ShortCodeStrategy {
    @Override
    public String description() {
      return "Test strategy";
    }

    @Override
    public String generate(String originalUrl, Predicate<String> exists) {
      return "fixed-code";
    }
  }
}
