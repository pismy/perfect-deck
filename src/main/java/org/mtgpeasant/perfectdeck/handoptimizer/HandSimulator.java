package org.mtgpeasant.perfectdeck.handoptimizer;

import lombok.Builder;
import lombok.Value;
import org.mtgpeasant.perfectdeck.common.cards.Cards;
import org.mtgpeasant.perfectdeck.common.cards.Deck;
import org.mtgpeasant.perfectdeck.common.matchers.Matchers;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;


@Builder
@Value
public class HandSimulator {
    int draw = 7;
    final int iterations;
    final MulliganRules rules;

    @Value
    public static class Results {
        final int iterations;
        final Map<String, int[]> matchCount;
        final int[] noMatchCount;

        private Results(int iterations, List<Matchers.NamedMatcher> criteria, Deck... decks) {
            this.iterations = iterations;
            this.matchCount = new HashMap<>();
            for (Matchers.NamedMatcher decl : criteria) {
                matchCount.put(decl.getName(), new int[decks.length]);
            }
            noMatchCount = new int[decks.length];
        }

        private void addMatch(int deck, Matchers.NamedMatcher criteria) {
            matchCount.get(criteria.getName())[deck]++;
        }

        private void addNoMatch(int deck) {
            noMatchCount[deck]++;
        }
    }

    public Results simulate(Deck... decks) {
        Results results = new Results(iterations, rules.getCriteria(), decks);
        for (int d = 0; d < decks.length; d++) {
            Deck deck = decks[d];
            for (int it = 0; it < iterations; it++) {
                Cards hand = deck.getMain().shuffle().draw(draw);
                Optional<Matchers.NamedMatcher> matching = rules.firstMatch(hand);
                if (matching.isPresent()) {
                    // increment match count
                    results.addMatch(d, matching.get());
                } else {
                    results.addNoMatch(d);
                }
            }
        }
        return results;
    }

}
