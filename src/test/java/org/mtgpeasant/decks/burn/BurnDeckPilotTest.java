package org.mtgpeasant.decks.burn;

import org.junit.Test;
import org.mtgpeasant.perfectdeck.Tools;
import org.mtgpeasant.perfectdeck.goldfish.GoldfishSimulator;

import java.io.IOException;

public class BurnDeckPilotTest {

    @Test
    public void urdjur_burn_goldfish() throws IOException, ClassNotFoundException {
        new Tools().goldfish(
                BurnDeckPilot.class.getResourceAsStream("urdjur-burn.txt"),
                BurnDeckPilot.class.getName(),
                50000,
                GoldfishSimulator.Start.BOTH,
                15,
                false,
                false);
    }

    @Test
    public void urdjur_burn_observe() throws IOException, ClassNotFoundException {
        new Tools().goldfish(
                BurnDeckPilot.class.getResourceAsStream("urdjur-burn.txt"),
                BurnDeckPilot.class.getName(),
                3,
                GoldfishSimulator.Start.BOTH,
                15,
                true,
                true);
    }

    @Test
    public void thermo_burn_goldfish() throws IOException, ClassNotFoundException {
        new Tools().goldfish(
                BurnDeckPilot.class.getResourceAsStream("thermo-burn.txt"),
                BurnDeckPilot.class.getName(),
                50000,
                GoldfishSimulator.Start.BOTH,
                15,
                false,
                false);
    }

    @Test
    public void thermo_burn_observe() throws IOException, ClassNotFoundException {
        new Tools().goldfish(
                BurnDeckPilot.class.getResourceAsStream("thermo-burn.txt"),
                BurnDeckPilot.class.getName(),
                3,
                GoldfishSimulator.Start.BOTH,
                15,
                true,
                true);
    }

    @Test
    public void walls_burn_goldfish() throws IOException, ClassNotFoundException {
        new Tools().goldfish(
                BurnDeckPilot.class.getResourceAsStream("walls-burn.txt"),
                BurnDeckPilot.class.getName(),
                50000,
                GoldfishSimulator.Start.BOTH,
                15,
                false,
                false);
    }

    @Test
    public void walls_burn_observe() throws IOException, ClassNotFoundException {
        new Tools().goldfish(
                BurnDeckPilot.class.getResourceAsStream("walls-burn.txt"),
                BurnDeckPilot.class.getName(),
                3,
                GoldfishSimulator.Start.BOTH,
                15,
                true,
                true);
    }

    @Test
    public void ghitu_burn_goldfish() throws IOException, ClassNotFoundException {
        new Tools().goldfish(
                BurnDeckPilot.class.getResourceAsStream("ghitu-burn.txt"),
                BurnDeckPilot.class.getName(),
                50000,
                GoldfishSimulator.Start.BOTH,
                15,
                false,
                false);
    }

    @Test
    public void ghitu_burn_observe() throws IOException, ClassNotFoundException {
        new Tools().goldfish(
                BurnDeckPilot.class.getResourceAsStream("ghitu-burn.txt"),
                BurnDeckPilot.class.getName(),
                3,
                GoldfishSimulator.Start.BOTH,
                15,
                true,
                true);
    }

    @Test
    public void kiln_burn_goldfish() throws IOException, ClassNotFoundException {
        new Tools().goldfish(
                BurnDeckPilot.class.getResourceAsStream("kiln-burn.txt"),
                BurnDeckPilot.class.getName(),
                50000,
                GoldfishSimulator.Start.BOTH,
                15,
                false,
                false);
    }

    @Test
    public void kiln_burn_observe() throws IOException, ClassNotFoundException {
        new Tools().goldfish(
                BurnDeckPilot.class.getResourceAsStream("kiln-burn.txt"),
                BurnDeckPilot.class.getName(),
                3,
                GoldfishSimulator.Start.BOTH,
                15,
                true,
                true);
    }
}
