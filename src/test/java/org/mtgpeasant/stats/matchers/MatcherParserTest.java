package org.mtgpeasant.stats.matchers;

import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.mtgpeasant.stats.utils.ParseError;

public class MatcherParserTest {

    @Test
    public void should_not_parse_due_to_no_matcher() throws ParseError {
        // WHEN
        Assertions.assertThatExceptionOfType(ParseError.class)
                .isThrownBy(() -> MatcherParser.parse("<R>: "))
                .withMessageStartingWith("you should declare at least one matcher");
    }

    @Test
    public void should_not_parse_due_to_and_or() throws ParseError {
        // WHEN
        Assertions.assertThatExceptionOfType(ParseError.class)
                .isThrownBy(() -> MatcherParser.parse("<R>: [mountain] || [lotus petal] && [crumbling vestige]"))
                .withMessageStartingWith("'|' expected");
    }

    @Test
    public void or_cards_matchers_should_parse() throws ParseError {
        // WHEN
        MatcherParser.MatcherDeclaration declaration = MatcherParser.parse("<R>: [mountain] || [lotus petal] || [crumbling vestige]  ");

        System.out.println(declaration);

        // THEN
        Assertions.assertThat(declaration.isCriteria()).isFalse();
        Assertions.assertThat(declaration.getMatcher()).isInstanceOf(OrMatcher.class);
        Assertions.assertThat(((OrMatcher)declaration.getMatcher()).getMatchers()).hasSize(3);
    }

    @Test
    public void fn_any_should_parse() throws ParseError {
        // WHEN
        MatcherParser.MatcherDeclaration declaration = MatcherParser.parse("<R>: @any([mountain] [lotus petal] [crumbling vestige])");

        System.out.println(declaration);

        // THEN
        Assertions.assertThat(declaration.isCriteria()).isFalse();
        Assertions.assertThat(declaration.getMatcher()).isInstanceOf(OrMatcher.class);
        Assertions.assertThat(((OrMatcher)declaration.getMatcher()).getMatchers()).hasSize(3);
    }

    @Test
    public void and_matchers_should_parse() throws ParseError {
        // WHEN
        MatcherParser.MatcherDeclaration declaration = MatcherParser.parse("<<turn 1>>: <B> &    [putrid imp] && <rea> && [dark ritual] && <steak>");

        System.out.println(declaration);

        // THEN
        Assertions.assertThat(declaration.isCriteria()).isTrue();
        Assertions.assertThat(declaration.getMatcher()).isInstanceOf(AndMatcher.class);
        Assertions.assertThat(((AndMatcher)declaration.getMatcher()).getMatchers()).hasSize(5);
    }

    @Test
    public void fn_all_should_parse() throws ParseError {
        // WHEN
        MatcherParser.MatcherDeclaration declaration = MatcherParser.parse("<<turn 1>>: @all(<B> [putrid imp] <rea> [dark ritual] <steak>)");

        System.out.println(declaration);

        // THEN
        Assertions.assertThat(declaration.isCriteria()).isTrue();
        Assertions.assertThat(declaration.getMatcher()).isInstanceOf(AndMatcher.class);
        Assertions.assertThat(((AndMatcher)declaration.getMatcher()).getMatchers()).hasSize(5);
    }

    @Test
    public void fn_xof_should_parse() throws ParseError {
        // WHEN
        MatcherParser.MatcherDeclaration declaration = MatcherParser.parse("<<turn 1>>: @xof(2)(<B> <rea> <steak>)");

        System.out.println(declaration);

        // THEN
        Assertions.assertThat(declaration.isCriteria()).isTrue();
        Assertions.assertThat(declaration.getMatcher()).isInstanceOf(OrMatcher.class);
        Assertions.assertThat(((OrMatcher)declaration.getMatcher()).getMatchers()).hasSize(3);
    }

    @Test
    public void compound_matchers_should_parse() throws ParseError {
        // WHEN
        MatcherParser.MatcherDeclaration declaration = MatcherParser.parse("<<turn 1 or 3>>: <B> & [putrid imp] && ( <rea> | ( [dark ritual] && [shred memory]) ) && <steak>");

        System.out.println(declaration);

        // THEN
        Assertions.assertThat(declaration.isCriteria()).isTrue();
        Assertions.assertThat(declaration.getMatcher()).isInstanceOf(AndMatcher.class);
        Assertions.assertThat(((AndMatcher)declaration.getMatcher()).getMatchers()).hasSize(4);
    }

}
