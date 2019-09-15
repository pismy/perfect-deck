package org.mtgpeasant.stats.domain;

import lombok.Builder;
import lombok.Value;

import java.util.Collections;
import java.util.List;

@Builder
@Value
public class CardMatcher extends Matcher {
    final String card;

    @Override
    public String toString() {
        return "[" + card + "]";
    }

    public List<Match> matches(Match upstream, MatcherContext context) {
        if (upstream.has(card)) {
            return Collections.singletonList(upstream.select(card));
        } else {
            return Collections.emptyList();
        }
    }
}
