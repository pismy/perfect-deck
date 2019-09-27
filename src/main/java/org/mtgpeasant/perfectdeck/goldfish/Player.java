package org.mtgpeasant.perfectdeck.goldfish;

import org.mtgpeasant.perfectdeck.common.cards.Cards;

public class Player {
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

    public boolean hasWon(Game context) {
        return context.getOpponentLife() <= 0 || context.getOpponentPoisonCounters() >= 10;
    }

}
