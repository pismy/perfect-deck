package org.mtgpeasant.decks.burn;

import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.mtgpeasant.perfectdeck.Tools;
import org.mtgpeasant.perfectdeck.goldfish.Game;
import org.mtgpeasant.perfectdeck.goldfish.GoldfishSimulator;

import java.io.File;
import java.io.IOException;

public class BurnDeckPilotTest {
    @Test
    public void ghitu_should_have_haste() {
        BurnGame game= new BurnGame(true, null);
        game.getGraveyard().add(game.LIGHTNING_BOLT);
        game.getGraveyard().add(game.CHAIN_LIGHTNING);
        game.getGraveyard().add(game.SEAL_OF_FIRE);
        Assertions.assertThat(game.countInGraveyard(Game.CardType.sorcery, Game.CardType.instant)).isEqualTo(2);
    }

    @Test
    public void thermo_burn_goldfish() throws IOException, ClassNotFoundException {
        new Tools().goldfish(
                new File("src/main/resources/thermo-burn.txt"),
                BurnDeckPilot.class.getName(),
                20000,
                GoldfishSimulator.Start.BOTH,
                15,
                false,
                false);
    }

    @Test
    public void thermo_burn_observe() throws IOException, ClassNotFoundException {
        new Tools().goldfish(
                new File("src/main/resources/thermo-burn.txt"),
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
                new File("src/main/resources/walls-burn.txt"),
                BurnDeckPilot.class.getName(),
                20000,
                GoldfishSimulator.Start.BOTH,
                15,
                false,
                false);
    }

    @Test
    public void walls_burn_observe() throws IOException, ClassNotFoundException {
        new Tools().goldfish(
                new File("src/main/resources/walls-burn.txt"),
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
                new File("src/main/resources/ghitu-burn.txt"),
                BurnDeckPilot.class.getName(),
                20000,
                GoldfishSimulator.Start.BOTH,
                15,
                false,
                false);
    }

    @Test
    public void ghitu_burn_observe() throws IOException, ClassNotFoundException {
        new Tools().goldfish(
                new File("src/main/resources/ghitu-burn.txt"),
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
                new File("src/main/resources/kiln-burn.txt"),
                BurnDeckPilot.class.getName(),
                20000,
                GoldfishSimulator.Start.BOTH,
                15,
                false,
                false);
    }

    @Test
    public void kiln_burn_observe() throws IOException, ClassNotFoundException {
        new Tools().goldfish(
                new File("src/main/resources/kiln-burn.txt"),
                BurnDeckPilot.class.getName(),
                3,
                GoldfishSimulator.Start.BOTH,
                15,
                true,
                true);
    }
}
