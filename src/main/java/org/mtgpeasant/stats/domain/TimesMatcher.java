package org.mtgpeasant.stats.domain;

import lombok.Builder;
import lombok.Value;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Builder
@Value
public class TimesMatcher extends Matcher {
    final int times;

    final Matcher matcher;

    @Override
    public String toString() {
        return times + " " + matcher;
    }

    @Override
    public void validate(Validation validation, MatcherContext context) {
        matcher.validate(validation, context);
    }

    @Override
    public List<Match> matches(Match upstream, MatcherContext context) {
        if (times == 0) {
            return Collections.singletonList(upstream);
        }
        List<Match> matches = matcher.matches(upstream, context);
        for (int i = 1; i < times && !matches.isEmpty(); i++) {
            // select sub matches
            matches = matches.stream()
                    .map(match -> matcher.matches(match, context))
                    .flatMap(List::stream)
                    .collect(Collectors.toList());
        }
        return matches;
    }
}
