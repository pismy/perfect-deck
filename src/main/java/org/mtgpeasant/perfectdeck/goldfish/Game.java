package org.mtgpeasant.perfectdeck.goldfish;

import lombok.Getter;
import lombok.ToString;
import org.mtgpeasant.perfectdeck.common.cards.Cards;

@Getter
@ToString(exclude = "library")
public class Game {
    public enum Area {hand, library, board, exile, graveyard}

    public enum Side {top, bottom}

    private int currentTurn = 1;
    private int opponentLife = 20;
    private int opponentPoisonCounters = 0;
    private boolean landed = false;

    private Cards library;
    private Cards hand;
    private Cards board = Cards.none();
    private Cards exile = Cards.none();
    private Cards graveyard = Cards.none();

    Game(Cards library, Cards hand) {
        this.library = library;
        this.hand = hand;
    }

    private Cards tapped = Cards.none();
    private Mana pool = Mana.zero();

    Cards area(Area area) {
        switch (area) {
            case hand:
                return hand;
            case library:
                return library;
            case board:
                return board;
            case exile:
                return exile;
            case graveyard:
                return graveyard;
            default:
                return null;
        }
    }

    Game startNextTurn() {
        currentTurn++;
        landed = false;
        emptyPool();
        return this;
    }

    Game emptyPool() {
        pool = Mana.zero();
        return this;
    }

    public Game pay(Mana mana) {
        if (!has(mana)) {
            throw new IllegalMoveException("Can't pay " + mana + ": not enough mana in pool (" + pool + ")");
        }
        pool = pool.minus(mana);
        return this;
    }

    public Game add(Mana mana) {
        pool = pool.plus(mana);
        return this;
    }

    public boolean has(Mana mana) {
        return pool.contains(mana);
    }

    public Game tap(String cardName) {
        int countOnBoard = board.count(cardName);
        if (countOnBoard == 0) {
            throw new IllegalMoveException("Can't tap [" + cardName + "]: not on board");
        }
        int countTapped = tapped.count(cardName);
        if (countTapped >= countOnBoard) {
            throw new IllegalMoveException("Can't tap [" + cardName + "]: already tapped");
        }
        tapped.add(cardName);
        return this;
    }

    public Game untap(String cardName) {
        if (!board.contains(cardName)) {
            throw new IllegalMoveException("Can't untap [" + cardName + "]: not on board");
        }
        tapped.remove(cardName);
        return this;
    }

    public Game untapAll() {
        tapped.clear();
        return this;
    }

    public Game land(String cardName) {
        if (!hand.contains(cardName)) {
            throw new IllegalMoveException("Can't land [" + cardName + "]: not in hand");
        }
        if (landed) {
            throw new IllegalMoveException("Can't land [" + cardName + "]: can't land twice the same turn");
        }
        hand.remove(cardName);
        board.add(cardName);
        landed = true;
        return this;
    }

    public Game damageOpponent(int damage) {
        opponentLife -= damage;
        return this;
    }

    public Game poisonOpponent(int counters) {
        opponentPoisonCounters += counters;
        return this;
    }

    public Game shuffleLibrary() {
        library = library.shuffle();
        return this;
    }

    public Game draw(int cards) {
        if (library.size() < cards) {
            throw new GameLostException("Can't draw [" + cards + "]: not enough cards");
        }
        for (int i = 0; i < cards; i++) {
            hand.add(library.draw());
        }
        return this;
    }

    /**
     * Moves the given card from the given origin area to the top of the given target area
     *
     * @param cardName name of the card to move
     * @param from     origin area
     * @param to       target area
     * @return this
     */
    public Game move(String cardName, Area from, Area to) {
        return move(cardName, from, to, Side.top);
    }

    /**
     * Moves the given card from the given origin area to the given target area
     *
     * @param cardName name of the card to move
     * @param from     origin area
     * @param to       target area
     * @param side     side of the target area
     * @return this
     */
    public Game move(String cardName, Area from, Area to, Side side) {
        Cards fromArea = area(from);
        if (!fromArea.contains(cardName)) {
            throw new IllegalMoveException("Can't move [" + cardName + "]: not in " + from);
        }
        fromArea.remove(cardName);
        if (side == Side.top) {
            area(to).addFirst(cardName);
        } else {
            area(to).addLast(cardName);
        }
        return this;
    }

    public Game cast(String cardName, Area from, Area to, Mana mana) {
        if (!has(mana)) {
            throw new IllegalMoveException("Can't cast [" + cardName + "]: not enough mana");
        }
        pay(mana);
        return move(cardName, from, to);
    }

    public Game castPermanent(String cardName, Mana mana) {
        return cast(cardName, Area.hand, Area.board, mana);
    }

    public Game castNonPermanent(String cardName, Mana mana) {
        return cast(cardName, Area.hand, Area.graveyard, mana);
    }

    public Game discard(String cardName) {
        return move(cardName, Area.hand, Area.graveyard);
    }

    public Game sacrifice(String cardName) {
        return move(cardName, Area.board, Area.graveyard);
    }

    public Game destroy(String cardName) {
        return move(cardName, Area.board, Area.graveyard);
    }
}
