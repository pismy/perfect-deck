package org.mtgpeasant.stats;

import org.mtgpeasant.stats.domain.*;
import org.mtgpeasant.stats.parser.DeckParser;
import org.mtgpeasant.stats.parser.MatcherParser;
import org.mtgpeasant.stats.parser.ParseError;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;
import org.springframework.shell.standard.ShellOption;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

@SpringBootApplication
public class PerfectHand {

    public static void main(String[] args) {
        SpringApplication.run(PerfectHand.class, args);
    }

    @ShellComponent
    public static class Statistics {

        private static final DecimalFormat PERCENT = new DecimalFormat("#.##");

        @ShellMethod("Simulates hundreds of hand draws and computes statistics about matching criteria")
        public void mkstats(
                @ShellOption(value = {"-D", "--deck"}, help = "the deck to test") File deckFile,
                @ShellOption(value = {"-R", "--rules"}, help = "opening hand keeping rules") File matchersFile,
                @ShellOption(value = {"-I", "--iterations"}, help = "number of simulated iterations", defaultValue = "1000") int iterations,
                @ShellOption(value = {"-v", "--verbose"}, help = "produces verbose output", defaultValue = "false") boolean verbose

        ) throws IOException, ParseError {
            Deck deck = DeckParser.parse(new FileReader(deckFile));

            System.out.println("Deck loaded: " + deck.getMain().size() + " cards (" + deck.getSideboard().size() + " cards in sideboard)");
            System.out.println();

            List<MatcherParser.MatcherDeclaration> criterias = new ArrayList<>();
            Map<String, Matcher> matchers = new HashMap<>();
            Map<String, Integer> matchCount = new HashMap<>();
            int noMatchCount = 0;

            BufferedReader reader = new BufferedReader(new FileReader(matchersFile));
            String line = null;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
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
                    .deck(deck.getMain())
                    .matchers(matchers)
                    .build();

            // validation
            Validation validation = new Validation();
            for (Matcher matcher : matchers.values()) {
                matcher.validate(validation, ctx);
            }
            if(!validation.getErrors().isEmpty()) {
                System.out.println("=== WARNING ===");
                for (String msg : validation.getWarnings()) {
                    System.out.println("-> " + msg);
                }
                System.out.println();
            }
            if(!validation.getErrors().isEmpty()) {
                System.out.println("=== ERRORS ===");
                for (String msg : validation.getErrors()) {
                    System.out.println("-> " + msg);
                }
                return;
            }

            // simulate draws
            if(verbose) {
                System.out.println("=== SIMULATE "+iterations+" DRAWS ===");
            }
            long startTime = System.currentTimeMillis();
            for (int i = 0; i < iterations; i++) {
                Cards hand = deck.getMain().copy().shuffle().draw(7);
                Match match = Match.from(hand);
                MatcherParser.MatcherDeclaration matching = null;
                for (MatcherParser.MatcherDeclaration decl : criterias) {
                    //                if (!decl.getMatcher().matches(match, ctx).isEmpty()) {
                    //                    matching = decl;
                    //                    break;
                    //                }
                    if (decl.getMatcher().matches(Stream.of(match), ctx).findFirst().isPresent()) {
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
                if (verbose) {
                    if (matching == null) {
                        System.out.println("Hand " + hand + " is rejected");
                    } else {
                        System.out.println("Hand " + hand + " matches " + matching);
                    }
                }
            }
            if(verbose) {
                System.out.println();
            }

            // dump stats
            long endTime = System.currentTimeMillis();
            System.out.println("=== STATS (elapsed " + (endTime - startTime) + "ms) ===");
            for (MatcherParser.MatcherDeclaration decl : criterias) {
                int matchesCount = matchCount.getOrDefault(decl.getName(), 0);
                System.out.println(decl.getName() + ": " + matchesCount + "/" + iterations + " (" + PERCENT.format(100f * (float) matchesCount / (float) iterations) + "%)");
            }
            System.out.println("NONE: " + noMatchCount + "/" + iterations + " (" + PERCENT.format(100f * (float) noMatchCount / (float) iterations) + "%)");
        }
    }
}

