package org.mtgpeasant.perfectdeck.common.matchers;

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
    protected Stream<Match> matches(Stream<Match> stream, MatcherContext context) {
        for (int i = 0; i < times; i++) {
            stream = matcher.matches(stream, context);
        }
        return stream;
    }
}
