package org.mtgpeasant.perfectdeck.goldfish;

import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.mtgpeasant.perfectdeck.common.Mana;
import org.mtgpeasant.perfectdeck.common.cards.Cards;

import java.util.ArrayList;
import java.util.Arrays;

public class GameTest {
    @Test
    public void shouldnt_cast_if_card_not_in_hand() {
        // GIVEN
        Cards library = Cards.of(new ArrayList<>(Arrays.asList("putrid imp", "swamp", "swamp", "mountain", "animate dead", "dragon breath", "faithless looting", "animate dead", "ulamog's crusher", "exhume", "gitaxian probe", "mountain", "hand of emrakul", "mountain")));
        Cards hand = library.draw(7);
        Game ctx = new Game(true, null);
        ctx.keepHandAndStart(library, hand);

        // WHEN / THEN
        Assertions.assertThatExceptionOfType(IllegalActionException.class)
                .isThrownBy(() -> ctx.castPermanent("ulamog's crusher", Mana.of("8")))
                .withMessageStartingWith("Can't pay");
    }

    @Test
    public void shouldnt_cast_if_required_mana_not_in_pool() {
        // GIVEN
        Cards library = Cards.of(new ArrayList<>(Arrays.asList("putrid imp", "swamp", "swamp", "mountain", "animate dead", "dragon breath", "faithless looting", "animate dead", "ulamog's crusher", "exhume", "gitaxian probe", "mountain", "hand of emrakul", "mountain")));
        Cards hand = library.draw(7);
        Game ctx = new Game(true, null);
        ctx.keepHandAndStart(library, hand);

        // WHEN / THEN
        Assertions.assertThatExceptionOfType(IllegalActionException.class)
                .isThrownBy(() -> ctx.castPermanent("putrid imp", Mana.of("B")))
                .withMessageStartingWith("Can't pay");
    }

    @Test
    public void shouldnt_be_able_to_land_twice() {
        // GIVEN
        Cards library = Cards.of(new ArrayList<>(Arrays.asList("putrid imp", "swamp", "swamp", "mountain", "animate dead", "dragon breath", "faithless looting", "animate dead", "ulamog's crusher", "exhume", "gitaxian probe", "mountain", "hand of emrakul", "mountain")));
        Cards hand = library.draw(7);
        Game ctx = new Game(true, null);
        ctx.keepHandAndStart(library, hand);

        // WHEN / THEN
        ctx.land("swamp");
        Assertions.assertThatExceptionOfType(IllegalActionException.class)
                .isThrownBy(() -> ctx.land("mountain"))
                .withMessageEndingWith(": can't land twice the same turn");
    }

    @Test
    public void should_work() {
        // GIVEN
        Cards library = Cards.of(new ArrayList<>(Arrays.asList("putrid imp", "swamp", "lotus petal", "simian spirit guide", "animate dead", "dragon breath", "ulamog's crusher", "faithless looting", "animate dead", "exhume", "gitaxian probe", "mountain", "hand of emrakul", "mountain")));
        Cards hand = library.draw(7);
        Game game = new Game(true, null);
        game.keepHandAndStart(library, hand);

        // WHEN / THEN
        Permanent swamp = game.land("swamp");
        game.tap(swamp);
        game.add(Mana.of("B"));
        game.castPermanent("putrid imp", Mana.of("B"));
        game.discard("ulamog's crusher");
        Permanent petal = game.castPermanent("lotus petal", Mana.zero());
        game.sacrifice(petal);
        game.add(Mana.of("B"));
        game.discard("simian spirit guide");
        game.add(Mana.of("R"));
        game.castNonPermanent("animate dead", Mana.of("1B"));

        System.out.println("remains: " + game.getPool());
    }

}
