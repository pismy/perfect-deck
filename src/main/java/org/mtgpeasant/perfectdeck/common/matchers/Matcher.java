package org.mtgpeasant.perfectdeck.common.matchers;


import org.mtgpeasant.perfectdeck.common.cards.Cards;

import java.util.stream.Stream;

public abstract class Matcher {
    public abstract void validate(Validation validation, MatcherContext context);

    protected abstract Stream<Match> matches(Stream<Match> stream, MatcherContext context);

    public Stream<Match> matches(Cards cards, MatcherContext context) {
        return matches(Stream.of(Match.from(cards)), context);
    }
}
