package org.mtgpeasant.perfectdeck.common.matchers;

import lombok.Value;

import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

@Value
class CardMatcher extends Matcher {
    final String card;

    @Override
    public String toString() {
        return "[" + card + "]";
    }

    @Override
    protected void validate(Validation validation, MatcherContext context) {
//        if(!context.getDeck().has(card)) {
//            validation.warning("Card [" + card + "] not found in deck");
//        }
    }

    @Override
    public Stream<Match> matches(Stream<Match> stream, MatcherContext context) {
        return stream.filter(match -> match.has(card)).map(match -> match.select(card));
    }

    public List<Match> matches(Match upstream, MatcherContext context) {
        if (upstream.has(card)) {
            return Collections.singletonList(upstream.select(card));
        } else {
            return Collections.emptyList();
        }
    }
}
