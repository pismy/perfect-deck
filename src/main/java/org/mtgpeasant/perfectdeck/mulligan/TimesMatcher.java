package org.mtgpeasant.perfectdeck.mulligan;

import lombok.Value;

import java.util.stream.Stream;

@Value
class TimesMatcher extends Matcher {
    final int times;

    final Matcher matcher;

    @Override
    public String toString() {
        return times + " " + matcher;
    }

    @Override
    protected void validate(Validation validation, MatcherContext context) {
        matcher.validate(validation, context);
    }

    @Override
    protected Stream<Match> matches(boolean onThePlay, int mulligans, Stream<Match> upStreamMatches, MatcherContext context) {
        for (int i = 0; i < times; i++) {
            upStreamMatches = matcher.matches(onThePlay, mulligans, upStreamMatches, context);
        }
        return upStreamMatches;
    }
}
