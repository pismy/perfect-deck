package org.mtgpeasant.perfectdeck.goldfish;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mtgpeasant.perfectdeck.goldfish.Permanent.*;

public class PermanentTest {
    private static final String NETTLE_SENTINEL = "nettle sentinel";
    private static final String SKARRGAN_PIT_SKULK = "skarrgan pit-skulk";

    @Test
    public void name_selector_should_match() {
        // GIVEN
        Permanent permanent = new Permanent(SKARRGAN_PIT_SKULK, Game.CardType.creature);

        // WHEN / THEN
        assertThat(withName(SKARRGAN_PIT_SKULK).test(permanent)).isTrue();
    }

    @Test
    public void name_selector_should_not_match() {
        // GIVEN
        Permanent permanent = new Permanent(SKARRGAN_PIT_SKULK, Game.CardType.creature);

        // WHEN / THEN
        assertThat(withName(NETTLE_SENTINEL).test(permanent)).isFalse();
    }

    @Test
    public void type_selector_should_match() {
        // GIVEN
        Permanent permanent = new Permanent(SKARRGAN_PIT_SKULK, Game.CardType.creature);

        // WHEN / THEN
        assertThat(withType(Game.CardType.creature).test(permanent)).isTrue();
    }

    @Test
    public void tapped_selector_should_match() {
        // GIVEN
        Permanent permanent = new Permanent(SKARRGAN_PIT_SKULK, Game.CardType.creature).setTapped(true);

        // WHEN / THEN
        assertThat(tapped().test(permanent)).isTrue();
    }

    @Test
    public void tapped_selector_should_not_match() {
        // GIVEN
        Permanent permanent = new Permanent(SKARRGAN_PIT_SKULK, Game.CardType.creature);

        // WHEN / THEN
        assertThat(tapped().test(permanent)).isFalse();
    }

    @Test
    public void tag_selector_should_match() {
        // GIVEN
        Permanent permanent = new Permanent(SKARRGAN_PIT_SKULK, Game.CardType.creature);

        // WHEN / THEN
        assertThat(withType(Game.CardType.creature)
                .and(untapped())
                .and(notWithTag("sickness"))
                .and(notWithTag("defender"))
                .test(permanent)).isTrue();
    }

    @Test
    public void tag_selector_should_not_match() {
        // GIVEN
        Permanent permanent = new Permanent(SKARRGAN_PIT_SKULK, Game.CardType.creature).tag("sickness");

        // WHEN / THEN
        assertThat(notWithTag("sickness").test(permanent)).isFalse();
    }

    @Test
    public void cleanup_should_work() {
        // GIVEN
        Permanent permanent = new Permanent(SKARRGAN_PIT_SKULK, Game.CardType.creature).setSickness(true).tag("*temp").tag("perm").addCounter("+1/+1", 3).addCounter("*+1/+1", 2);

        // WHEN
        permanent.cleanup();

        // THEN
        assertThat(permanent.getTags()).containsExactly("perm");
        assertThat(permanent.getCounters().keySet()).containsExactly("+1/+1");
    }

}
