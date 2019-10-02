package org.mtgpeasant.perfectdeck.common;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class ManaTest {
    @Test
    public void should_contain_1() {
        Mana mana = Mana.of("BU1");
        assertThat(mana.contains(Mana.of("3"))).isTrue();
    }

    @Test
    public void should_contain_2() {
        Mana mana = Mana.of("BU1");
        assertThat(mana.contains(Mana.of("B2"))).isTrue();
    }

    @Test
    public void should_contain_3() {
        Mana mana = Mana.of("BU1");
        assertThat(mana.contains(Mana.of("U"))).isTrue();
    }

    @Test
    public void should_not_contain_1() {
        Mana mana = Mana.of("BU1");
        assertThat(mana.contains(Mana.of("4"))).isFalse();
    }

    @Test
    public void should_not_contain_2() {
        Mana mana = Mana.of("BU1");
        assertThat(mana.contains(Mana.of("R"))).isFalse();
    }
}
