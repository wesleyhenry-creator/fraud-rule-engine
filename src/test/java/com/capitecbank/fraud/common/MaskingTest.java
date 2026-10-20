package com.capitecbank.fraud.common;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class MaskingTest {

    @Test
    void keepsOnlyLastFourCharacters() {
        assertThat(Masking.mask("ZA1234567890")).isEqualTo("********7890");
    }

    @Test
    void fullyMasksShortValues() {
        assertThat(Masking.mask("AB12")).isEqualTo("****");
    }

    @Test
    void passesNullThrough() {
        assertThat(Masking.mask(null)).isNull();
    }
}
