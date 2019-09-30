package org.mtgpeasant.perfectdeck.common.matchers;

import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.mtgpeasant.perfectdeck.common.cards.Cards;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.mtgpeasant.perfectdeck.common.matchers.Matchers.and;
import static org.mtgpeasant.perfectdeck.common.matchers.Matchers.card;

public class AndMatcherTest {
    @Test
    public void matcher_should_match() {
        // Given
        Cards cards = Cards.from(
                "swamp",
                "mountain",
                "ulamog crusher",
                "animate dead",
                "dark ritual",
                "lotus petal",
                "putrid imp"
        );

        Matcher matcher = and(card("swamp"), card("animate dead"), card("ulamog crusher"), card("putrid imp"), card("dark ritual"));

        // When
        List<Match> matches = matcher.matches(Stream.of(Match.from(cards)), null).collect(Collectors.toList());

        // Then
        Assertions.assertThat(matches).hasSize(1);
        Assertions.assertThat(matches.get(0).getSelected().size()).isEqualTo(5);
        Assertions.assertThat(matches.get(0).getRemaining().size()).isEqualTo(2);
    }

    @Test
    public void matcher_should_not_match() {
        // Given
        Cards cards = Cards.from(
                "swamp",
                "mountain",
                "ulamog crusher",
                "animate dead",
                "dark ritual",
                "lotus petal",
                "putrid imp"
        );

        Matcher matcher = and(card("swamp"), card("animate dead"), card("ulamog crusher"), card("putrid imp"), card("swamp"));

        // When
        List<Match> matches = matcher.matches(Stream.of(Match.from(cards)), null).collect(Collectors.toList());

        // Then
        Assertions.assertThat(matches).isEmpty();
    }
}
