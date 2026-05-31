package com.memcyco.shortener.strategy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class SequentialStrategyTest {
  private final SequentialStrategy strategy = new SequentialStrategy();

  @Test
  void generatesDistinctBase62Codes() {
    String first = strategy.generate("https://example.com/one", ignored -> false);
    String second = strategy.generate("https://example.com/two", ignored -> false);

    assertThat(first).matches("[0-9A-Za-z]+");
    assertThat(second).matches("[0-9A-Za-z]+");
    assertThat(second).isNotEqualTo(first);
  }

  @Test
  void throwsAfterRepeatedSequentialCollisions() {
    assertThatThrownBy(() -> strategy.generate("https://example.com", ignored -> true))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("sequential");
  }
}
