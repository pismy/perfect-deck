package org.mtgpeasant.decks;

import org.junit.Test;
import org.mtgpeasant.perfectdeck.common.cards.Cards;
import org.mtgpeasant.perfectdeck.common.cards.Deck;
import org.mtgpeasant.perfectdeck.goldfish.Game;
import org.mtgpeasant.perfectdeck.goldfish.GameMock;
import org.mtgpeasant.perfectdeck.common.Mana;

import java.io.IOException;
import java.io.InputStreamReader;

import static org.assertj.core.api.Assertions.assertThat;

public class ReanimatorDeckPilotTest {
    @Test
    public void can_pay_should_be_true() throws IOException {
        // GIVEN
        ReanimatorDeckPilot pilot = new ReanimatorDeckPilot();

        Deck deck = Deck.parse(new InputStreamReader(getClass().getResourceAsStream("/reanimator-deck2.txt")));

        Game game = GameMock.mock(
                true,
                Cards.of("animate dead", "exhume", "animate dead", "crumbling vestige", "greater sandwurm"),
                deck.getMain().shuffle(),
                Cards.of("faithless looting"),
                Cards.of("mountain", "swamp"),
                Cards.none(),
                pilot
        );

        // WHEN / THEN
        assertThat(pilot.canPay(Mana.of("2"))).isTrue();
    }

    @Test
    public void pay_should_work() throws IOException {
        // GIVEN
        ReanimatorDeckPilot pilot = new ReanimatorDeckPilot();

        Deck deck = Deck.parse(new InputStreamReader(getClass().getResourceAsStream("/reanimator-deck2.txt")));

        Game game = GameMock.mock(
                true,
                Cards.of("animate dead", "exhume", "animate dead", "crumbling vestige", "greater sandwurm"),
                deck.getMain().shuffle(),
                Cards.of("faithless looting"),
                Cards.of("mountain", "swamp"),
                Cards.none(),
                pilot
        );

        // WHEN
        pilot.pay(Mana.of("2"));

        // THEN
        assertThat(game.getBoard()).containsExactlyInAnyOrder("mountain", "swamp");
        assertThat(game.getTapped()).containsExactlyInAnyOrder("mountain", "swamp");
    }
}
