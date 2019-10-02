package org.mtgpeasant.perfectdeck.common.matchers;

import lombok.Value;
import org.mtgpeasant.perfectdeck.common.cards.Cards;

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
    protected Stream<Match> matches(Stream<Match> stream, MatcherContext context) {
        return stream
                .filter(match -> match.getRemaining().contains(card))
                .map(match -> {
                    Cards newRemaining = match.getRemaining().clone();
                    newRemaining.remove(card);
                    Cards newSelected = match.getSelected().clone();
                    newSelected.add(card);
                    return Match.builder()
                            .remaining(newRemaining)
                            .selected(newSelected)
                            .build();
                });
    }
}
