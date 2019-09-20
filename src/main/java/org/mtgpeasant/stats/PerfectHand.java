package org.mtgpeasant.stats;

import org.mtgpeasant.stats.cards.Deck;
import org.mtgpeasant.stats.cards.DeckParser;
import org.mtgpeasant.stats.matchers.MatcherParser;
import org.mtgpeasant.stats.matchers.Validation;
import org.mtgpeasant.stats.simulator.Rules;
import org.mtgpeasant.stats.simulator.Simulator;
import org.mtgpeasant.stats.utils.ParseError;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;
import org.springframework.shell.standard.ShellOption;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.text.DecimalFormat;

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

            Rules rules = Rules.parse(new FileReader(matchersFile));

            // validation
            Validation validation = rules.validate();
            if (!validation.getErrors().isEmpty()) {
                System.out.println("=== WARNING ===");
                for (String msg : validation.getWarnings()) {
                    System.out.println("-> " + msg);
                }
                System.out.println();
            }
            if (!validation.getErrors().isEmpty()) {
                System.out.println("=== ERRORS ===");
                for (String msg : validation.getErrors()) {
                    System.out.println("-> " + msg);
                }
                return;
            }

            // simulate draws
            if (verbose) {
                System.out.println("=== SIMULATE " + iterations + " DRAWS ===");
            }
            Simulator simulator = Simulator.builder().iterations(iterations).rules(rules).build();
            Simulator.Results results = simulator.simulate(deck);
            if (verbose) {
                System.out.println();
            }

            // dump stats
            System.out.println("=== STATS (elapsed " + (results.getElapse()) + "ms) ===");
            for (MatcherParser.MatcherDeclaration decl : rules.getCriterias()) {
                int matchesCount = results.getMatchCount().getOrDefault(decl.getName(), 0);
                System.out.println(decl.getName() + ": " + matchesCount + "/" + iterations + " (" + PERCENT.format(100f * (float) matchesCount / (float) iterations) + "%)");
            }
            System.out.println("NONE: " + results.getNoMatchCount() + "/" + iterations + " (" + PERCENT.format(100f * (float) results.getNoMatchCount() / (float) iterations) + "%)");
        }
    }
}

