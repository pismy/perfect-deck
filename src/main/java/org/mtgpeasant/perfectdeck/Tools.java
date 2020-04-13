package org.mtgpeasant.perfectdeck;

import org.mtgpeasant.perfectdeck.common.cards.Deck;
import org.mtgpeasant.perfectdeck.mulligan.Matchers;
import org.mtgpeasant.perfectdeck.mulligan.MulliganRules;
import org.mtgpeasant.perfectdeck.mulligan.Validation;
import org.mtgpeasant.perfectdeck.common.utils.ParseError;
import org.mtgpeasant.perfectdeck.common.utils.TableFormatter;
import org.mtgpeasant.perfectdeck.goldfish.DeckPilot;
import org.mtgpeasant.perfectdeck.goldfish.GoldfishSimulator;
import org.mtgpeasant.perfectdeck.mulligan.MulliganSimulator;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static org.mtgpeasant.perfectdeck.goldfish.GoldfishSimulator.DeckStats.*;

//@ShellComponent
public class Tools {

    //    @ShellMethod("Simulates hundreds of hand draws and computes statistics about mulligans criterion")
    public void mulligans(
//            @ShellOption(getPercentage = {"-D", "--deck"}, help = "the deck to test")
            InputStream deckFile,
//            @ShellOption(getPercentage = {"-R", "--rules"}, help = "opening hand keeping rules")
            InputStream mulligansRules,
//            @ShellOption(getPercentage = {"-I", "--iterations"}, help = "number of simulated iterations", defaultValue = "1000")
            int iterations,
//            @ShellOption(getPercentage = {"-n", "--nostats"}, help = "disable statistics computation (logs only)", defaultValue = "false")
            boolean noStats,
//            @ShellOption(getPercentage = {"-v", "--verbose"}, help = "produces verbose output", defaultValue = "false")
            boolean verbose

    ) throws IOException {
        Deck deck = Deck.parse(new InputStreamReader(deckFile));

        System.out.println("Deck loaded: " + deck.getMain().size() + " cards (" + deck.getSideboard().size() + " cards in sideboard)");
        System.out.println();

        MulliganRules rules = MulliganRules.parse(new InputStreamReader(mulligansRules));
        if (!rules.getErrors().isEmpty()) {
            System.out.println("=== ERRORS ===");
            for (ParseError error : rules.getErrors()) {
                System.out.println(error.getMessage());
            }
            return;
        }

        // validation
        Validation validation = rules.validate();
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
        MulliganSimulator simulator = MulliganSimulator.builder()
                .iterations(iterations)
                .rules(rules)
                .verbose(verbose)
                .build();
        MulliganSimulator.DeckMatches matches = simulator.simulate(deck);
        if (verbose) {
            System.out.println();
        }

        if (!noStats) {
            System.out.println("=== STATS (elapsed " + (System.currentTimeMillis() - startTime) + "ms) ===");
            for (Matchers.NamedMatcher criteria : rules.getCriteria()) {
                int count = matches.getMatchCount(criteria);
                System.out.println(criteria.getName() + ": " + percent(count, iterations));
            }
            System.out.println("no match: " + percent(matches.getNoMatchCount(), iterations));
        }
    }

    //    @ShellMethod("Simulates hundreds of goldfish games and computes statistics")
    public void goldfish(
//            @ShellOption(getPercentage = {"-D", "--deck"}, help = "the deck to test")
            InputStream deckFile,
//            @ShellOption(getPercentage = {"-P", "--pilot"}, help = "Deck pilot class name")
            String pilotClassName,
//            @ShellOption(getPercentage = {"-I", "--iterations"}, help = "number of simulated iterations", defaultValue = "1000")
            int iterations,
//            @ShellOption(getPercentage = {"-s", "--start"}, help = "starting case (one of: OTP, OTD, BOTH)", defaultValue = "BOTH")
            GoldfishSimulator.Start start,
//            @ShellOption(getPercentage = {"-M", "--maxturns"}, help = "maximum number of turn in a game before giving up the simulation", defaultValue = "15")
            int maxTurns,
//            @ShellOption(getPercentage = {"-n", "--nostats"}, help = "disable statistics computation (logs only)", defaultValue = "false")
            boolean noStats,
//            @ShellOption(getPercentage = {"-v", "--verbose"}, help = "produces verbose output", defaultValue = "false")
            boolean verbose

    ) throws IOException, ClassNotFoundException {
        Class<? extends DeckPilot> pilotClass = (Class<? extends DeckPilot>) Class.forName(pilotClassName);

        Deck deck = Deck.parse(new InputStreamReader(deckFile));

        System.out.println("Deck loaded: " + deck.getMain().size() + " cards (" + deck.getSideboard().size() + " cards in sideboard)");
        System.out.println();

        // simulate games
        if (verbose) {
            System.out.println("=== SIMULATE " + iterations + " GAMES ===");
        }
        long startTime = System.currentTimeMillis();
        StringWriter buffer = verbose ? new StringWriter() : null;
        PrintWriter output = verbose ? new PrintWriter(buffer) : null;
        GoldfishSimulator simulator = GoldfishSimulator.builder()
                .iterations(iterations)
                .pilotClass(pilotClass)
                .start(start)
                .maxTurns(maxTurns)
                .out(output)
                .build();

        GoldfishSimulator.DeckStats stats = simulator.simulate(deck);
        if (verbose) {
            System.out.println(buffer.toString());
            System.out.println();
        }

        // dump stats
        if (!noStats) {
            System.out.println("=== STATS (elapsed " + (System.currentTimeMillis() - startTime) + "ms) ===");

            List<Integer> winTurns = stats.getWinTurns(result -> result.getOutcome() == GoldfishSimulator.GameResult.Outcome.WON)
                    .stream()
                    .filter(turn -> {
                        long count = stats.count(result -> result.getEndTurn() == turn);
                        return moreThanOnePercent(count, stats.getIterations());
                    })
                    .collect(Collectors.toList());

            if (start != GoldfishSimulator.Start.OTD) {
                System.out.println("ON THE PLAY");
                dumpStats(stats, GoldfishSimulator.Start.OTP, winTurns);
            }
            if (start != GoldfishSimulator.Start.OTP) {
                System.out.println("ON THE DRAW");
                dumpStats(stats, GoldfishSimulator.Start.OTD, winTurns);
            }
        }
    }

    private void dumpStats(GoldfishSimulator.DeckStats stats, GoldfishSimulator.Start start, List<Integer> winTurns) {
        TableFormatter.TableFormatterBuilder table = TableFormatter.builder().column("mulligans");
        table.column("avg. turn");
        winTurns.forEach(turn -> table.column("T" + turn));
        table.row(TableFormatter.SEPARATOR);

        // one rows per mulligans taken
        stats.getMulligans().forEach(mulligans -> {
            if(stats.getPercentage(withStart(start), withMulligans(mulligans, false)).getPercentage() > 1d) {
                table.row(computeRow(
                        stats, winTurns, start,
                        mulligans
                ));
            }
        });

        // last row is global
        table.row(TableFormatter.SEPARATOR);
        table.row(computeRow(stats, winTurns, start, -1));

        // dump
        System.out.println(table.build().render());
    }

    private List<String> computeRow(GoldfishSimulator.DeckStats stats, List<Integer> winTurns, GoldfishSimulator.Start start, int mulligans) {
        List<String> row = new ArrayList<>(winTurns.size() + 1);

        // row title (number of mulligans and percentage)
        row.add(mulligans < 0 ? "global" : mulligans + " (" + stats.getPercentage(withStart(start), withMulligans(mulligans, false)) + ")");

        // first column: avg win turn
        row.add(stats.getAverageWinTurn(withStart(start).and(withMulligans(mulligans, false))).toString());

        // one column per win turn
        winTurns.forEach(turn -> {
            row.add(stats.getPercentage(withStart(start), withMulligans(mulligans, false).and(withEndTurn(turn, false))).toString());
        });
        return row;
    }

    private static String percent(long count, long total) {
        return String.format("%.1f%%", (100f * (float) count / (float) total));
//        return String.format("%.1f%% (%d/%d)", (100f * (float) count / (float) total), count, total);
    }

    private static boolean moreThanOnePercent(long count, int total) {
        return count * 100 / total > 1;
    }
}
