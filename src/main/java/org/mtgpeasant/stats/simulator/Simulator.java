package org.mtgpeasant.stats.simulator;

import lombok.Builder;
import lombok.Value;
import org.mtgpeasant.stats.cards.Cards;
import org.mtgpeasant.stats.cards.Deck;
import org.mtgpeasant.stats.matchers.Match;
import org.mtgpeasant.stats.matchers.MatcherParser;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;


@Builder
@Value
public class Simulator {
    int draw = 7;
    final int iterations;
    final Rules rules;

    @Value
    public static class Results {
        final int iterations;
        final Map<String, int[]> matchCount;
        final int[] noMatchCount;

        private Results(int iterations, List<MatcherParser.DeclaredMatcher> criteria, Deck... decks) {
            this.iterations = iterations;
            this.matchCount = new HashMap<>();
            for (MatcherParser.DeclaredMatcher decl : criteria) {
                matchCount.put(decl.getName(), new int[decks.length]);
            }
            noMatchCount = new int[decks.length];
        }

        private void addMatch(int deck, MatcherParser.DeclaredMatcher criteria) {
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
                MatcherParser.DeclaredMatcher matching = findMatch(hand);
                if (matching != null) {
                    // increment match count
                    results.addMatch(d, matching);
                } else {
                    results.addNoMatch(d);
                }
            }
        }
        return results;
    }

    private MatcherParser.DeclaredMatcher findMatch(Cards hand) {
        Match match = Match.from(hand);
        for (MatcherParser.DeclaredMatcher decl : rules.getCriteria()) {
            if (decl.getMatcher().matches(Stream.of(match), rules).findFirst().isPresent()) {
                return decl;
            }
        }
        return null;
    }
}
