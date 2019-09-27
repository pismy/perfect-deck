package org.mtgpeasant.perfectdeck.common.matchers;

import lombok.Builder;
import lombok.Value;

import java.util.stream.Stream;

@Builder
@Value
class RefMatcher implements Matcher {
    final String name;

    @Override
    public String toString() {
        return "<" + name + ">";
    }

    @Override
    public void validate(Validation validation, MatcherContext context) {
        if (context.findByName(name) == null) {
            validation.error("Matcher <" + name + "> not found");
        }
    }

    @Override
    public Stream<Match> matches(Stream<Match> stream, MatcherContext context) {
        return context.findByName(name).matches(stream, context);
    }
}
