package org.mtgpeasant.perfectdeck.goldfish;

import org.mtgpeasant.perfectdeck.common.cards.Cards;

public class DeckPilot {
    protected Game game;

    void setGame(Game game) {
        this.game = game;
    }

    /**
     * Determines whether the given hand should be kept
     *
     * @param hand opening hand
     * @return {@code true} if the hand should be kept
     */
    public boolean keepHand(Cards hand) {
        return true;
    }

    /**
     * Starts a game
     * <p>
     * Mulligans taken shall be applied (N cards sent on the bottom of the library)
     */
    public void start() {
    }

    public void untapPhase() {
        game.untapAll();
    }

    public void upkeepPhase() {

    }

    public void drawPhase() {
        game.draw(1);
    }

    public void firstMainPhase() {

    }

    public void combatPhase() {

    }

    public void secondMainPhase() {

    }

    public void endingPhase() {

    }

    public String checkWin() {
        if (game.getOpponentLife() <= 0) {
            return "opponent is dead";
        }
        if (game.getOpponentPoisonCounters() >= 10) {
            return "opponent is deadly poisoned";
        }
        return null;
    }
}
