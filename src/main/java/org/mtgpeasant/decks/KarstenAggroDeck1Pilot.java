package org.mtgpeasant.decks;

import org.mtgpeasant.perfectdeck.common.cards.Cards;
import org.mtgpeasant.perfectdeck.goldfish.DeckPilot;
import org.mtgpeasant.perfectdeck.goldfish.Game;
import org.mtgpeasant.perfectdeck.common.Mana;

import static org.mtgpeasant.perfectdeck.goldfish.Game.Area.hand;
import static org.mtgpeasant.perfectdeck.goldfish.Game.Area.library;

/**
 * See: https://www.channelfireball.com/articles/frank-analysis-finding-the-optimal-aggro-deck-via-computer-simulation/
 * <p>
 * https://pastebin.com/rXGLDind
 */
public class KarstenAggroDeck1Pilot extends DeckPilot {
    public static final Mana ONE = Mana.of("1");
    public static final Mana TWO = Mana.of("2");
    private static String LIONS = "savannah lions";
    private static String LEECH = "putrid leech";
    private static String BOLT = "lightning bolt";
    private static String LAND = "city of brass";

    /**
     * The simple idea is that we're keeping a hand if it contains between 2 and 5 lands and at least one spell of 3 mana or less.
     * <p>
     * Also, keep any 2-card hand.
     */
    @Override
    public boolean keepHand(Cards hand) {
        int lands = hand.count(LAND);
        int onedrops = hand.count(LIONS, BOLT);
        int twodrops = hand.count(LEECH);
        switch (game.getMulligans()) {
            case 0:
                return lands >= 2 && lands <= 5 && (onedrops + twodrops >= 1);
            case 1:
                return lands >= 1 && lands <= 4 && (onedrops + twodrops >= 1);
            case 2:
            case 3:
                return lands >= 1 && lands <= 4;
            default:
                // always keep
                return true;
        }
    }

    @Override
    public void start() {
        // place one card on the bottom of the library for each mulligan
        for (int i = 0; i < game.getMulligans(); i++) {
            int lands = game.getHand().count(LAND);
            if (lands > 2) {
                game.move(LAND, hand, library, Game.Side.bottom);
            } else if (game.getHand().contains(LEECH)) {
                game.move(LEECH, hand, library, Game.Side.bottom);
            } else if (game.getHand().contains(BOLT)) {
                game.move(BOLT, hand, library, Game.Side.bottom);
            } else {
                game.move(LIONS, hand, library, Game.Side.bottom);
            }
        }
    }

    @Override
    public void firstMainPhase() {
        // land
        if (game.getHand().contains(LAND)) {
            game.land(LAND);
        }
    }

    @Override
    public void combatPhase() {
        // attack with all creatures on board
        game.getBoard().select(LIONS).forEach(crea -> game.tapForAttack(LIONS, 2));
        game.getBoard().select(LEECH).forEach(crea -> game.tapForAttack(LEECH, 4));
    }

    @Override
    public void secondMainPhase() {
        // draw all mana
        game.getBoard().select(LAND).forEach(land -> game.tapLandForMana(LAND, ONE));

        int castableBolts = Math.min(game.getPool().ccm(), game.getHand().count(BOLT));
        if (castableBolts * 3 >= game.getOpponentLife()) {
            // my bolts can kill the opponent: go ahead
            for (int i = 0; i < castableBolts; i++) {
                game.castNonPermanent(BOLT, ONE).damageOpponent(3);
            }
        } else {
            // cast as many leeches as we can
            while (game.getPool().ccm() >= 2 && game.getHand().contains(LEECH)) {
                game.castPermanent(LEECH, TWO);
            }
            // cast as many lions as we can
            while (game.getPool().ccm() >= 1 && game.getHand().contains(LIONS)) {
                game.castPermanent(LIONS, ONE);
            }
            // finally cast as many bolts as we can
            while (game.getPool().ccm() >= 1 && game.getHand().contains(BOLT)) {
                game.castNonPermanent(BOLT, ONE);
            }
        }
    }
}
