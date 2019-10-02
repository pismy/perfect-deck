package org.mtgpeasant.perfectdeck.common.matchers;

import lombok.Value;

import java.util.stream.Stream;

@Value
class RefMatcher extends Matcher {
    final String name;

    @Override
    public String toString() {
        return "<" + name + ">";
    }

    @Override
    protected void validate(Validation validation, MatcherContext context) {
        if (context.findByName(name) == null) {
            validation.error("Matcher <" + name + "> not found");
        }
    }

    @Override
    protected Stream<Match> matches(Stream<Match> stream, MatcherContext context) {
        return context.findByName(name).matches(stream, context);
    }
}
