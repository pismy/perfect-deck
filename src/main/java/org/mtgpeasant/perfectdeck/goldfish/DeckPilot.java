package org.mtgpeasant.perfectdeck.goldfish;

import org.mtgpeasant.perfectdeck.common.cards.Cards;

public class DeckPilot {
    public Game game;

    /**
     * Determines whether the given hand should be kept
     *
     * @param onThePlay whether you are on the play or on the drawPhase
     * @param mulligans mulligans counts (0 for first drawPhase)
     * @param hand      opening hand
     * @return {@code true} if the hand should be kept
     */
    public boolean keepHand(boolean onThePlay, int mulligans, Cards hand) {
        return true;
    }

    /**
     * Starts a game
     *
     * @param mulligans number of mulligans taken - therefore that number of cards should be sent at the bottom of the library
     * @param game      game
     */
    public void startGame(int mulligans, Game game) {
        this.game = game;
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
