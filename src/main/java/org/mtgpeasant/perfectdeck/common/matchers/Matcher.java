package org.mtgpeasant.perfectdeck.common.matchers;


import java.util.stream.Stream;

public interface Matcher {
    void validate(Validation validation, MatcherContext context);

    Stream<Match> matches(Stream<Match> stream, MatcherContext context);
}
