package org.mtgpeasant.stats.domain;

import lombok.Builder;
import lombok.Singular;
import lombok.Value;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Builder
@Value
public class AndMatcher extends Matcher {
    @Singular
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
    public List<Match> matches(Match upstream, MatcherContext context) {
        if (matchers.isEmpty()) {
            return Collections.singletonList(upstream);
        }
        List<Match> matches = matchers.get(0).matches(upstream, context);
        for (int i = 1; i < matchers.size() && !matches.isEmpty(); i++) {
            final Matcher matcher = matchers.get(i);
            // select sub matches
            matches = matches.stream()
                    .map(match -> matcher.matches(match, context))
                    .flatMap(List::stream)
                    .collect(Collectors.toList());
        }
        return matches;
    }
}
