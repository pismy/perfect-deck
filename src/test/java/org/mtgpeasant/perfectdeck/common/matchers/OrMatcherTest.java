package org.mtgpeasant.perfectdeck.common.matchers;

import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.mtgpeasant.perfectdeck.common.cards.Cards;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.mtgpeasant.perfectdeck.common.cards.Cards.from;
import static org.mtgpeasant.perfectdeck.common.matchers.Matchers.*;

public class OrMatcherTest {
    @Test
    public void matcher_should_match() {
        // Given
        Cards cards = from(
                "swamp",
                "mountain",
                "ulamog crusher",
                "animate dead",
                "exhume",
                "lotus petal",
                "putrid imp"
        );

        Matcher matcher = or(
                card("swamp"),
                and(card("animate dead"), card("exhume")),
                card("pathrazer of ulamog")
        );

        // When
        List<Match> matches = matcher.matches(Stream.of(Match.from(cards)), null).collect(Collectors.toList());

        // Then
        Assertions.assertThat(matches).hasSize(2);
        Assertions.assertThat(matches.get(0).getSelected()).containsExactly("swamp");
        Assertions.assertThat(matches.get(0).getRemaining()).hasSize(6);
        Assertions.assertThat(matches.get(1).getSelected()).containsExactly("animate dead", "exhume");
        Assertions.assertThat(matches.get(1).getRemaining()).hasSize(5);
    }

    @Test
    public void matcher_should_not_match() {
        // Given
        Cards cards = from(
                "mountain",
                "mountain",
                "ulamog crusher",
                "exhume",
                "dark ritual",
                "lotus petal",
                "putrid imp"
        );

        Matcher matcher = or(card("swamp"), card("animate dead"), card("pathrazer of ulamog"));

        // When
        List<Match> matches = matcher.matches(Stream.of(Match.from(cards)), null).collect(Collectors.toList());

        // Then
        Assertions.assertThat(matches).isEmpty();
    }
}
