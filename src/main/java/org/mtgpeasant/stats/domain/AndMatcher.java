package org.mtgpeasant.stats.domain;

import lombok.Builder;
import lombok.Singular;
import lombok.Value;

import java.util.List;
import java.util.stream.Stream;

@Builder
@Value
public class AndMatcher implements Matcher {
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
    public void validate(Validation validation, MatcherContext context) {
        for (Matcher matcher : matchers) {
            matcher.validate(validation, context);
        }
    }

    @Override
    public Stream<Match> matches(Stream<Match> stream, MatcherContext context) {
        for (Matcher matcher : matchers) {
            stream = matcher.matches(stream, context);
        }
        return stream;
    }
}
