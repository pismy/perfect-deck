package org.mtgpeasant.decks.infect;

import org.junit.Test;
import org.mtgpeasant.perfectdeck.Tools;
import org.mtgpeasant.perfectdeck.goldfish.GoldfishSimulator;

import java.io.IOException;

public class InfectDeckPilotTest {
    @Test
    public void infect_invigorate_deck_goldfish() throws IOException, ClassNotFoundException {
        new Tools().goldfish(
                InfectDeckPilot.class.getResourceAsStream("infect-invigorate-deck.txt"),
                InfectDeckPilot.class.getName(),
                100000,
                GoldfishSimulator.Start.BOTH,
                15,
                false,
                false);
    }

    @Test
    public void infect_invigorate_deck_observe() throws IOException, ClassNotFoundException {
        new Tools().goldfish(
                InfectDeckPilot.class.getResourceAsStream("infect-invigorate-deck.txt"),
                InfectDeckPilot.class.getName(),
                3,
                GoldfishSimulator.Start.BOTH,
                15,
                true,
                true);
    }

    @Test
    public void infect_scaleup_deck_goldfish() throws IOException, ClassNotFoundException {
        new Tools().goldfish(
                InfectDeckPilot.class.getResourceAsStream("infect-scaleup-deck.txt"),
                InfectDeckPilot.class.getName(),
                100000,
                GoldfishSimulator.Start.BOTH,
                15,
                false,
                false);
    }

    @Test
    public void infect_scaleup_deck_observe() throws IOException, ClassNotFoundException {
        new Tools().goldfish(
                InfectDeckPilot.class.getResourceAsStream("infect-scaleup-deck.txt"),
                InfectDeckPilot.class.getName(),
                3,
                GoldfishSimulator.Start.BOTH,
                15,
                true,
                true);
    }

}
