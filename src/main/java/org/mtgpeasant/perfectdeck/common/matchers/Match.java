package org.mtgpeasant.perfectdeck.common.matchers;

import lombok.Builder;
import lombok.Value;
import org.mtgpeasant.perfectdeck.common.cards.Cards;
import org.mtgpeasant.perfectdeck.common.utils.UnorderedCollections;

@Builder
@Value
public class Match {
    final Cards selected;
    final Cards remaining;

    public static Match from(Cards cards) {
        return Match.builder().remaining(cards).selected(Cards.empty()).build();
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (other == null || getClass() != other.getClass()) return false;
        Match match = (Match) other;
        return UnorderedCollections.equals(this.selected, match.selected) && UnorderedCollections.equals(this.remaining, match.remaining);
    }

    @Override
    public int hashCode() {
        return UnorderedCollections.hashCode(selected) + 31 * UnorderedCollections.hashCode(remaining);
    }
}
