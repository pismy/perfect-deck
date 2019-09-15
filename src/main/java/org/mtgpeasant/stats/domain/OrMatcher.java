package org.mtgpeasant.stats.domain;

import lombok.Builder;
import lombok.Singular;
import lombok.Value;

import java.util.List;
import java.util.stream.Collectors;

@Builder
@Value
public class OrMatcher extends Matcher {
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
    public void validate(Validation validation, MatcherContext context) {
        for(Matcher matcher : matchers) {
            matcher.validate(validation, context);
        }
    }

    @Override
    public List<Match> matches(Match upstream, MatcherContext context) {
        return matchers.stream().map(m -> m.matches(upstream, context)).flatMap(List::stream).collect(Collectors.toList());
    }
}
