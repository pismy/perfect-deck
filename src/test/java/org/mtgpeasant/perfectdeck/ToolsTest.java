package org.mtgpeasant.perfectdeck;

import org.junit.Test;
import org.mtgpeasant.decks.InfectDeckPilot;
import org.mtgpeasant.decks.KarstenAggroDeck1Pilot;
import org.mtgpeasant.decks.ReanimatorDeckPilot;
import org.mtgpeasant.perfectdeck.goldfish.GoldfishSimulator;

import java.io.File;
import java.io.IOException;

public class ToolsTest {
    @Test
    public void reanimator_deck1_mulligans() throws IOException {
        new Tools().hands(
                new File("src/main/resources/reanimator-deck.txt"),
                new File("src/main/resources/reanimator-rules.txt"),
                50000,
                false,
                false);
    }

    @Test
    public void reanimator_deck2_mulligans() throws IOException {
        new Tools().hands(
                new File("src/main/resources/reanimator-deck2.txt"),
                new File("src/main/resources/reanimator-rules.txt"),
                50000,
                false,
                false);
    }

    @Test
    public void infect_deck_goldfish() throws IOException, InstantiationException, IllegalAccessException, ClassNotFoundException {
        new Tools().goldfish(
                new File("src/main/resources/infect-invigorate-deck.txt"),
                InfectDeckPilot.class.getName(),
                100000,
                GoldfishSimulator.Start.RANDOM,
                15,
                false,
                false);
    }

    @Test
    public void reanimator_deck_goldfish() throws IOException, InstantiationException, IllegalAccessException, ClassNotFoundException {
        new Tools().goldfish(
                new File("src/main/resources/reanimator-deck2.txt"),
                ReanimatorDeckPilot.class.getName(),
                100000,
                GoldfishSimulator.Start.RANDOM,
                15,
                false,
                false);
    }

    @Test
    public void karsten_aggro_deck1_goldfish() throws IOException, InstantiationException, IllegalAccessException, ClassNotFoundException {
        new Tools().goldfish(
                new File("src/main/resources/karsten-deck-1.txt"),
                KarstenAggroDeck1Pilot.class.getName(),
                50000,
                GoldfishSimulator.Start.RANDOM,
                15,
                false,
                false);
    }

    @Test
    public void infect_deck_observe() throws IOException, InstantiationException, IllegalAccessException, ClassNotFoundException {
        new Tools().goldfish(
                new File("src/main/resources/infect-invigorate-deck.txt"),
                InfectDeckPilot.class.getName(),
                3,
                GoldfishSimulator.Start.RANDOM,
                15,
                true,
                true);
    }

    @Test
    public void reanimator_deck_observe() throws IOException, InstantiationException, IllegalAccessException, ClassNotFoundException {
        new Tools().goldfish(
                new File("src/main/resources/reanimator-deck2.txt"),
                ReanimatorDeckPilot.class.getName(),
                3,
                GoldfishSimulator.Start.RANDOM,
                15,
                true,
                true);
    }

    @Test
    public void karsten_aggro_deck1_observe() throws IOException, InstantiationException, IllegalAccessException, ClassNotFoundException {
        new Tools().goldfish(
                new File("src/main/resources/karsten-deck-1.txt"),
                KarstenAggroDeck1Pilot.class.getName(),
                3,
                GoldfishSimulator.Start.RANDOM,
                15,
                true,
                true);
    }

}
