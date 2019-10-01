package org.mtgpeasant.perfectdeck.goldfish;

import org.junit.Test;
import org.mtgpeasant.perfectdeck.common.cards.Deck;
import org.mtgpeasant.perfectdeck.common.matchers.MulliganRules;

import java.io.IOException;
import java.io.InputStreamReader;
import java.text.DecimalFormat;

public class GoldfishSimulatorTest {
    private static final DecimalFormat PERCENT = new DecimalFormat("#.##");

    @Test
    public void reanimator_deck_goldfish_otd() throws IOException {
        Deck deck = Deck.parse(new InputStreamReader(getClass().getResourceAsStream("/reanimator-deck2.txt")));
        MulliganRules rules = MulliganRules.parse(new InputStreamReader(getClass().getResourceAsStream("/reanimator-rules.txt")));
        simulate(deck, GoldfishSimulator.builder()
                .iterations(5000)
                .maxTurns(15)
                .otp(GoldfishSimulator.OnThePlay.NO)
                .pilot(new ReanimatorDeckPilot(rules))
                .build());
    }

    @Test
    public void reanimator_deck_goldfish_otp() throws IOException {
        Deck deck = Deck.parse(new InputStreamReader(getClass().getResourceAsStream("/reanimator-deck2.txt")));
        MulliganRules rules = MulliganRules.parse(new InputStreamReader(getClass().getResourceAsStream("/reanimator-rules.txt")));
        simulate(deck, GoldfishSimulator.builder()
                .iterations(5000)
                .maxTurns(15)
                .otp(GoldfishSimulator.OnThePlay.YES)
                .pilot(new ReanimatorDeckPilot(rules))
                .build());
    }

    @Test
    public void karsten_aggro_deck1_goldfish() throws IOException {
        Deck deck = Deck.parse(new InputStreamReader(getClass().getResourceAsStream("/karsten-deck-1.txt")));
        simulate(deck, GoldfishSimulator.builder()
                .iterations(5000)
                .maxTurns(15)
                .otp(GoldfishSimulator.OnThePlay.RANDOM)
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
        stats.getWinCount().forEach((turn, count) -> {
            System.out.println("kill turn " + turn + ": " + count + "/" + stats.getIterations() + " (" + PERCENT.format(100f * (float) count / (float) stats.getIterations()) + "%)");
        });
        System.out.println("Average kill: " + String.format("%.2f", stats.getAverageWinTurn()) + " (std derivation: " + String.format("%.2f", stats.getWinTurnStdDerivation()) + ")");
    }
}
