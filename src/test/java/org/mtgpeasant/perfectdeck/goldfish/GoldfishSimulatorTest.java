package org.mtgpeasant.perfectdeck.goldfish;

import org.junit.Test;
import org.mtgpeasant.perfectdeck.common.cards.Deck;
import org.mtgpeasant.perfectdeck.common.matchers.MulliganRules;

import java.io.IOException;
import java.io.InputStreamReader;
import java.text.DecimalFormat;
import java.util.function.Predicate;

public class GoldfishSimulatorTest {
    private static final DecimalFormat PERCENT = new DecimalFormat("#.##");

    @Test
    public void reanimator_deck_goldfish() throws IOException {
        Deck deck = Deck.parse(new InputStreamReader(getClass().getResourceAsStream("/reanimator-deck2.txt")));
        MulliganRules rules = MulliganRules.parse(new InputStreamReader(getClass().getResourceAsStream("/reanimator-rules.txt")));
        simulate(deck, GoldfishSimulator.builder()
                .iterations(100000)
                .maxTurns(15)
                .start(GoldfishSimulator.Start.RANDOM)
                .pilot(new ReanimatorDeckPilot(rules))
                .build());
    }

    @Test
    public void karsten_aggro_deck1_goldfish() throws IOException {
        Deck deck = Deck.parse(new InputStreamReader(getClass().getResourceAsStream("/karsten-deck-1.txt")));
        simulate(deck, GoldfishSimulator.builder()
                .iterations(100000)
                .maxTurns(15)
                .start(GoldfishSimulator.Start.RANDOM)
                .pilot(new KarstenAggroDeck1Pilot())
                .build());
    }

    public static void simulate(Deck deck, GoldfishSimulator simulator) {
        System.out.println("Deck loaded: " + deck.getMain().size() + " cards (" + deck.getSideboard().size() + " cards in sideboard)");
        System.out.println();

        // simulate
        long startTime = System.currentTimeMillis();
        GoldfishSimulator.DeckStats stats = simulator.simulate(deck);

        // dump perfectdeck
        System.out.println("=== STATS (elapsed " + (System.currentTimeMillis() - startTime) + "ms) ===");

        Predicate<GoldfishSimulator.GameResult> isWin = result -> result.getOutcome() == GoldfishSimulator.GameResult.Outcome.WON;

        stats.getMulligans().forEach(nbMulligans -> {
            Predicate<GoldfishSimulator.GameResult> thisMull = result -> result.getMulligans() == nbMulligans;
            long totalGames = stats.count(thisMull);
            Predicate<GoldfishSimulator.GameResult> winWithMull = result -> result.getMulligans() == nbMulligans && result.getOutcome() == GoldfishSimulator.GameResult.Outcome.WON;
            System.out.println("stats with " + nbMulligans + " mulligans (" + percentage(totalGames, stats.getIterations()) + "):" +
                    "\n   wins   : " + percentage(stats.count(winWithMull), totalGames) +
                    "\n     with avg kill: " + String.format("%.2f", stats.getAverageWinTurn(winWithMull)) + " (std derivation: " + String.format("%.2f", stats.getWinTurnStdDerivation(winWithMull)) + ")" +
                    "\n   timeout: " + percentage(stats.count(result -> result.getMulligans() == nbMulligans && result.getOutcome() == GoldfishSimulator.GameResult.Outcome.TIMEOUT), totalGames) +
                    "\n   lost   : " + percentage(stats.count(result -> result.getMulligans() == nbMulligans && result.getOutcome() == GoldfishSimulator.GameResult.Outcome.LOST), totalGames));
        });

        stats.getWinTurns(result -> result.getOutcome() == GoldfishSimulator.GameResult.Outcome.WON).forEach(turn -> {
            long count = stats.count(result -> result.getOutcome() == GoldfishSimulator.GameResult.Outcome.WON && result.getEndTurn() == turn);
            long countOtp = stats.count(result -> result.getOutcome() == GoldfishSimulator.GameResult.Outcome.WON && result.getEndTurn() == turn && result.isOnThePlay());
            long countOtd = stats.count(result -> result.getOutcome() == GoldfishSimulator.GameResult.Outcome.WON && result.getEndTurn() == turn && !result.isOnThePlay());
            System.out.println("kill turn " + turn + ":" +
                    "\n   global: " + percentage(count, stats.getIterations()) +
                    "\n   OTP   : " + percentage(countOtp, stats.getIterations() / 2) +
                    "\n   OTD   : " + percentage(countOtd, stats.getIterations() / 2)
            );
        });
        Predicate<GoldfishSimulator.GameResult> otpFilter = result -> result.getOutcome() == GoldfishSimulator.GameResult.Outcome.WON && result.isOnThePlay();
        Predicate<GoldfishSimulator.GameResult> otdFilter = result -> result.getOutcome() == GoldfishSimulator.GameResult.Outcome.WON && !result.isOnThePlay();
        System.out.println("Average kill: " +
                "\n   global: " + String.format("%.2f", stats.getAverageWinTurn()) + " (std derivation: " + String.format("%.2f", stats.getWinTurnStdDerivation()) + ")" +
                "\n   OTP   : " + String.format("%.2f", stats.getAverageWinTurn(otpFilter)) + " (std derivation: " + String.format("%.2f", stats.getWinTurnStdDerivation(otpFilter)) + ")" +
                "\n   OTD   : " + String.format("%.2f", stats.getAverageWinTurn(otdFilter)) + " (std derivation: " + String.format("%.2f", stats.getWinTurnStdDerivation(otdFilter)) + ")"
        );
    }

    private static String percentage(long count, long total) {
        return count + "/" + total + " (" + PERCENT.format(100f * (float) count / (float) total) + "%)";
    }
}
