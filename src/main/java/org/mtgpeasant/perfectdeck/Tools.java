package org.mtgpeasant.perfectdeck;

import org.mtgpeasant.perfectdeck.common.cards.Deck;
import org.mtgpeasant.perfectdeck.common.matchers.Matchers;
import org.mtgpeasant.perfectdeck.common.matchers.MulliganRules;
import org.mtgpeasant.perfectdeck.common.matchers.Validation;
import org.mtgpeasant.perfectdeck.common.utils.ParseError;
import org.mtgpeasant.perfectdeck.goldfish.DeckPilot;
import org.mtgpeasant.perfectdeck.goldfish.GoldfishSimulator;
import org.mtgpeasant.perfectdeck.mulligan.MulliganSimulator;
import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;
import org.springframework.shell.standard.ShellOption;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.function.Predicate;

@ShellComponent
public class Tools {

    private static final DecimalFormat PERCENT = new DecimalFormat("#.##");

    private static String percent(long count, long total) {
        return count + "/" + total + " (" + PERCENT.format(100f * (float) count / (float) total) + "%)";
    }

    private static String f2d(double number) {
        return String.format("%.2f", number);
    }

    @ShellMethod("Simulates hundreds of hand draws and computes statistics about matching criterion")
    public void hands(
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
            @ShellOption(value = {"-s", "--start"}, help = "starting case (one of: OTP, OTD, RANDOM)", defaultValue = "RANDOM") GoldfishSimulator.Start start,
            @ShellOption(value = {"-M", "--maxturns"}, help = "maximum number of turn in a game before giving up the simulation", defaultValue = "15") int maxTurns,
            @ShellOption(value = {"-n", "--nostats"}, help = "disable statistics computation (logs only)", defaultValue = "false") boolean noStats,
            @ShellOption(value = {"-v", "--verbose"}, help = "produces verbose output", defaultValue = "false") boolean verbose

    ) throws IOException, IllegalAccessException, InstantiationException, ClassNotFoundException {
        DeckPilot pilot = (DeckPilot) Class.forName(pilotClassName).newInstance();

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
                .pilot(pilot)
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
            Predicate<GoldfishSimulator.GameResult> isWin = result -> result.getOutcome() == GoldfishSimulator.GameResult.Outcome.WON;

            stats.getMulligans().forEach(nbMulligans -> {
                Predicate<GoldfishSimulator.GameResult> thisMull = result -> result.getMulligans() == nbMulligans;
                long totalGames = stats.count(thisMull);
                Predicate<GoldfishSimulator.GameResult> winWithMull = result -> result.getMulligans() == nbMulligans && result.getOutcome() == GoldfishSimulator.GameResult.Outcome.WON;
                if (moreThanOnePercent(totalGames, stats.getIterations())) {
                    System.out.println("stats with " + nbMulligans + " mulligans (" + percent(totalGames, stats.getIterations()) + "):" +
                            "\n   wins   : " + percent(stats.count(winWithMull), totalGames) +
                            "\n     with avg kill: " + f2d(stats.getAverageWinTurn(winWithMull)) + " (std derivation: " + f2d(stats.getWinTurnStdDerivation(winWithMull)) + ")" +
                            "\n   timeout: " + percent(stats.count(result -> result.getMulligans() == nbMulligans && result.getOutcome() == GoldfishSimulator.GameResult.Outcome.TIMEOUT), totalGames) +
                            "\n   lost   : " + percent(stats.count(result -> result.getMulligans() == nbMulligans && result.getOutcome() == GoldfishSimulator.GameResult.Outcome.LOST), totalGames));
                }
            });

            stats.getWinTurns(result -> result.getOutcome() == GoldfishSimulator.GameResult.Outcome.WON).forEach(turn -> {
                long count = stats.count(result -> result.getOutcome() == GoldfishSimulator.GameResult.Outcome.WON && result.getEndTurn() == turn);
                if (moreThanOnePercent(count, stats.getIterations())) {
                    System.out.println("kill turn " + turn + ":");
                    if (start == GoldfishSimulator.Start.RANDOM) {
                        System.out.println("   global: " + percent(count, stats.getIterations()));
                    }
                    if (start != GoldfishSimulator.Start.OTD) {
                        long countOtp = stats.count(result -> result.getOutcome() == GoldfishSimulator.GameResult.Outcome.WON && result.getEndTurn() == turn && result.isOnThePlay());
                        System.out.println("   OTP   : " + percent(countOtp, stats.getIterations() / 2));
                    }
                    if (start != GoldfishSimulator.Start.OTP) {
                        long countOtd = stats.count(result -> result.getOutcome() == GoldfishSimulator.GameResult.Outcome.WON && result.getEndTurn() == turn && !result.isOnThePlay());
                        System.out.println("   OTD   : " + percent(countOtd, stats.getIterations() / 2));
                    }
                }
            });

            System.out.println("Average kill:");
            if (start == GoldfishSimulator.Start.RANDOM) {
                System.out.println("   global: " + f2d(stats.getAverageWinTurn()) + " (std derivation: " + f2d(stats.getWinTurnStdDerivation()) + ")");
            }
            if (start != GoldfishSimulator.Start.OTD) {
                Predicate<GoldfishSimulator.GameResult> otpFilter = result -> result.getOutcome() == GoldfishSimulator.GameResult.Outcome.WON && result.isOnThePlay();
                System.out.println("   OTP   : " + f2d(stats.getAverageWinTurn(otpFilter)) + " (std derivation: " + f2d(stats.getWinTurnStdDerivation(otpFilter)) + ")");
            }
            if (start != GoldfishSimulator.Start.OTP) {
                Predicate<GoldfishSimulator.GameResult> otdFilter = result -> result.getOutcome() == GoldfishSimulator.GameResult.Outcome.WON && !result.isOnThePlay();
                System.out.println("   OTD   : " + f2d(stats.getAverageWinTurn(otdFilter)) + " (std derivation: " + f2d(stats.getWinTurnStdDerivation(otdFilter)) + ")");
            }
        }
    }

    private boolean moreThanOnePercent(long count, int total) {
        return count * 100 / total > 1;
    }
}
