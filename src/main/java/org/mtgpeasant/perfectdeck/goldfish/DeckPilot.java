package org.mtgpeasant.perfectdeck.goldfish;

import org.mtgpeasant.perfectdeck.common.cards.Cards;

public class DeckPilot {
    /**
     * Determines whether the given hand should be kept
     *
     * @param hand      opening hand
     * @param mulligans mulligans counts (0 for first draw)
     * @return {@code true} if the hand should be kept
     */
    public boolean keep(Cards hand, int mulligans) {
        return true;
    }

    public void untap(Game game) {
        game.untapAll();
    }

    public void upkeep(Game game) {

    }

    public void draw(Game game) {
        game.draw(1);
    }

    public void firstMainPhase(Game game) {

    }

    public void combatPhase(Game game) {

    }

    public void secondMainPhase(Game game) {

    }

    public void endingPhase(Game game) {

    }

    public String checkWin(Game game) {
        if (game.getOpponentLife() <= 0) {
            return "opponent is dead";
        }
        if (game.getOpponentPoisonCounters() >= 10) {
            return "opponent is deadly poisoned";
        }
        return null;
    }

    /**
     * Get rid of cards after one or several mulligans
     *
     * @param cards number of cards to get rid of
     * @param game  game
     */
    public void getRidOfCards(int cards, Game game) {
        throw new RuntimeException("getRidOfCards() not implemented yet");
    }
}
