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

//    @Test
//    public void reanimator_deck_goldfish_otd() throws IOException {
//        Deck deck = Deck.parse(new InputStreamReader(getClass().getResourceAsStream("/reanimator-deck2.txt")));
//        MulliganRules rules = MulliganRules.parse(new InputStreamReader(getClass().getResourceAsStream("/reanimator-rules.txt")));
//        simulate(deck, GoldfishSimulator.builder()
//                .iterations(5000)
//                .maxTurns(15)
//                .start(GoldfishSimulator.Start.OTD)
//                .pilot(new ReanimatorDeckPilot(rules))
//                .build());
//    }
//
//    @Test
//    public void reanimator_deck_goldfish_otp() throws IOException {
//        Deck deck = Deck.parse(new InputStreamReader(getClass().getResourceAsStream("/reanimator-deck2.txt")));
//        MulliganRules rules = MulliganRules.parse(new InputStreamReader(getClass().getResourceAsStream("/reanimator-rules.txt")));
//        simulate(deck, GoldfishSimulator.builder()
//                .iterations(5000)
//                .maxTurns(15)
//                .start(GoldfishSimulator.Start.OTP)
//                .pilot(new ReanimatorDeckPilot(rules))
//                .build());
//    }

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
        stats.getWinTurns(result -> result.getOutcome() == GoldfishSimulator.GameResult.Outcome.WON).forEach((turn) -> {
            long count = stats.count(result -> result.getOutcome() == GoldfishSimulator.GameResult.Outcome.WON && result.getEndTurn() == turn);
            long countOtp = stats.count(result -> result.getOutcome() == GoldfishSimulator.GameResult.Outcome.WON && result.getEndTurn() == turn && result.isOnThePlay());
            long countOtd = stats.count(result -> result.getOutcome() == GoldfishSimulator.GameResult.Outcome.WON && result.getEndTurn() == turn && !result.isOnThePlay());
            System.out.println("kill turn " + turn + ":" +
                    "\n   global: " + count + "/" + stats.getIterations() + " (" + PERCENT.format(100f * (float) count / (float) stats.getIterations()) + "%)" +
                    "\n   OTP   : " + countOtp + "/" + stats.getIterations() / 2 + " (" + PERCENT.format(100f * (float) countOtp / (float) stats.getIterations() * 2f) + "%)" +
                    "\n   OTD   : " + countOtd + "/" + stats.getIterations() / 2 + " (" + PERCENT.format(100f * (float) countOtd / (float) stats.getIterations() * 2f) + "%)"
            );
        });
        Predicate<GoldfishSimulator.GameResult> otpFilter = result -> result.getOutcome() == GoldfishSimulator.GameResult.Outcome.WON && result.isOnThePlay();
        Predicate<GoldfishSimulator.GameResult> otdFilter = result -> result.getOutcome() == GoldfishSimulator.GameResult.Outcome.WON && !result.isOnThePlay();
        System.out.println("Average kill: " +
                "\n   global: "+ String.format("%.2f", stats.getAverageWinTurn()) + " (std derivation: " + String.format("%.2f", stats.getWinTurnStdDerivation()) + ")" +
                "\n   OTP   : "+ String.format("%.2f", stats.getAverageWinTurn(otpFilter)) + " (std derivation: " + String.format("%.2f", stats.getWinTurnStdDerivation(otpFilter)) + ")" +
                "\n   OTD   : "+ String.format("%.2f", stats.getAverageWinTurn(otdFilter)) + " (std derivation: " + String.format("%.2f", stats.getWinTurnStdDerivation(otdFilter)) + ")"
        );
    }
}
