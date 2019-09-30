package org.mtgpeasant.perfectdeck.goldfish;

import org.mtgpeasant.perfectdeck.common.cards.Cards;
import org.mtgpeasant.perfectdeck.common.matchers.MulliganRules;

import java.awt.geom.Area;

public class ReanimatorDeckPilot extends DeckPilot {
    public static final Mana B = Mana.of("B");
    public static final Mana B1 = Mana.of("1B");
    public static final Mana R = Mana.of("R");
    public static final Mana R2 = Mana.of("2R");
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
        if (mulligans >= 3) {
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
            if (game.getHand().contains("reanimate") && canPay(B)) {
                pay(B);
                game.castNonPermanent("reanimate", B);
                game.move(monstersInGy.getFirst(), Game.Area.graveyard, Game.Area.board);
                // done
                return false;
            } else if (!reanimators.isEmpty() && canPay(B1)) {
                pay(B1);
                game.castNonPermanent(reanimators.getFirst(), B1);
                game.move(monstersInGy.getFirst(), Game.Area.graveyard, Game.Area.board);
                // done
                return false;
            } else if (game.getHand().contains("gitaxian probe")) {
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
                if (game.getHand().contains("gitaxian probe")) {
                    game.castNonPermanent("gitaxian probe", Mana.zero()).draw(1);
                    return true;
                } else if (game.getHand().contains("faithless looting") && canPay(R)) {
                    pay(R);
                    game
                            .castNonPermanent("faithless looting", R)
                            .draw(2);
                    // now discard 2 cards
                    discard(2);
                    return true;
                } else if (game.getGraveyard().contains("faithless looting") && canPay(R2)) {
                    pay(R2);
                    game
                            .cast("faithless looting", Game.Area.graveyard, Game.Area.exile, R2)
                            .draw(2);
                    // then another card...
                    discard(2);
                    return true;
                } else {
                    // fallback: land, cast imp if none on board
                    // TODO
                }
            } else if (game.getBoard().contains("putrid imp")) {
                // I can discard a monster (any)
                game.discard(monstersInHand.getFirst());
                return true;
            } else if (game.getHand().contains("putrid imp") && canPay(B)) {
                pay(B);
                game
                        .castPermanent("putrid imp", B)
                        // discard a monster (any)
                        .discard(monstersInHand.getFirst());
                return true;
            } else if (game.getHand().contains("faithless looting") && canPay(R)) {
                pay(R);
                game
                        .castNonPermanent("faithless looting", R)
                        .draw(2)
                        // discard a monster (any)
                        .discard(monstersInHand.getFirst());
                // then another card...
                discard(1);
                return true;
            } else if(game.getHand().contains("greater sandwurm") && canPay(Mana.of("2"))) {
                // cycle
                pay(Mana.of("2"));
                game
                        .discard("greater sandwurm")
                        .draw(1);
                return true;
            } else if (game.getGraveyard().contains("faithless looting") && canPay(R2)) {
                pay(R2);
                game
                        .cast("faithless looting", Game.Area.graveyard, Game.Area.exile, R2)
                        .draw(2)
                        // discard a monster (any)
                        .discard(monstersInHand.getFirst());
                // then another card...
                discard(1);
                return true;
            } else {
                // TODO
            }
        }
    }

    private void discard(int number) {
        // TODO
    }

    private void pay(Mana mana) {
        // TODO
    }

    private boolean canPay(Mana mana) {
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
