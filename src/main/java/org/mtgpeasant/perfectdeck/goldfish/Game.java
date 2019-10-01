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

    /**
     * Pay the given mana from mana pool
     *
     * @param mana mana to pay
     */
    public Game pay(Mana mana) {
        if (!has(mana)) {
            throw new IllegalMoveException("Can't pay " + mana + ": not enough mana in pool (" + pool + ")");
        }
        pool = pool.minus(mana);
        return this;
    }

    /**
     * Adds the given mana to pool
     *
     * @param mana mana to add
     */
    public Game add(Mana mana) {
        pool = pool.plus(mana);
        return this;
    }

    /**
     * Checks whether we have given mana in pool
     *
     * @param mana required mana
     */
    public boolean has(Mana mana) {
        return pool.contains(mana);
    }

    /**
     * Tap the given cards
     *
     * @param cardName card name
     */
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

    /**
     * Untap the given card
     *
     * @param cardName card name
     */
    public Game untap(String cardName) {
        if (!board.contains(cardName)) {
            throw new IllegalMoveException("Can't untap [" + cardName + "]: not on board");
        }
        tapped.remove(cardName);
        return this;
    }

    /**
     * Untap all permanents
     */
    public Game untapAll() {
        tapped.clear();
        return this;
    }

    /**
     * Drop the given land
     *
     * @param cardName land card name
     */
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

    /**
     * Damage opponent
     *
     * @param damage damage amount
     */
    public Game damageOpponent(int damage) {
        opponentLife -= damage;
        return this;
    }

    /**
     * Put poison counters on opponent
     *
     * @param counters number of poison counters
     */
    public Game poisonOpponent(int counters) {
        opponentPoisonCounters += counters;
        return this;
    }

    /**
     * Shuffle the library
     */
    public Game shuffleLibrary() {
        library = library.shuffle();
        return this;
    }

    /**
     * Draw several cards from library
     *
     * @param cards number of cards to draw
     */
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

    /**
     * Cast the given spell
     *
     * @param cardName spell card name
     * @param from     origin area
     * @param to       target area
     * @param cost     mana cost
     */
    public Game cast(String cardName, Area from, Area to, Mana cost) {
        if (!has(cost)) {
            throw new IllegalMoveException("Can't cast [" + cardName + "]: not enough cost");
        }
        pay(cost);
        return move(cardName, from, to);
    }

    /**
     * Cast a permanent spell from the hand
     *
     * @param cardName spell name
     * @param cost     mana cost
     */
    public Game castPermanent(String cardName, Mana cost) {
        return cast(cardName, Area.hand, Area.board, cost);
    }

    /**
     * Cast a non-permanent spell from the hand
     *
     * @param cardName spell name
     * @param mana     cost
     */
    public Game castNonPermanent(String cardName, Mana mana) {
        return cast(cardName, Area.hand, Area.graveyard, mana);
    }

    /**
     * Cast a card from the hand
     *
     * @param cardName card name
     */
    public Game discard(String cardName) {
        return move(cardName, Area.hand, Area.graveyard);
    }

    /**
     * Discard the first card of the given list that is found in hand
     *
     * @param cards cards to discard, ordered by preference
     * @return discarded card name, or {@code null} if none
     */
    public String discardOneOf(String... cards) {
        String selected = getHand().hasOne(cards);
        if (selected != null) {
            discard(selected);
        }
        return selected;
    }

    /**
     * Sacrifice a permanent
     *
     * @param cardName permanent name
     */
    public Game sacrifice(String cardName) {
        tapped.remove(cardName);
        return move(cardName, Area.board, Area.graveyard);
    }

    /**
     * Sacrifice a permanent
     *
     * @param cardName permanent name
     */
    public Game destroy(String cardName) {
        tapped.remove(cardName);
        return move(cardName, Area.board, Area.graveyard);
    }

    /**
     * Put a card from the hand on the bottom of the library
     *
     * @param cardName card name
     */
    public Game putOnBottomOfLibrary(String cardName) {
        return move(cardName, Game.Area.hand, Game.Area.library, Game.Side.bottom);
    }

    /**
     * Put the first card of the given list that is found in hand on the bottom of the library
     *
     * @param cards cards to ret rid of, ordered by preference
     * @return discarded card name, or {@code null} if none
     */
    public String putOnBottomOfLibraryOneOf(String... cards) {
        String selected = getHand().hasOne(cards);
        if (selected != null) {
            discard(selected);
        }
        return selected;
    }

}
