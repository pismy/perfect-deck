package org.mtgpeasant.stats.domain;

import org.assertj.core.api.Assertions;
import org.junit.Test;

import java.util.List;

public class OrMatcherTest {
    @Test
    public void matcher_should_match() {
        // Given
        Cards cards = Cards.builder()
                .card("swamp")
                .card("mountain")
                .card("ulamog crusher")
                .card("animate dead")
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
        List<Match> matches = matcher.matches(Match.from(cards), null);

        // Then
        Assertions.assertThat(matches).hasSize(2);
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
        List<Match> matches = matcher.matches(Match.from(cards), null);

        // Then
        Assertions.assertThat(matches).isEmpty();
    }
}