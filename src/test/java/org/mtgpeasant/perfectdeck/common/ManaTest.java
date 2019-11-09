package org.mtgpeasant.perfectdeck.common;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
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
    public void equals_test() {
        assertThat(of("1BU")).isEqualTo(of("B1U"));
    }

    @Test
    public void remove_test1() {
        Mana.RemoveResult result = of("1BU").remove(of("1BR"));
        assertThat(result.getRemoved()).isEqualTo(of("1B"));
        assertThat(result.getNotRemoved()).isEqualTo(of("R"));
        assertThat(result.getRest()).isEqualTo(of("U"));
    }

    @Test
    public void remove_test2() {
        Mana.RemoveResult result = of("1BU").remove(of("2BR"));
        assertThat(result.getRemoved()).isEqualTo(of("1BU"));
        assertThat(result.getNotRemoved()).isEqualTo(of("R"));
        assertThat(result.getRest()).isEqualTo(Mana.zero());
    }

    @Test
    public void remove_test3() {
        Mana.RemoveResult result = of("2BU").remove(of("1BR"));
        assertThat(result.getRemoved()).isEqualTo(of("1B"));
        assertThat(result.getNotRemoved()).isEqualTo(of("R"));
        assertThat(result.getRest()).isEqualTo(of("1U"));
    }

    @Test
    public void remove_test4() {
        Mana.RemoveResult result = of("RRR").remove(of("2R"));
        assertThat(result.getRemoved()).isEqualTo(of("RRR"));
        assertThat(result.getNotRemoved()).isEqualTo(zero());
        assertThat(result.getRest()).isEqualTo(zero());
    }

    @Test
    public void remove_test5() {
        Mana.RemoveResult result = of("2R").remove(of("RRR"));
        assertThat(result.getRemoved()).isEqualTo(of("R"));
        assertThat(result.getNotRemoved()).isEqualTo(of("RR"));
        assertThat(result.getRest()).isEqualTo(of("2"));
    }
}
