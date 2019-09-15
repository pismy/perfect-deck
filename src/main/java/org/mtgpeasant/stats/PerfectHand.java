package org.mtgpeasant.stats;

import org.mtgpeasant.stats.domain.*;
import org.mtgpeasant.stats.parser.DeckParser;
import org.mtgpeasant.stats.parser.MatcherParser;
import org.mtgpeasant.stats.parser.ParseError;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PerfectHand {

    private static final int DRAWS = 1000;
    private static final boolean DEBUG = true;
    private static final DecimalFormat PERCENT = new DecimalFormat("#.##");

    public static void main(String... args) throws IOException, ParseError {
        Cards deck = DeckParser.parse(new FileReader("src/main/resources/reanimator-deck.txt"));

        if (DEBUG) {
            System.out.println("Deck loaded: " + deck.size() + " cards");
        }

        List<MatcherParser.MatcherDeclaration> criterias = new ArrayList<>();
        Map<String, Matcher> matchers = new HashMap<>();
        Map<String, Integer> matchCount = new HashMap<>();
        int noMatchCount = 0;

        BufferedReader reader = new BufferedReader(new FileReader("src/main/resources/reanimator-matchers.txt"));
        String line = null;
        while ((line = reader.readLine()) != null) {
            line = line.trim().toLowerCase();
            if (line.isEmpty() || line.startsWith("#")) {
                // empty or commented line
            } else {
                MatcherParser.MatcherDeclaration decl = MatcherParser.parse(line);
                matchers.put(decl.getName(), decl.getMatcher());
                if (decl.isCriteria()) {
                    criterias.add(decl);
                }
            }
        }
        reader.close();

        MatcherContext ctx = MatcherContext.builder()
                .deck(deck)
                .matchers(matchers)
                .build();

        // validation
        Validation validation = new Validation();
        for(Matcher matcher : matchers.values()) {
            matcher.validate(validation, ctx);
        }
        for(String msg : validation.getWarnings()) {
            System.out.println("WARN: " + msg);
        }
        for(String msg : validation.getErrors()) {
            System.out.println("ERROR: " + msg);
        }

        // start stats
        for (int i = 0; i < DRAWS; i++) {
            Cards hand = deck.copy().shuffle().draw(7);
            Match match = Match.from(hand);
            MatcherParser.MatcherDeclaration matching = null;
            for (MatcherParser.MatcherDeclaration decl : criterias) {
                if (!decl.getMatcher().matches(match, ctx).isEmpty()) {
                    matching = decl;
                    break;
                }
            }
            if (matching != null) {
                // increment match count
                matchCount.put(matching.getName(), matchCount.getOrDefault(matching.getName(), 0) + 1);
            } else {
                noMatchCount++;
            }
            if (DEBUG) {
                if (matching == null) {
                    System.out.println("Hand " + hand + " is rejected");
                } else {
                    System.out.println("Hand " + hand + " matches " + matching);
                }
            }
        }

        // dump stats
        System.out.println("=== STATS ===");
        for (MatcherParser.MatcherDeclaration decl : criterias) {
            int matchesCount = matchCount.getOrDefault(decl.getName(), 0);
            System.out.println(decl.getName() + ": " + matchesCount + "/" + DRAWS + " (" + PERCENT.format(100f * (float) matchesCount / (float) DRAWS) + "%)");
        }
        System.out.println("NONE: " + noMatchCount + "/" + DRAWS + " (" + PERCENT.format(100f * (float) noMatchCount / (float) DRAWS) + "%)");
    }
}
