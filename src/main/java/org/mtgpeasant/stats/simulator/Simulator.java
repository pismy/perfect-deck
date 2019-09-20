package org.mtgpeasant.stats.simulator;

import lombok.Builder;
import lombok.Value;
import org.mtgpeasant.stats.cards.Cards;
import org.mtgpeasant.stats.cards.Deck;
import org.mtgpeasant.stats.matchers.Match;
import org.mtgpeasant.stats.matchers.MatcherParser;

import java.util.HashMap;
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
        final Map<String, Integer> matchCount;
        final int noMatchCount;
        final long elapse;
    }

    public Results simulate(Deck deck) {
        Map<String, Integer> matchCount = new HashMap<>();
        int noMatchCount = 0;

        long startTime = System.currentTimeMillis();
        for (int it = 0; it < iterations; it++) {
            Cards hand = deck.getMain().shuffle().draw(draw);
            MatcherParser.MatcherDeclaration matching = findMatch(hand);
            if (matching != null) {
                // increment match count
                matchCount.put(matching.getName(), matchCount.getOrDefault(matching.getName(), 0) + 1);
            } else {
                noMatchCount++;
            }
//            if (verbose) {
//                if (matching == null) {
//                    System.out.println("Hand " + hand + " is rejected");
//                } else {
//                    System.out.println("Hand " + hand + " matches " + matching);
//                }
//            }
        }
        return new Results(matchCount, noMatchCount, System.currentTimeMillis() - startTime);
    }

    private MatcherParser.MatcherDeclaration findMatch(Cards hand) {
        Match match = Match.from(hand);
        for (MatcherParser.MatcherDeclaration decl : rules.getCriterias()) {
            if (decl.getMatcher().matches(Stream.of(match), rules).findFirst().isPresent()) {
                return decl;
            }
        }
        return null;
    }
}
