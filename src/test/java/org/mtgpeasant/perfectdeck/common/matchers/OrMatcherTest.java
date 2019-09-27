package org.mtgpeasant.perfectdeck.common.matchers;

import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.mtgpeasant.perfectdeck.common.cards.Cards;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class OrMatcherTest {
    @Test
    public void matcher_should_match() {
        // Given
        Cards cards = Cards.builder()
                .card("swamp")
                .card("mountain")
                .card("ulamog crusher")
                .card("animate dead")
                .card("exhume")
                .card("lotus petal")
                .card("putrid imp")
                .build();

        Matcher matcher = OrMatcher.builder()
                .matcher(CardMatcher.builder().card("swamp").build())
                .matcher(
                        AndMatcher.builder()
                                .matcher(CardMatcher.builder().card("animate dead").build())
                                .matcher(CardMatcher.builder().card("exhume").build())
                        .build()
                )
                .matcher(CardMatcher.builder().card("pathrazer of ulamog").build())
                .build();

        // When
        List<Match> matches = matcher.matches(Stream.of(Match.from(cards)), null).collect(Collectors.toList());

        // Then
        Assertions.assertThat(matches).hasSize(2);
        Assertions.assertThat(matches.get(0).getSelected().getCards()).containsExactly("swamp");
        Assertions.assertThat(matches.get(0).getRemaining().getCards()).hasSize(6);
        Assertions.assertThat(matches.get(1).getSelected().getCards()).containsExactly("animate dead", "exhume");
        Assertions.assertThat(matches.get(1).getRemaining().getCards()).hasSize(5);
    }

    @Test
    public void matcher_should_not_match() {
        // Given
        Cards cards = Cards.builder()
                .card("mountain")
                .card("mountain")
                .card("ulamog crusher")
                .card("exhume")
                .card("dark ritual")
                .card("lotus petal")
                .card("putrid imp")
                .build();

        Matcher matcher = OrMatcher.builder()
                .matcher(CardMatcher.builder().card("swamp").build())
                .matcher(CardMatcher.builder().card("animate dead").build())
                .matcher(CardMatcher.builder().card("pathrazer of ulamog").build())
                .build();

        // When
        List<Match> matches = matcher.matches(Stream.of(Match.from(cards)), null).collect(Collectors.toList());

        // Then
        Assertions.assertThat(matches).isEmpty();
    }
}
