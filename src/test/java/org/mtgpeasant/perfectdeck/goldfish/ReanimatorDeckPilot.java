package org.mtgpeasant.perfectdeck.goldfish;

import org.mtgpeasant.perfectdeck.common.cards.Cards;
import org.mtgpeasant.perfectdeck.common.matchers.MulliganRules;

import java.awt.geom.Area;

public class ReanimatorDeckPilot extends DeckPilot {
    private static String[] LANDS = new String[]{"swamp", "mountain", "crumbling vestige"};
    private static String[] FREEMANA = new String[]{"lotus petal", "simian spirit guide"};
    private static String[] REANIMATORS_1B = new String[]{"exhume", "animate dead"};
    private static String[] MONSTERS = new String[]{"hand of emrakul", "greater sandwurm", "pathrazer of ulamog", "ulamog's crusher"};
    private static String[] DISCARD = new String[]{"faithless looting", "putrid imp"};

    private final MulliganRules rules;

    public ReanimatorDeckPilot(MulliganRules rules) {
        this.rules = rules;
    }

    @Override
    public boolean keepHand(boolean onThePlay, int mulligans, Cards hand) {
        if (mulligans >= 2) {
            return true;
        }
        return rules.firstMatch(hand) != null;
    }

    @Override
    public void firstMainPhase() {
        while (play()) {

        }
    }

    private boolean play() {
        Cards monstersInGy = game.getGraveyard().select(MONSTERS);
        if (!monstersInGy.isEmpty()) {
            // I have a monster in the graveyard: I must now reanimate
            Cards reanimators = game.getHand().select(REANIMATORS_1B);
            if (game.getHand().has("reanimate") && canPay(game, Mana.of("B"))) {
                pay(game, Mana.of("B"));
                game.castNonPermanent("reanimate", Mana.of("B"));
                game.move(Area.GRAVEYARD, Area.BOARD, monstersInGy.getCards().get(0));
                // done
                return false;
            } else if (!reanimators.isEmpty() && canPay(game, Mana.of("1B"))) {
                pay(game, Mana.of("1B"));
                game.castNonPermanent(reanimators.getCards().get(0), Mana.of("1B"));
                game.move(Area.GRAVEYARD, Area.BOARD, monstersInGy.getCards().get(0));
                // done
                return false;
            } else if (game.getHand().has("gitaxian probe")) {
                // play it: we'll maybe find what we miss
                game.castNonPermanent("gitaxian probe", Mana.zero()).draw(1);
                return true;
            } else {
                // no castable reanimator: what is missing ? mana or reanimator spell ?
                // TODO
            }
        } else {
            // my first objective is now to put a monster in the GY
            Cards monstersInHand = game.getHand().select(MONSTERS);
            Cards lands = game.getHand().select(LANDS);
            Cards freemana = game.getHand().select(FREEMANA);

            if (monstersInHand.isEmpty()) {
                // no monster in hand: can I look for one ?
                if (game.getHand().has("gitaxian probe")) {
                    game.castNonPermanent("gitaxian probe", Mana.zero()).draw(1);
                    return true;
                } else if (game.getHand().has("faithless looting") && canPay(game, Mana.of("R"))) {
                    pay(game, Mana.of("R"));
                    game
                            .castPermanent("faithless looting", Mana.of("R"))
                            .draw(2);
                    // now discard 2 cards
                    discard(game, 2);
                    return true;
                } else {
                    // fallback: land, cast imp if none on board
                    // TODO
                }
            } else if (game.getBoard().has("putrid imp")) {
                // I can discard a monster (any)
                game.discard(monstersInHand.getCards().get(0));
                return true;
            } else if (game.getHand().has("putrid imp") && canPay(game, Mana.of("B"))) {
                pay(game, Mana.of("B"));
                game
                        .castPermanent("putrid imp", Mana.of("B"))
                        // discard a monster (any)
                        .discard(monstersInHand.getCards().get(0));
                return true;
            } else if (game.getHand().has("faithless looting") && canPay(game, Mana.of("R"))) {
                pay(game, Mana.of("R"));
                game
                        .castNonPermanent("faithless looting", Mana.of("R"))
                        .draw(2)
                        // discard a monster (any)
                        .discard(monstersInHand.getCards().get(0));
                // then another card...
                discard(game, 1);
                return true;
            } else if (game.getGraveyard().has("faithless looting") && canPay(game, Mana.of("2R"))) {
                pay(game, Mana.of("2R"));
                game
                        .castNonPermanent(Area.GRAVEYARD, Area.EXILE, "faithless looting", Mana.of("2R"))
                        .draw(2)
                        // discard a monster (any)
                        .discard(monstersInHand.getCards().get(0));
                // then another card...
                discard(game, 1);
                return true;
            } else {

            }

            // if I have in hand: a monster + a discard + the required mana, let's do it!
        }

        // count available mana
        if (rules.findByName("turn 1 imp").matches(game.getHand(), rules) != null) {

        }
        // first land
//        Cards hand = game.getHand();
//        if (LANDS.stream().filter(hand::has).findFirst().isPresent()) {
//
//        }
        return false;
    }

    @Override
    public String checkWin() {
        super.checkWin();
        // consider I won as soon as I have a monster on the board
        if (game.getBoard().count(MONSTERS) > 0) {
            return "I reanimated a monster";
        }
        return null;
    }
}
