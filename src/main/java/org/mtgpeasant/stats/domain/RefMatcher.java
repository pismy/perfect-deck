package org.mtgpeasant.stats.domain;

import lombok.Builder;
import lombok.Value;

import java.util.List;

@Builder
@Value
public class RefMatcher extends Matcher {
    final String name;

    @Override
    public String toString() {
        return "<" + name + ">";
    }

    public List<Match> matches(Match upstream, MatcherContext context) {
        return context.findByName(name).matches(upstream, context);
    }
}
