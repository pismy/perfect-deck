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

    public void untap(Game context) {
        context.untapAll();
    }

    public void upkeep(Game context) {

    }

    public void draw(Game context) {
        context.draw(1);
    }

    public void firstMainPhase(Game context) {

    }

    public void combatPhase(Game context) {

    }

    public void secondMainPhase(Game context) {

    }

    public void endingPhase(Game context) {

    }

    public String checkWin(Game context) {
        if (context.getOpponentLife() <= 0) {
            return "opponent is dead";
        }
        if (context.getOpponentPoisonCounters() >= 10) {
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
