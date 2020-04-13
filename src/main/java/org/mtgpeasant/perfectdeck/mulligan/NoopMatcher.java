package org.mtgpeasant.perfectdeck.mulligan;

import lombok.Value;

import java.util.stream.Stream;

@Value
class NoopMatcher extends Matcher {
    @Override
    public String toString() {
        return "@noop";
    }

    @Override
    protected void validate(Validation validation, MatcherContext context) {
    }

    @Override
    protected Stream<Match> matches(boolean onThePlay, int mulligans, Stream<Match> upStreamMatches, MatcherContext context) {
        return upStreamMatches;
    }
}
