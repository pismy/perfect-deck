package org.mtgpeasant.perfectdeck.common.matchers;

import org.assertj.core.api.Assertions;
import org.junit.Ignore;
import org.junit.Test;
import org.mtgpeasant.perfectdeck.common.cards.Cards;
import org.mtgpeasant.perfectdeck.common.utils.ParseError;

import java.util.List;
import java.util.stream.Collectors;

import static org.mtgpeasant.perfectdeck.common.cards.Cards.of;
import static org.mtgpeasant.perfectdeck.common.matchers.Matchers.*;

public class MatchersTest {

    @Test
    public void not_matcher_should_match() {
        // Given
        Cards cards = Cards.of(
                "swamp",
                "mountain",
                "ulamog crusher",
                "animate dead",
                "exhume",
                "lotus petal",
                "putrid imp"
        );

        Matcher matcher = not(card("reanimate"));

        // When
        List<Match> matches = matcher.matches(cards, null).collect(Collectors.toList());

        // Then
        Assertions.assertThat(matches).hasSize(1);
        Assertions.assertThat(matches.get(0).getSelected()).hasSize(0);
        Assertions.assertThat(matches.get(0).getRemaining()).hasSize(7);
    }

    @Test
    @Ignore // TODO: not managing distinct properly
    public void times_matcher_should_match() {
        // Given
        Cards cards = Cards.of(
                "swamp",
                "mountain",
                "ulamog crusher",
                "animate dead",
                "exhume",
                "lotus petal",
                "putrid imp"
        );

        Matcher matcher = times(2, or(card("swamp"), card("mountain")));

        // When
        List<Match> matches = matcher.matches(cards, null).collect(Collectors.toList());

        // Then
        Assertions.assertThat(matches).hasSize(1);
        Assertions.assertThat(matches.get(0).getSelected()).containsExactlyInAnyOrder("swamp", "mountain");
    }

    @Test
    public void compound_matcher_should_match() {
        // Given
        Cards cards = Cards.of(
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
        List<Match> matches = matcher.matches(cards, null).collect(Collectors.toList());

        // Then
        Assertions.assertThat(matches).hasSize(2);
        Assertions.assertThat(matches.get(0).getSelected()).containsExactly("swamp");
        Assertions.assertThat(matches.get(0).getRemaining()).hasSize(6);
        Assertions.assertThat(matches.get(1).getSelected()).containsExactly("animate dead", "exhume");
        Assertions.assertThat(matches.get(1).getRemaining()).hasSize(5);
    }

    @Test
    public void or_matcher_should_not_match() {
        // Given
        Cards cards = Cards.of(
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
        List<Match> matches = matcher.matches(cards, null).collect(Collectors.toList());

        // Then
        Assertions.assertThat(matches).isEmpty();
    }

    @Test
    public void and_matcher_should_match() {
        // Given
        Cards cards = Cards.of(
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
        List<Match> matches = matcher.matches(cards, null).collect(Collectors.toList());

        // Then
        Assertions.assertThat(matches).hasSize(1);
        Assertions.assertThat(matches.get(0).getSelected().size()).isEqualTo(5);
        Assertions.assertThat(matches.get(0).getRemaining().size()).isEqualTo(2);
    }

    @Test
    public void and_matcher_should_not_match() {
        // Given
        Cards cards = Cards.of(
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
        List<Match> matches = matcher.matches(cards, null).collect(Collectors.toList());

        // Then
        Assertions.assertThat(matches).isEmpty();
    }

    @Test
    public void should_not_parse_due_to_no_matcher() throws ParseError {
        // WHEN
        Assertions.assertThatExceptionOfType(ParseError.class)
                .isThrownBy(() -> Matchers.parse("<R>: "))
                .withMessageStartingWith("you should declare at least one matcher");
    }

    @Test
    public void should_not_parse_due_to_and_or() throws ParseError {
        // WHEN
        Assertions.assertThatExceptionOfType(ParseError.class)
                .isThrownBy(() -> Matchers.parse("<R>: [mountain] || [lotus petal] && [crumbling vestige]"))
                .withMessageStartingWith("'|' expected");
    }

    @Test
    public void or_cards_matchers_should_parse() throws ParseError {
        // WHEN
        Matchers.NamedMatcher declaration = Matchers.parse("<R>: [mountain] || [lotus petal] || [crumbling vestige]  ");

        System.out.println(declaration);

        // THEN
        Assertions.assertThat(declaration.isCriterion()).isFalse();
        Assertions.assertThat(declaration.getMatcher()).isInstanceOf(OrMatcher.class);
        Assertions.assertThat(((OrMatcher) declaration.getMatcher()).getMatchers()).hasSize(3);
    }

    @Test
    public void fn_any_should_parse() throws ParseError {
        // WHEN
        Matchers.NamedMatcher declaration = Matchers.parse("<R>: @any([mountain] [lotus petal] [crumbling vestige])");

        System.out.println(declaration);

        // THEN
        Assertions.assertThat(declaration.isCriterion()).isFalse();
        Assertions.assertThat(declaration.getMatcher()).isInstanceOf(OrMatcher.class);
        Assertions.assertThat(((OrMatcher) declaration.getMatcher()).getMatchers()).hasSize(3);
    }

    @Test
    public void and_matchers_should_parse() throws ParseError {
        // WHEN
        Matchers.NamedMatcher declaration = Matchers.parse("<<turn 1>>: <B> &    [putrid imp] && <rea> && [dark ritual] && <steak>");

        System.out.println(declaration);

        // THEN
        Assertions.assertThat(declaration.isCriterion()).isTrue();
        Assertions.assertThat(declaration.getMatcher()).isInstanceOf(AndMatcher.class);
        Assertions.assertThat(((AndMatcher) declaration.getMatcher()).getMatchers()).hasSize(5);
    }

    @Test
    public void fn_all_should_parse() throws ParseError {
        // WHEN
        Matchers.NamedMatcher declaration = Matchers.parse("<<turn 1>>: @all(<B> [putrid imp] <rea> [dark ritual] <steak>)");

        System.out.println(declaration);

        // THEN
        Assertions.assertThat(declaration.isCriterion()).isTrue();
        Assertions.assertThat(declaration.getMatcher()).isInstanceOf(AndMatcher.class);
        Assertions.assertThat(((AndMatcher) declaration.getMatcher()).getMatchers()).hasSize(5);
    }

    @Test
    public void fn_xof_should_parse() throws ParseError {
        // WHEN
        Matchers.NamedMatcher declaration = Matchers.parse("<<turn 1>>: @xof(2)(<B> <rea> <steak>)");

        System.out.println(declaration);

        // THEN
        Assertions.assertThat(declaration.isCriterion()).isTrue();
        Assertions.assertThat(declaration.getMatcher()).isInstanceOf(OrMatcher.class);
        Assertions.assertThat(((OrMatcher) declaration.getMatcher()).getMatchers()).hasSize(3);
    }

    @Test
    public void compound_matchers_should_parse() throws ParseError {
        // WHEN
        Matchers.NamedMatcher declaration = Matchers.parse("<<turn 1 or 3>>: <B> & [putrid imp] && ( <rea> | ( [dark ritual] && [shred memory]) ) && <steak>");

        System.out.println(declaration);

        // THEN
        Assertions.assertThat(declaration.isCriterion()).isTrue();
        Assertions.assertThat(declaration.getMatcher()).isInstanceOf(AndMatcher.class);
        Assertions.assertThat(((AndMatcher) declaration.getMatcher()).getMatchers()).hasSize(4);
    }

}
