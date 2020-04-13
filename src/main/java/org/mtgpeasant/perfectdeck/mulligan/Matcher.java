package org.mtgpeasant.perfectdeck.mulligan;


import org.mtgpeasant.perfectdeck.common.cards.Cards;

import java.util.stream.Stream;

public abstract class Matcher {
    /**
     * Validates himself
     *
     * @param validation receives validation messages
     * @param context    global context
     */
    protected abstract void validate(Validation validation, MatcherContext context);

    /**
     * Finds all possible downstream matches from the upstream matches
     *
     * @param onThePlay           game start
     * @param mulligans       number of mulligans so far
     * @param upStreamMatches upstream matches
     * @param context         global context
     * @return all possible downstream matches
     */
    protected abstract Stream<Match> matches(boolean onThePlay, int mulligans, Stream<Match> upStreamMatches, MatcherContext context);

    public Stream<Match> matches(boolean onThePlay, int mulligans, Cards cards, MatcherContext context) {
        return matches(onThePlay, mulligans, Stream.of(Match.from(cards)), context);
    }
}
