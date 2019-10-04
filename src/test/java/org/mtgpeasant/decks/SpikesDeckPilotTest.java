package org.mtgpeasant.decks;

import org.junit.Test;
import org.mtgpeasant.perfectdeck.Tools;
import org.mtgpeasant.perfectdeck.goldfish.GoldfishSimulator;

import java.io.File;
import java.io.IOException;

public class SpikesDeckPilotTest {
    @Test
    public void goldfish() throws IOException, InstantiationException, IllegalAccessException, ClassNotFoundException {
        new Tools().goldfish(
                new File("src/main/resources/spikes-deck.txt"),
                SpikesDeckPilot.class.getName(),
                50000,
                GoldfishSimulator.Start.BOTH,
                15,
                false,
                false);
    }

}