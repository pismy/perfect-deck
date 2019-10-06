package org.mtgpeasant.perfectdeck;

import com.google.common.base.Predicates;
import com.google.common.base.Strings;
import org.mtgpeasant.perfectdeck.common.cards.Deck;
import org.mtgpeasant.perfectdeck.common.matchers.Matchers;
import org.mtgpeasant.perfectdeck.common.matchers.MulliganRules;
import org.mtgpeasant.perfectdeck.common.matchers.Validation;
import org.mtgpeasant.perfectdeck.common.utils.ParseError;
import org.mtgpeasant.perfectdeck.common.utils.TableFormatter;
import org.mtgpeasant.perfectdeck.goldfish.DeckPilot;
import org.mtgpeasant.perfectdeck.goldfish.GoldfishSimulator;
import org.mtgpeasant.perfectdeck.mulligan.MulliganSimulator;
import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;
import org.springframework.shell.standard.ShellOption;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@ShellComponent
public class Tools {

    private static String percent(long count, long total) {
        return String.format("%.1f%%", (100f * (float) count / (float) total));
    }

    private static String f2d(double number) {
        return String.format("%.2f", number);
    }

    @ShellMethod("Simulates hundreds of hand draws and computes statistics about mulligans criterion")
    public void mulligans(
            @ShellOption(value = {"-D", "--deck"}, help = "the deck to test") File deckFile,
            @ShellOption(value = {"-R", "--rules"}, help = "opening hand keeping rules") File matchersFile,
            @ShellOption(value = {"-I", "--iterations"}, help = "number of simulated iterations", defaultValue = "1000") int iterations,
            @ShellOption(value = {"-n", "--nostats"}, help = "disable statistics computation (logs only)", defaultValue = "false") boolean noStats,
            @ShellOption(value = {"-v", "--verbose"}, help = "produces verbose output", defaultValue = "false") boolean verbose

    ) throws IOException {
        Deck deck = Deck.parse(new FileReader(deckFile));

        System.out.println("Deck loaded: " + deck.getMain().size() + " cards (" + deck.getSideboard().size() + " cards in sideboard)");
        System.out.println();

        MulliganRules rules = MulliganRules.parse(new FileReader(matchersFile));
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

    @ShellMethod("Simulates hundreds of goldfish games and computes statistics")
    public void goldfish(
            @ShellOption(value = {"-D", "--deck"}, help = "the deck to test") File deckFile,
            @ShellOption(value = {"-P", "--pilot"}, help = "Deck pilot class name") String pilotClassName,
            @ShellOption(value = {"-I", "--iterations"}, help = "number of simulated iterations", defaultValue = "1000") int iterations,
            @ShellOption(value = {"-s", "--start"}, help = "starting case (one of: OTP, OTD, BOTH)", defaultValue = "BOTH") GoldfishSimulator.Start start,
            @ShellOption(value = {"-M", "--maxturns"}, help = "maximum number of turn in a game before giving up the simulation", defaultValue = "15") int maxTurns,
            @ShellOption(value = {"-n", "--nostats"}, help = "disable statistics computation (logs only)", defaultValue = "false") boolean noStats,
            @ShellOption(value = {"-v", "--verbose"}, help = "produces verbose output", defaultValue = "false") boolean verbose

    ) throws IOException, ClassNotFoundException {
        Class<? extends DeckPilot> pilotClass = (Class<? extends DeckPilot>) Class.forName(pilotClassName);

        Deck deck = Deck.parse(new FileReader(deckFile));

        System.out.println("Deck loaded: " + deck.getMain().size() + " cards (" + deck.getSideboard().size() + " cards in sideboard)");
        System.out.println();

        // simulate games
        if (verbose) {
            System.out.println("=== SIMULATE " + iterations + " GAMES ===");
        }
        long startTime = System.currentTimeMillis();
        GoldfishSimulator simulator = GoldfishSimulator.builder()
                .iterations(iterations)
                .pilotClass(pilotClass)
                .start(start)
                .maxTurns(maxTurns)
                .verbose(verbose)
                .build();

        GoldfishSimulator.DeckStats stats = simulator.simulate(deck);
        if (verbose) {
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

            TableFormatter.TableFormatterBuilder table = TableFormatter.builder().column("mulligans taken");
            table.column("avg win turn");
            winTurns.forEach(turn -> table.column("win turn " + turn));

            // add a row OTP | OTD if both
            if (start == GoldfishSimulator.Start.BOTH) {
                List<String> row = new ArrayList<>(winTurns.size() + 1);
                row.add("");
//                row.add("       OTP |        OTD |        avg");
                row.add("       OTP |        OTD");
                winTurns.forEach(turn -> {
                    row.add("  OTP |   OTD");
                });
                table.row(row);
            }
            table.row(TableFormatter.SEPARATOR);

            // one rows per mulligans taken
            stats.getMulligans().forEach(mulligansTaken -> {
                long totalGamesWithThisNumberOfMulligans = stats.count(result -> result.getMulligans() == mulligansTaken);
                if (moreThanOnePercent(totalGamesWithThisNumberOfMulligans, stats.getIterations())) {
                    table.row(computeRow(
                            mulligansTaken + " mulligans (" + percent(totalGamesWithThisNumberOfMulligans, stats.getIterations()) + ")",
                            start,
                            stats,
                            winTurns,
                            result -> result.getMulligans() == mulligansTaken,
                            totalGamesWithThisNumberOfMulligans
                    ));
                }
            });

            // last row is global
            table.row(TableFormatter.SEPARATOR);
            table.row(computeRow("global", start, stats, winTurns, Predicates.alwaysTrue(), stats.getIterations()));

            // dump
            System.out.println(table.build().render());
        }
    }

    private List<String> computeRow(String title, GoldfishSimulator.Start start, GoldfishSimulator.DeckStats stats, List<Integer> winTurns, Predicate<GoldfishSimulator.GameResult> gamesFilter, long totalGames) {
        List<String> row = new ArrayList<>(winTurns.size() + 1);
        row.add(title);

        // first column: avg win turn
        String avg = "";
        if (start != GoldfishSimulator.Start.OTD) {
            Predicate<GoldfishSimulator.GameResult> otpFilter = gamesFilter.and(result -> result.isOnThePlay());
            avg += Strings.padStart(f2d(stats.getAverageWinTurn(otpFilter)) + " ±" + f2d(stats.getWinTurnAvgDistance(otpFilter)), 10, ' ');
        }
        if (start == GoldfishSimulator.Start.BOTH) {
            avg += " | ";
        }
        if (start != GoldfishSimulator.Start.OTP) {
            Predicate<GoldfishSimulator.GameResult> otdFilter = gamesFilter.and(result -> !result.isOnThePlay());
            avg += Strings.padStart(f2d(stats.getAverageWinTurn(otdFilter)) + " ±" + f2d(stats.getWinTurnAvgDistance(otdFilter)), 10, ' ');
        }
//        if (start == GoldfishSimulator.Start.BOTH) {
//            avg += " | ";
//        }
//        if (start == GoldfishSimulator.Start.BOTH) {
//            avg += Strings.padStart(f2d(stats.getAverageWinTurn(gamesFilter)) + " ±" + f2d(stats.getWinTurnAvgDistance(gamesFilter)),10, ' ');
//        }
        row.add(avg);

        // one column per win turn
        winTurns.forEach(turn -> {
            String cell = "";
            if (start != GoldfishSimulator.Start.OTD) {
                long count = stats.count(gamesFilter.and(result -> result.getEndTurn() == turn && result.isOnThePlay()));
                long total = start == GoldfishSimulator.Start.BOTH ? totalGames / 2 : totalGames;
                cell += Strings.padStart(percent(count, total), 5, ' ');
            }
            if (start == GoldfishSimulator.Start.BOTH) {
                cell += " | ";
            }
            if (start != GoldfishSimulator.Start.OTP) {
                long count = stats.count(gamesFilter.and(result -> result.getEndTurn() == turn && !result.isOnThePlay()));
                long total = start == GoldfishSimulator.Start.BOTH ? totalGames / 2 : totalGames;
                cell += Strings.padStart(percent(count, total), 5, ' ');
            }
            row.add(cell);
        });
        return row;
    }

    private boolean moreThanOnePercent(long count, int total) {
        return count * 100 / total > 1;
    }
}
