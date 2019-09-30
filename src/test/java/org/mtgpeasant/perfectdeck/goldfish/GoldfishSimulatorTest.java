package org.mtgpeasant.perfectdeck.goldfish;

import org.junit.Test;
import org.mtgpeasant.perfectdeck.common.cards.Deck;

import java.io.IOException;
import java.io.InputStreamReader;
import java.text.DecimalFormat;

public class GoldfishSimulatorTest {
    private static final DecimalFormat PERCENT = new DecimalFormat("#.##");

    @Test
    public void karsten_aggro_deck1_goldfish() throws IOException {
        Deck deck = Deck.parse(new InputStreamReader(getClass().getResourceAsStream("/karsten-deck-1.txt")));
        simulate(deck, new KarstenAggroDeck1Pilot(), 50000);
    }

    public static void simulate(Deck deck, DeckPilot pilot, int iterations) throws IOException {
        System.out.println("Deck loaded: " + deck.getMain().size() + " cards (" + deck.getSideboard().size() + " cards in sideboard)");
        System.out.println();

        // simulate
        long startTime = System.currentTimeMillis();
        GoldfishSimulator simulator = GoldfishSimulator.builder().iterations(iterations).maxTurns(15).pilot(pilot).build();
        GoldfishSimulator.DeckStats stats = simulator.simulate(deck);

        // dump perfectdeck
        System.out.println("=== STATS (elapsed " + (System.currentTimeMillis() - startTime) + "ms) ===");
        stats.getWinCount().forEach((turn, count) -> {
            System.out.println("kill turn " + turn + ": " + count + "/" + iterations + " (" + PERCENT.format(100f * (float) count / (float) iterations) + "%)");
        });
        System.out.println("Average kill: " + String.format("%.2f", stats.getAverageWinTurn()) + " (std derivation: " + String.format("%.2f", stats.getWinTurnStdDerivation()) + ")");
    }
}
