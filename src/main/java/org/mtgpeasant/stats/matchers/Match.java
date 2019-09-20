package org.mtgpeasant.stats.matchers;

import lombok.Builder;
import lombok.Value;
import org.mtgpeasant.stats.cards.Cards;

@Builder
@Value
public class Match {
    final Cards selected;
    final Cards remaining;

    public static Match from(Cards cards) {
        return Match.builder().remaining(cards).selected(Cards.none()).build();
    }

    public boolean has(String card) {
        return remaining.has(card);
    }

    public Match select(String card) {
        return Match.builder()
                .remaining(remaining.remove(card))
                .selected(selected.add(card))
                .build();
    }
}