package org.mtgpeasant.perfectdeck.common.matchers;

import lombok.Builder;
import lombok.Value;

import java.util.stream.Stream;

@Builder
@Value
class TimesMatcher extends Matcher {
    final int times;

    final Matcher matcher;

    @Override
    public String toString() {
        return times + " " + matcher;
    }

    @Override
    public void validate(Validation validation, MatcherContext context) {
        matcher.validate(validation, context);
    }

    @Override
    public Stream<Match> matches(Stream<Match> stream, MatcherContext context) {
        for (int i = 0; i < times; i++) {
            stream = matcher.matches(stream, context);
        }
        return stream;
    }
}
