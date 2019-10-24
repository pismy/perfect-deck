package org.mtgpeasant.decks.stompy;

import org.junit.Test;
import org.mtgpeasant.perfectdeck.Tools;
import org.mtgpeasant.perfectdeck.goldfish.GoldfishSimulator;

import java.io.File;
import java.io.IOException;

public class StompyDeckPilotTest {
    @Test
    public void stompy_deck_goldfish() throws IOException, ClassNotFoundException {
        new Tools().goldfish(
                new File("src/main/resources/stompy-deck.txt"),
                StompyDeckPilot.class.getName(),
                100000,
                GoldfishSimulator.Start.BOTH,
                15,
                false,
                false);
    }

    @Test
    public void stompy_deck_observe() throws IOException, ClassNotFoundException {
        new Tools().goldfish(
                new File("src/main/resources/stompy-deck.txt"),
                StompyDeckPilot.class.getName(),
                3,
                GoldfishSimulator.Start.BOTH,
                15,
                true,
                true);
    }

}
