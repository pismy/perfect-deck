package org.mtgpeasant.perfectdeck.mulligan;

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
    protected Stream<Match> matches(boolean onThePlay, int mulligans, Stream<Match> upStreamMatches, MatcherContext context) {
        return context.findByName(name).matches(onThePlay, mulligans, upStreamMatches, context);
    }
}
