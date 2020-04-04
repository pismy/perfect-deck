package org.mtgpeasant.decks.theoretic;

import org.junit.Test;
import org.mtgpeasant.perfectdeck.Tools;
import org.mtgpeasant.perfectdeck.goldfish.GoldfishSimulator;

import java.io.IOException;

public class SpikesDeckPilotTest {
    @Test
    public void goldfish() throws IOException, ClassNotFoundException {
        new Tools().goldfish(
                KarstenAggroDeck1Pilot.class.getResourceAsStream("spikes-deck.txt"),
                SpikesDeckPilot.class.getName(),
                50000,
                GoldfishSimulator.Start.BOTH,
                15,
                false,
                false);
    }

}
