package org.mtgpeasant.decks.theoretic;

import org.mtgpeasant.perfectdeck.common.Mana;
import org.mtgpeasant.perfectdeck.common.cards.Cards;
import org.mtgpeasant.perfectdeck.goldfish.DeckPilot;
import org.mtgpeasant.perfectdeck.goldfish.Game;

import static org.mtgpeasant.perfectdeck.goldfish.Card.withType;

/**
 * A deck made of mountains and lava spikes
 */
public class SpikesDeckPilot extends DeckPilot<Game> {
    public static final Mana R = Mana.of("R");
    private static String BOLT = "lava spike";
    private static String LAND = "mountain";

    public SpikesDeckPilot(Game game) {
        super(game);
    }

    /**
     * OTD, premier mulligan si on a 0, 1, 4, 5, 6 ou 7 terrains
     * OTD, second mulligan si on a 0, 5, 6 ou 7 terrains ; si on a 1 ou 2 terrains on met une foudre en dessous ; si on a 3 ou 4 terrains on met un terrain en dessous.
     * OTD, après le second mulligan si on a 0, 1 ou 2 terrains on met deux foudres en dessous ; si on a 3 terrains on met une foudre et une montagne en dessous et sinon on met deux montagnes en dessous.
     * OTP, premier mulligan si on a 0, 4, 5, 6 ou 7 terrains
     * OTP, second mulligan si on a 0, 5, 6 ou 7 terrains ; si on a 1 ou 2 terrains on met une foudre en dessous ; si on a 3 ou 4 terrains on met un terrain en dessous.
     * OTP, après le second mulligan si on a 0 ou 1 terrain on met deux foudres en dessous ; si on a 2 terrains on met une foudre et une montagne en dessous et sinon on met deux montagnes en dessous.
     * Résultats :
     * - Tour létal moyen OTD : 4,437 (optimal)
     * - Tour létal moyen OTP : 4,863 (optimal)
     */
    @Override
    public boolean keepHand(Cards hand) {
        int lands = hand.count(LAND);
        switch (game.getMulligans()) {
            case 0:
                if (game.isOnThePlay()) {
                    return lands > 0 && lands < 4;
                } else {
                    return lands > 1 && lands < 4;
                }
            case 1:
                return lands > 0 && lands < 5;
            default:
                // always keep
                return true;
        }
    }

    @Override
    public void start() {
        int lands = game.getHand().count(LAND);
        switch (game.getMulligans()) {
            case 0:
                return;
            case 1:
                if (lands <= 2) {
                    game.putOnBottomOfLibrary(BOLT);
                } else {
                    game.putOnBottomOfLibrary(LAND);
                }
                return;
            default: // 2 mulligans
                if (game.isOnThePlay()) {
                    if (lands <= 1) {
                        game.putOnBottomOfLibrary(BOLT);
                        game.putOnBottomOfLibrary(BOLT);
                    } else if (lands == 2) {
                        game.putOnBottomOfLibrary(BOLT);
                        game.putOnBottomOfLibrary(LAND);
                    } else {
                        game.putOnBottomOfLibrary(LAND);
                        game.putOnBottomOfLibrary(LAND);
                    }
                } else {
                    if (lands <= 2) {
                        game.putOnBottomOfLibrary(BOLT);
                        game.putOnBottomOfLibrary(BOLT);
                    } else if (lands == 3) {
                        game.putOnBottomOfLibrary(BOLT);
                        game.putOnBottomOfLibrary(LAND);
                    } else {
                        game.putOnBottomOfLibrary(LAND);
                        game.putOnBottomOfLibrary(LAND);
                    }
                }
                return;
        }
    }

    @Override
    public void firstMainPhase() {
        // land
        if (game.getHand().contains(LAND)) {
            game.land(LAND);
        }

        // draw all mana
        game.find(withType(Game.CardType.land)).forEach(land -> game.tapLandForMana(land, R));

        // cast all bolts
        while (game.getHand().contains(BOLT) && game.canPay(R)) {
            game.castSorcery(BOLT, R);
            game.damageOpponent(3);
        }
    }
}
