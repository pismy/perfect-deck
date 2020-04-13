package org.mtgpeasant.perfectdeck.mulligan;

import lombok.Value;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Value
class NotMatcher extends Matcher {
    final Matcher matcher;

    @Override
    public String toString() {
        return "not " + matcher;
    }

    @Override
    protected void validate(Validation validation, MatcherContext context) {
        matcher.validate(validation, context);
    }

    @Override
    protected Stream<Match> matches(boolean onThePlay, int mulligans, Stream<Match> upStreamMatches, MatcherContext context) {
        List<Match> upstreamMatches = upStreamMatches.collect(Collectors.toList());
        return upstreamMatches.stream().filter(match -> !matcher.matches(onThePlay, mulligans, upstreamMatches.stream(), context).findFirst().isPresent());
    }
}
