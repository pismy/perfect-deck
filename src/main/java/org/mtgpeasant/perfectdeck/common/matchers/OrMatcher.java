package org.mtgpeasant.perfectdeck.common.matchers;

import lombok.Singular;
import lombok.Value;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Value
class OrMatcher extends Matcher {
    @Singular
    final List<Matcher> matchers;

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("(");
        for (int i = 0; i < matchers.size(); i++) {
            if (i > 0) {
                sb.append(" || ");
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
    protected Stream<Match> matches(Stream<Match> stream, MatcherContext context) {
//        return stream
//                // TODO: can we optimize not to collect here ? (Stream all way down)
//                .map(match -> matchers.stream().map(matcher -> matcher.matches(match, context)).collect(Collectors.toList()))
//                .flatMap(Collection::stream)
//                .flatMap(Collection::stream);
        List<Match> upstreamMatches = stream.collect(Collectors.toList());
        return matchers.stream()
                .map(matcher -> matcher.matches(upstreamMatches.stream(), context).collect(Collectors.toList()))
                .flatMap(Collection::stream)
                .distinct();
    }
}
