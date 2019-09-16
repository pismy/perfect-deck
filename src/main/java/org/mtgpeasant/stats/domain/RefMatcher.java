package org.mtgpeasant.stats.domain;

import lombok.Builder;
import lombok.Value;

import java.util.List;
import java.util.stream.Stream;

@Builder
@Value
public class RefMatcher extends Matcher {
    final String name;

    @Override
    public String toString() {
        return "<" + name + ">";
    }

    @Override
    public void validate(Validation validation, MatcherContext context) {
        if (context.findByName(name) == null) {
            validation.error("Matcher <" + name + "> not found");
        }
    }

    @Override
    public Stream<Match> matches(Stream<Match> stream, MatcherContext context) {
        return context.findByName(name).matches(stream, context);
    }

    public List<Match> matches(Match upstream, MatcherContext context) {
        return context.findByName(name).matches(upstream, context);
    }
}
