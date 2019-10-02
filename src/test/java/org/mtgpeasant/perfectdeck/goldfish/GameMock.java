package org.mtgpeasant.perfectdeck.goldfish;

import org.mtgpeasant.perfectdeck.common.cards.Cards;

public class GameMock {
    public static Game mock(boolean onThePlay, Cards hand, Cards library, Cards graveyard, Cards board, Cards exile, DeckPilot pilot) {
        Game game = new Game(System.out);
        game.start(onThePlay);
        game.keepHandAndStart(library, hand);
        game.getGraveyard().addAll(graveyard);
        game.getBoard().addAll(board);
        game.getExile().addAll(exile);
        pilot.setGame(game);

        return game;
    }
}
