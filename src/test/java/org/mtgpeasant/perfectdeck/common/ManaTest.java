package org.mtgpeasant.perfectdeck.common;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mtgpeasant.perfectdeck.common.Mana.of;
import static org.mtgpeasant.perfectdeck.common.Mana.zero;

public class ManaTest {
    @Test
    public void should_contain_1() {
        Mana mana = of("BU1");
        assertThat(mana.contains(of("3"))).isTrue();
    }

    @Test
    public void should_contain_2() {
        Mana mana = of("BU1");
        assertThat(mana.contains(of("B2"))).isTrue();
    }

    @Test
    public void should_contain_3() {
        Mana mana = of("BU1");
        assertThat(mana.contains(of("U"))).isTrue();
    }

    @Test
    public void should_not_contain_1() {
        Mana mana = of("BU1");
        assertThat(mana.contains(of("4"))).isFalse();
    }

    @Test
    public void should_not_contain_2() {
        Mana mana = of("BU1");
        assertThat(mana.contains(of("R"))).isFalse();
    }

    @Test
    public void minus_test_1() {
        Mana mana = of("BU1");
        assertThat(mana.minus(of("3"))).isEqualTo(zero());
    }

    @Test
    public void minus_test_2() {
        Mana mana = of("BU1");
        assertThat(mana.minus(of("B2"))).isEqualTo(zero());
    }

    @Test
    public void minus_test_3() {
        Mana mana = of("BU1");
        assertThat(mana.minus(of("U"))).isEqualTo(of("1B"));
    }

    @Test
    public void minus_test_4() {
        Mana mana = of("BU1");
        assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> mana.minus(of("4")));
    }

    @Test
    public void minus_test_5() {
        Mana mana = of("BU1");
        assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> mana.minus(of("R")));
    }

    @Test
    public void equals_test() {
        assertThat(of("1BU")).isEqualTo(of("B1U"));
    }

    @Test
    public void remove_test1() {
        Mana.Extraction result = of("1BU").extract(of("1BR"));
        assertThat(result.getExtracted()).isEqualTo(of("1B"));
        assertThat(result.getNotExtracted()).isEqualTo(of("R"));
        assertThat(result.getRest()).isEqualTo(of("U"));
    }

    @Test
    public void remove_test2() {
        Mana.Extraction result = of("1BU").extract(of("2BR"));
        assertThat(result.getExtracted()).isEqualTo(of("1BU"));
        assertThat(result.getNotExtracted()).isEqualTo(of("R"));
        assertThat(result.getRest()).isEqualTo(Mana.zero());
    }

    @Test
    public void remove_test3() {
        Mana.Extraction result = of("2BU").extract(of("1BR"));
        assertThat(result.getExtracted()).isEqualTo(of("1B"));
        assertThat(result.getNotExtracted()).isEqualTo(of("R"));
        assertThat(result.getRest()).isEqualTo(of("1U"));
    }

    @Test
    public void remove_test4() {
        Mana.Extraction result = of("RRR").extract(of("2R"));
        assertThat(result.getExtracted()).isEqualTo(of("RRR"));
        assertThat(result.getNotExtracted()).isEqualTo(zero());
        assertThat(result.getRest()).isEqualTo(zero());
    }

    @Test
    public void remove_test5() {
        Mana.Extraction result = of("2R").extract(of("RRR"));
        assertThat(result.getExtracted()).isEqualTo(of("R"));
        assertThat(result.getNotExtracted()).isEqualTo(of("RR"));
        assertThat(result.getRest()).isEqualTo(of("2"));
    }
}
