package org.mtgpeasant.decks;

import org.junit.Test;
import org.mtgpeasant.perfectdeck.Tools;
import org.mtgpeasant.perfectdeck.common.Mana;
import org.mtgpeasant.perfectdeck.common.cards.Cards;
import org.mtgpeasant.perfectdeck.common.cards.Deck;
import org.mtgpeasant.perfectdeck.goldfish.Game;
import org.mtgpeasant.perfectdeck.goldfish.GameMock;
import org.mtgpeasant.perfectdeck.goldfish.GoldfishSimulator;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;

import static org.assertj.core.api.Assertions.assertThat;

public class ReanimatorDeckPilotTest {
    @Test
    public void can_pay_should_be_true() throws IOException {
        // GIVEN

        Deck deck = Deck.parse(new InputStreamReader(getClass().getResourceAsStream("/reanimator-deck2.txt")));

        Game game = GameMock.mock(
                true,
                Cards.of("animate dead", "exhume", "animate dead", "crumbling vestige", "greater sandwurm"),
                deck.getMain().shuffle(),
                Cards.of("faithless looting"),
                Cards.of("mountain", "swamp"),
                Cards.none()
        );
        ReanimatorDeckPilot pilot = new ReanimatorDeckPilot(game);

        // WHEN / THEN
        assertThat(pilot.canPay(Mana.of("2"))).isTrue();
    }

    @Test
    public void pay_should_work() throws IOException {
        // GIVEN

        Deck deck = Deck.parse(new InputStreamReader(getClass().getResourceAsStream("/reanimator-deck2.txt")));

        Game game = GameMock.mock(
                true,
                Cards.of("animate dead", "exhume", "animate dead", "crumbling vestige", "greater sandwurm"),
                deck.getMain().shuffle(),
                Cards.of("faithless looting"),
                Cards.of("mountain", "swamp"),
                Cards.none()
        );
        ReanimatorDeckPilot pilot = new ReanimatorDeckPilot(game);

        // WHEN
        pilot.pay(Mana.of("2"));

        // THEN
        assertThat(game.getBoard()).containsExactlyInAnyOrder("mountain", "swamp");
        assertThat(game.getTapped()).containsExactlyInAnyOrder("mountain", "swamp");
    }

    @Test
    public void reanimator_deck1_mulligans() throws IOException {
        new Tools().mulligans(
                new File("src/main/resources/reanimator-deck.txt"),
                new File("src/main/resources/reanimator-rules.txt"),
                50000,
                false,
                false);
    }

    @Test
    public void reanimator_deck2_mulligans() throws IOException {
        new Tools().mulligans(
                new File("src/main/resources/reanimator-deck2.txt"),
                new File("src/main/resources/reanimator-rules.txt"),
                50000,
                false,
                false);
    }

//    @Test
//    public void reanimator_deck1_goldfish() throws IOException, InstantiationException, IllegalAccessException, ClassNotFoundException {
//        new Tools().goldfish(
//                new File("src/main/resources/reanimator-deck.txt"),
//                ReanimatorDeckPilot.class.getName(),
//                100000,
//                GoldfishSimulator.Start.BOTH,
//                15,
//                false,
//                false);
//    }

    @Test
    public void reanimator_deck2_goldfish() throws IOException, ClassNotFoundException {
        new Tools().goldfish(
                new File("src/main/resources/reanimator-deck2.txt"),
                ReanimatorDeckPilot.class.getName(),
                100000,
                GoldfishSimulator.Start.BOTH,
                15,
                false,
                false);
    }

    @Test
    public void reanimator_deck_observe() throws IOException, ClassNotFoundException {
        new Tools().goldfish(
                new File("src/main/resources/reanimator-deck2.txt"),
                ReanimatorDeckPilot.class.getName(),
                3,
                GoldfishSimulator.Start.BOTH,
                15,
                true,
                true);
    }
}
