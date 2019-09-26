package org.mtgpeasant.stats;

import dnl.utils.text.table.TextTable;
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

        @ShellMethod("Simulates hundreds of hand draws and computes statistics about matching criterion")
        public void mkstats(
                @ShellOption(value = {"-D", "--deck"}, help = "the deck to test") File deckFile,
                @ShellOption(value = {"-R", "--rules"}, help = "opening hand keeping rules") File matchersFile,
                @ShellOption(value = {"-I", "--iterations"}, help = "number of simulated iterations", defaultValue = "1000") int iterations,
                @ShellOption(value = {"-v", "--verbose"}, help = "produces verbose output", defaultValue = "false") boolean verbose

        ) throws IOException {
            Deck deck = DeckParser.parse(new FileReader(deckFile));

            System.out.println("Deck loaded: " + deck.getMain().size() + " cards (" + deck.getSideboard().size() + " cards in sideboard)");
            System.out.println();

            Rules rules = Rules.parse(new FileReader(matchersFile));
            if (!rules.getErrors().isEmpty()) {
                System.out.println("=== ERRORS ===");
                for (ParseError error : rules.getErrors()) {
                    System.out.println(error.getMessage());
                }
                return;
            }

            // validation
            Validation validation = rules.validate();
//            if (!validation.getWarnings().isEmpty()) {
//                System.out.println("=== WARNING ===");
//                for (String msg : validation.getWarnings()) {
//                    System.out.println("-> " + msg);
//                }
//                System.out.println();
//            }
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
            long startTime = System.currentTimeMillis();
            Simulator simulator = Simulator.builder().iterations(iterations).rules(rules).build();
            Simulator.Results results = simulator.simulate(deck);
            if (verbose) {
                System.out.println();
            }

            // dump stats
            System.out.println("=== STATS (elapsed " + (System.currentTimeMillis() - startTime) + "ms) ===");
            String[] columnNames = new String[]{"Criteria", "Deck #1"};
            Object[][] data = new Object[rules.getCriteria().size()+1][2];
            for(int i = 0; i<rules.getCriteria().size(); i++) {
                MatcherParser.DeclaredMatcher criteria = rules.getCriteria().get(i);
                data[i][0] = criteria.getName();
                int count = results.getMatchCount().get(criteria.getName())[0];
                data[i][1] = count + "/" + iterations + " (" + PERCENT.format(100f * (float) count / (float) iterations) + "%)";
            }
            data[rules.getCriteria().size()][0] = "no match";
            int count = results.getNoMatchCount()[0];
            data[rules.getCriteria().size()][1] = count + "/" + iterations + " (" + PERCENT.format(100f * (float) count / (float) iterations) + "%)";
            new TextTable(columnNames, data).printTable();
//            for (RulesParser.DeclaredMatcher decl : rules.getCriteria()) {
//                int matchesCount = results.getMatchCount().getOrDefault(decl.getName(), 0);
//                System.out.println(decl.getName() + ": " + matchesCount + "/" + iterations + " (" + PERCENT.format(100f * (float) matchesCount / (float) iterations) + "%)");
//            }
//            System.out.println("NONE: " + results.getNoMatchCount() + "/" + iterations + " (" + PERCENT.format(100f * (float) results.getNoMatchCount() / (float) iterations) + "%)");
        }
    }
}

