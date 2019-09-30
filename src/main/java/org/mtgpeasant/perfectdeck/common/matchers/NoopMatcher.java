package org.mtgpeasant.perfectdeck.common.matchers;

import lombok.Builder;
import lombok.Value;

import java.util.stream.Stream;

@Builder
@Value
class NoopMatcher extends Matcher {
    @Override
    public String toString() {
        return "$noop";
    }

    @Override
    protected void validate(Validation validation, MatcherContext context) {
    }

    @Override
    public Stream<Match> matches(Stream<Match> stream, MatcherContext context) {
        return stream;
    }
}
