package org.mtgpeasant.perfectdeck;

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
        return String.format("%.2f%%", (100f * (float) count / (float) total));
    }

    private static String f3d(double number) {
        return String.format("%.3f", number);
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

            List<Integer> representativeWinTurns = stats.getWinTurns(result -> result.getOutcome() == GoldfishSimulator.GameResult.Outcome.WON)
                    .stream()
                    .filter(turn -> {
                        long count = stats.count(result -> result.getEndTurn() == turn);
                        return moreThanOnePercent(count, stats.getIterations());
                    })
                    .collect(Collectors.toList());

            TableFormatter.TableFormatterBuilder table = TableFormatter.builder().column("mulligans taken");
            representativeWinTurns.forEach(turn -> table.column("win turn " + turn));

            // add a row OTP | OTD if both
            if (start == GoldfishSimulator.Start.BOTH) {
                List<String> row = new ArrayList<>(representativeWinTurns.size() + 1);
                row.add("");
                representativeWinTurns.forEach(turn -> {
                    row.add("OTP    | OTD   ");
                });
                table.row(row);
            }

            // rows are for mulligans taken
            stats.getMulligans().forEach(mulligansTaken -> {
                long totalGamesWithThisNumberOfMulligans = stats.count(result -> result.getMulligans() == mulligansTaken);
                if (moreThanOnePercent(totalGamesWithThisNumberOfMulligans, stats.getIterations())) {
                    List<String> row = new ArrayList<>(representativeWinTurns.size() + 1);
                    row.add(mulligansTaken + " mulligans (" + percent(totalGamesWithThisNumberOfMulligans, stats.getIterations()) + ")");

                    // one column per win turn
                    representativeWinTurns.forEach(turn -> {
                        String cell = "";
                        if (start != GoldfishSimulator.Start.OTD) {
                            long count = stats.count(result -> result.getMulligans() == mulligansTaken && result.getEndTurn() == turn && result.isOnThePlay());
                            long total = start == GoldfishSimulator.Start.BOTH ? totalGamesWithThisNumberOfMulligans / 2 : totalGamesWithThisNumberOfMulligans;
                            cell += Strings.padEnd(percent(count, total), 6, ' ');
                        }
                        if (start == GoldfishSimulator.Start.BOTH) {
                            cell += " | ";
                        }
                        if (start != GoldfishSimulator.Start.OTP) {
                            long count = stats.count(result -> result.getMulligans() == mulligansTaken && result.getEndTurn() == turn && !result.isOnThePlay());
                            long total = start == GoldfishSimulator.Start.BOTH ? totalGamesWithThisNumberOfMulligans / 2 : totalGamesWithThisNumberOfMulligans;
                            cell += Strings.padEnd(percent(count, total), 6, ' ');
                        }
                        row.add(cell);
                    });

                    table.row(row);
                }
            });

            // last row is global
            List<String> row = new ArrayList<>(representativeWinTurns.size() + 1);
            row.add("global");

            // one column per win turn
            representativeWinTurns.forEach(turn -> {
                String cell = "";
                if (start != GoldfishSimulator.Start.OTD) {
                    long count = stats.count(result -> result.getEndTurn() == turn && result.isOnThePlay());
                    long total = start == GoldfishSimulator.Start.BOTH ? stats.getIterations() / 2 : stats.getIterations();
                    cell += Strings.padEnd(percent(count, total), 6, ' ');
                }
                if (start == GoldfishSimulator.Start.BOTH) {
                    cell += " | ";
                }
                if (start != GoldfishSimulator.Start.OTP) {
                    long count = stats.count(result -> result.getEndTurn() == turn && !result.isOnThePlay());
                    long total = start == GoldfishSimulator.Start.BOTH ? stats.getIterations() / 2 : stats.getIterations();
                    cell += Strings.padEnd(percent(count, total), 6, ' ');
                }
                row.add(cell);
            });
            table.row(row);

            // dump
            System.out.println(table.build().render());

            System.out.println("Average win turn:");
            if (start != GoldfishSimulator.Start.OTD) {
                Predicate<GoldfishSimulator.GameResult> otpFilter = result -> result.getOutcome() == GoldfishSimulator.GameResult.Outcome.WON && result.isOnThePlay();
                System.out.println("   OTP   : " + f3d(stats.getAverageWinTurn(otpFilter)) + " (±" + f3d(stats.getWinTurnStdDerivation(otpFilter)) + ")");
            }
            if (start != GoldfishSimulator.Start.OTP) {
                Predicate<GoldfishSimulator.GameResult> otdFilter = result -> result.getOutcome() == GoldfishSimulator.GameResult.Outcome.WON && !result.isOnThePlay();
                System.out.println("   OTD   : " + f3d(stats.getAverageWinTurn(otdFilter)) + " (±" + f3d(stats.getWinTurnStdDerivation(otdFilter)) + ")");
            }
            if (start == GoldfishSimulator.Start.BOTH) {
                System.out.println("   global: " + f3d(stats.getAverageWinTurn()) + " (±" + f3d(stats.getWinTurnStdDerivation()) + ")");
            }
        }
    }

    private boolean moreThanOnePercent(long count, int total) {
        return count * 100 / total > 1;
    }
}
