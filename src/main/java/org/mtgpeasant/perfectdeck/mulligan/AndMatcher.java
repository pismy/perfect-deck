package org.mtgpeasant.perfectdeck.mulligan;

import lombok.Value;

import java.util.List;
import java.util.stream.Stream;

@Value
class AndMatcher extends Matcher {
    final List<Matcher> matchers;

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("(");
        for (int i = 0; i < matchers.size(); i++) {
            if (i > 0) {
                sb.append(" && ");
            }
            sb.append(matchers.get(i));
        }
        sb.append(")");
        return sb.toString();
    }

    @Override
    protected void validate(Validation validation, MatcherContext context) {
        for (Matcher matcher : matchers) {
            matcher.validate(validation, context);
        }
    }

    @Override
    protected Stream<Match> matches(boolean onThePlay, int mulligans, Stream<Match> upStreamMatches, MatcherContext context) {
        for (Matcher matcher : matchers) {
            upStreamMatches = matcher.matches(onThePlay, mulligans, upStreamMatches, context);
        }
        return upStreamMatches;
    }
}
