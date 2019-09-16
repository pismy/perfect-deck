package org.mtgpeasant.stats.domain;

import lombok.Builder;
import lombok.Value;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
    public Stream<Match> matches(Stream<Match> stream, MatcherContext context) {
        for (int i = 0; i < times; i++) {
            stream = matcher.matches(stream, context);
        }
        return stream;
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
