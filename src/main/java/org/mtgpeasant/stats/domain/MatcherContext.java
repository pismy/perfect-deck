package org.mtgpeasant.stats.domain;

import lombok.Builder;
import lombok.Singular;
import lombok.Value;

import java.util.Map;

@Builder
@Value
public class MatcherContext {
    final Cards deck;

    @Singular
    final Map<String, Matcher> matchers;

    public Matcher findByName(String name) {
        return matchers.get(name);
    }

}
