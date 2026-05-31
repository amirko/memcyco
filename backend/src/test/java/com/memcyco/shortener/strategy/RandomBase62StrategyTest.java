package com.memcyco.shortener.strategy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class RandomBase62StrategyTest {
  private final RandomBase62Strategy strategy = new RandomBase62Strategy();

  @Test
  void generatesSevenCharacterBase62Code() {
    String code = strategy.generate("https://example.com", ignored -> false);

    assertThat(code).matches("[0-9A-Za-z]{7}");
  }

  @Test
  void throwsAfterRepeatedCollisions() {
    assertThatThrownBy(() -> strategy.generate("https://example.com", ignored -> true))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("unique short code");
  }
}
