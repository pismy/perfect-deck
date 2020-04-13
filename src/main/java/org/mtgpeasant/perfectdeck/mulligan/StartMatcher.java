package org.mtgpeasant.perfectdeck.mulligan;

import lombok.Value;

import java.util.stream.Stream;

@Value
class StartMatcher extends Matcher {
    boolean onThePlay;

    @Override
    public String toString() {
        return "@start(" + (onThePlay ? "OTP" : "OTD") + ")";
    }

    @Override
    protected void validate(Validation validation, MatcherContext context) {
    }

    @Override
    protected Stream<Match> matches(boolean onThePlay, int mulligans, Stream<Match> upStreamMatches, MatcherContext context) {
        if (this.onThePlay == onThePlay) {
            return upStreamMatches;
        } else {
            return Stream.empty();
        }
    }
}
