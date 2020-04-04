package org.mtgpeasant.decks.infect;

import org.junit.Test;
import org.mtgpeasant.perfectdeck.Tools;
import org.mtgpeasant.perfectdeck.goldfish.GoldfishSimulator;

import java.io.IOException;

public class GruulInfectTest {
    @Test
    public void gruul_infect_deck_goldfish() throws IOException, ClassNotFoundException {
        new Tools().goldfish(
                GruulInfectPilot.class.getResourceAsStream("infect-gruul-deck.txt"),
                GruulInfectPilot.class.getName(),
                100000,
                GoldfishSimulator.Start.BOTH,
                15,
                false,
                false);
    }

    @Test
    public void gruul_infect_deck_observe() throws IOException, ClassNotFoundException {
        new Tools().goldfish(
                GruulInfectPilot.class.getResourceAsStream("infect-gruul-deck.txt"),
                GruulInfectPilot.class.getName(),
                3,
                GoldfishSimulator.Start.BOTH,
                15,
                true,
                true);
    }
}
