package org.mtgpeasant.perfectdeck.goldfish;

import org.mtgpeasant.perfectdeck.common.cards.Cards;

import java.io.PrintWriter;
import java.util.List;

public class GameMock {
    public static Game mock(boolean onThePlay, Cards hand, Cards library, Cards graveyard, List<Card> board, List<Card> exile) {
        Game game = new Game(onThePlay, new PrintWriter(System.out));
        game.keepHandAndStart(library, hand);
        game.getGraveyard().addAll(graveyard);
        game.getBoard().addAll(board);
        game.getExile().addAll(exile);
        return game;
    }
}
