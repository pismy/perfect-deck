package org.mtgpeasant.perfectdeck.mulligan;

import lombok.Builder;
import lombok.Getter;
import lombok.Value;
import org.mtgpeasant.perfectdeck.common.cards.Cards;
import org.mtgpeasant.perfectdeck.common.cards.Deck;
import org.mtgpeasant.perfectdeck.common.matchers.Matchers;
import org.mtgpeasant.perfectdeck.common.matchers.MulliganRules;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;


/**
 * Tool that simulates one or several versions of a deck against defined mulligan rules.
 */
@Builder
@Value
public class MulliganSimulator {
    @Builder.Default
    final int draw = 7;
    @Builder.Default
    final int iterations = 50000;

    final MulliganRules rules;

    @Builder
    @Getter
    public static class DeckMatches {
        final Deck deck;
        final int iterations;
        final Map<String, Integer> matchCount = new HashMap<>();
        int noMatchCount = 0;

        private void addMatch(Matchers.NamedMatcher criteria) {
            matchCount.put(criteria.getName(), getMatchCount(criteria) + 1);
        }

        /**
         * Returns the number of matches for the given selection criterion
         *
         * @param criteria hand selection criterion
         * @return number of opening hands that matched this criterion
         */
        public int getMatchCount(Matchers.NamedMatcher criteria) {
            return matchCount.getOrDefault(criteria.getName(), 0);
        }

        private void addNoMatch() {
            noMatchCount++;
        }
    }

    /**
     * Performs a simulation on several versions of the given deck
     *
     * @param decksProvider provides the different version of the deck to optimize
     * @return list of opening hands statistics (one per deck)
     */
    public List<DeckMatches> simulate(Iterable<Deck> decksProvider) {
        return StreamSupport.stream(decksProvider.spliterator(), false)
                .map(deck -> simulate(deck))
                .collect(Collectors.toList());
    }

    /**
     * Performs a simulation on a deck
     *
     * @param deck the deck to optimize
     * @return opening hands statistics
     */
    public DeckMatches simulate(Deck deck) {
        DeckMatches deckMatches = DeckMatches.builder().deck(deck).iterations(iterations).build();
        for (int it = 0; it < iterations; it++) {
            Cards hand = deck.getMain().shuffle().draw(draw);
            Optional<Matchers.NamedMatcher> matching = rules.firstMatch(hand);
            if (matching.isPresent()) {
                // increment match count
                deckMatches.addMatch(matching.get());
            } else {
                deckMatches.addNoMatch();
            }
        }
        return deckMatches;
    }

}
