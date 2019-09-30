package org.mtgpeasant.perfectdeck.common.matchers;

import lombok.Builder;
import lombok.Value;
import org.mtgpeasant.perfectdeck.common.cards.Cards;

@Builder
@Value
public class Match {
    final Cards selected;
    final Cards remaining;

    public static Match from(Cards cards) {
        return Match.builder().remaining(cards).selected(Cards.none()).build();
    }

    public boolean has(String card) {
        return remaining.contains(card);
    }

    public Match select(String card) {
        Cards newRemaining = remaining.clone();
        newRemaining.remove(card);
        Cards newSelected = selected.clone();
        newSelected.add(card);
        return Match.builder()
                .remaining(newRemaining)
                .selected(newSelected)
                .build();
    }
}
