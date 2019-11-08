package org.mtgpeasant.perfectdeck.goldfish;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mtgpeasant.perfectdeck.goldfish.Card.*;

public class CardTest {
    private static final String NETTLE_SENTINEL = "nettle sentinel";
    private static final String SKARRGAN_PIT_SKULK = "skarrgan pit-skulk";

    @Test
    public void name_selector_should_match() {
        // GIVEN
        Card card = new Card(SKARRGAN_PIT_SKULK, Game.CardType.creature);

        // WHEN / THEN
        assertThat(withName(SKARRGAN_PIT_SKULK).test(card)).isTrue();
    }

    @Test
    public void name_selector_should_not_match() {
        // GIVEN
        Card card = new Card(SKARRGAN_PIT_SKULK, Game.CardType.creature);

        // WHEN / THEN
        assertThat(withName(NETTLE_SENTINEL).test(card)).isFalse();
    }

    @Test
    public void type_selector_should_match() {
        // GIVEN
        Card card = new Card(SKARRGAN_PIT_SKULK, Game.CardType.creature);

        // WHEN / THEN
        assertThat(withType(Game.CardType.creature).test(card)).isTrue();
    }

    @Test
    public void tapped_selector_should_match() {
        // GIVEN
        Card card = new Card(SKARRGAN_PIT_SKULK, Game.CardType.creature).setTapped(true);

        // WHEN / THEN
        assertThat(tapped().test(card)).isTrue();
    }

    @Test
    public void tapped_selector_should_not_match() {
        // GIVEN
        Card card = new Card(SKARRGAN_PIT_SKULK, Game.CardType.creature);

        // WHEN / THEN
        assertThat(tapped().test(card)).isFalse();
    }

    @Test
    public void tag_selector_should_match() {
        // GIVEN
        Card card = new Card(SKARRGAN_PIT_SKULK, Game.CardType.creature);

        // WHEN / THEN
        assertThat(withType(Game.CardType.creature)
                .and(untapped())
                .and(notWithTag("sickness"))
                .and(notWithTag("defender"))
                .test(card)).isTrue();
    }

    @Test
    public void tag_selector_should_not_match() {
        // GIVEN
        Card card = new Card(SKARRGAN_PIT_SKULK, Game.CardType.creature).tag("sickness");

        // WHEN / THEN
        assertThat(notWithTag("sickness").test(card)).isFalse();
    }

}
