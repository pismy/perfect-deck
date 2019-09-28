package org.mtgpeasant.perfectdeck.goldfish;

import lombok.Getter;
import org.mtgpeasant.perfectdeck.common.cards.Cards;

import java.util.ArrayList;
import java.util.List;

@Getter
public class Game {
    private int currentTurn = 0;
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

    private List<String> tapped = new ArrayList<>();
    private Mana pool = Mana.zero();

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
        if (!board.has(cardName)) {
            throw new IllegalMoveException("Can't tap [" + cardName + "]: not on board");
        }
        if (tapped.contains(cardName)) {
            throw new IllegalMoveException("Can't tap [" + cardName + "]: already tapped");
        }
        tapped.add(cardName);
        return this;
    }

    public Game untap(String cardName) {
        if (!board.has(cardName)) {
            throw new IllegalMoveException("Can't untap [" + cardName + "]: not on board");
        }
//        if (!tapped.contains(cardName)) {
//            throw new IllegalMoveException("Can't untap [" + cardName + "]: already untapped");
//        }
        tapped.remove(cardName);
        return this;
    }

    public Game untapAll() {
        tapped.clear();
        return this;
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

    public Game land(String cardName) {
        if (!hand.has(cardName)) {
            throw new IllegalMoveException("Can't land [" + cardName + "]: not in hand");
        }
        if (landed) {
            throw new IllegalMoveException("Can't land [" + cardName + "]: can't land twice the same turn");
        }
        hand = hand.remove(cardName);
        board = board.add(cardName);
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
            hand = hand.add(library.draw());
        }
        return this;
    }

    public Game castPermanent(String cardName, Mana mana) {
        if (!hand.has(cardName)) {
            throw new IllegalMoveException("Can't play [" + cardName + "]: not in hand");
        }
        if (!has(mana)) {
            throw new IllegalMoveException("Can't play [" + cardName + "]: not enough mana");
        }
        hand = hand.remove(cardName);
        board = board.add(cardName);
        pay(mana);
        return this;
    }

    public Game castNonPermanent(String cardName, Mana mana) {
        if (!hand.has(cardName)) {
            throw new IllegalMoveException("Can't play [" + cardName + "]: not in hand");
        }
        if (!has(mana)) {
            throw new IllegalMoveException("Can't play [" + cardName + "]: not enough mana");
        }
        hand = hand.remove(cardName);
        graveyard = graveyard.add(cardName);
        pay(mana);
        return this;
    }

    public Game discard(String cardName) {
        if (!hand.has(cardName)) {
            throw new IllegalMoveException("Can't discard [" + cardName + "]: not in hand");
        }
        hand = hand.remove(cardName);
        graveyard = graveyard.add(cardName);
        return this;
    }

    public Game sacrifice(String cardName) {
        if (!board.has(cardName)) {
            throw new IllegalMoveException("Can't sacrifice [" + cardName + "]: not on board");
        }
        board = board.remove(cardName);
        graveyard = graveyard.add(cardName);
        return this;
    }

    public Game destroy(String cardName) {
        if (!board.has(cardName)) {
            throw new IllegalMoveException("Can't destroy [" + cardName + "]: not on board");
        }
        board = board.remove(cardName);
        graveyard = graveyard.add(cardName);
        return this;
    }
}
