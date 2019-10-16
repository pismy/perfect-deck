package org.mtgpeasant.decks.infect;

import org.junit.Test;
import org.mtgpeasant.decks.infect.InfectDeckPilot;
import org.mtgpeasant.perfectdeck.Tools;
import org.mtgpeasant.perfectdeck.goldfish.GoldfishSimulator;

import java.io.File;
import java.io.IOException;

public class InfectDeckPilotTest {
    @Test
    public void infect_invigorate_deck_goldfish() throws IOException, InstantiationException, IllegalAccessException, ClassNotFoundException {
        new Tools().goldfish(
                new File("src/main/resources/infect-invigorate-deck.txt"),
                InfectDeckPilot.class.getName(),
                100000,
                GoldfishSimulator.Start.BOTH,
                15,
                false,
                false);
    }

    @Test
    public void infect_scaleup_deck_goldfish() throws IOException, InstantiationException, IllegalAccessException, ClassNotFoundException {
        new Tools().goldfish(
                new File("src/main/resources/infect-scaleup-deck.txt"),
                InfectDeckPilot.class.getName(),
                100000,
                GoldfishSimulator.Start.BOTH,
                15,
                false,
                false);
    }

    @Test
    public void infect_invigorate_deck_observe() throws IOException, InstantiationException, IllegalAccessException, ClassNotFoundException {
        new Tools().goldfish(
                new File("src/main/resources/infect-invigorate-deck.txt"),
                InfectDeckPilot.class.getName(),
                3,
                GoldfishSimulator.Start.BOTH,
                15,
                true,
                true);
    }

    @Test
    public void infect_scaleup_deck_observe() throws IOException, InstantiationException, IllegalAccessException, ClassNotFoundException {
        new Tools().goldfish(
                new File("src/main/resources/infect-scaleup-deck.txt"),
                InfectDeckPilot.class.getName(),
                3,
                GoldfishSimulator.Start.BOTH,
                15,
                true,
                true);
    }

}
