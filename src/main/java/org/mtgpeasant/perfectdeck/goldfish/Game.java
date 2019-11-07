package org.mtgpeasant.perfectdeck.goldfish;

import lombok.Getter;
import lombok.ToString;
import org.mtgpeasant.perfectdeck.common.Mana;
import org.mtgpeasant.perfectdeck.common.cards.Cards;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Getter
@ToString(exclude = "library")
public class Game {

    public enum Phase {beginning, first_main, combat, second_main, ending}

    public enum Area {hand, library, board, exile, graveyard}

    public enum Side {top, bottom}

    public enum CardType {artifact, creature, enchantment, instant, land, planeswalker, sorcery}

    // game state
    private final boolean onThePlay;
    private final PrintWriter logs;
    private int mulligans = 0;
    private int currentTurn = 0;
    private int opponentLife = 20;
    private int opponentPoisonCounters = 0;

    private Cards library;
    private Cards hand;

    private Cards board = Cards.none();
    private Cards exile = Cards.none();
    private Cards graveyard = Cards.none();
    private Cards tapped = Cards.none();

    private List<Counters> counters = new ArrayList<>();

    // turn and phase state
    private Phase currentPhase;
    private boolean landed = false;
    private Mana pool = Mana.zero();


    protected Game(boolean onThePlay, PrintWriter logs) {
        this.onThePlay = onThePlay;
        this.logs = logs;
    }

    protected void keepHandAndStart(Cards library, Cards hand) {
        this.library = library;
        this.hand = hand;
        log("hand #" + mulligans + " " + hand + " kept");
    }

    protected void rejectHand(Cards hand) {
        log("hand #" + mulligans + " " + hand + " rejected: take mulligan");
        mulligans++;
    }

    /**
     * Returns the cards releated to the given area
     *
     * @param area area
     * @return cards
     */
    protected Cards area(Area area) {
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

    protected void startNextTurn() {
        currentTurn++;
        landed = false;
        _emptyPool();

        // log
        log("=== Turn " + currentTurn + " ===");
        log("> opponent life: " + opponentLife);
        if (opponentPoisonCounters > 0) {
            log("> opponent poison counters: " + opponentPoisonCounters);
        }
        log("> hand: " + hand);
        log("> board: " + board);
        if (!graveyard.isEmpty()) {
            log("> graveyard: " + graveyard);
        }
        if (!exile.isEmpty()) {
            log("> exile: " + exile);
        }
    }

    protected void startPhase(Phase phase) {
        _emptyPool();
        currentPhase = phase;
    }

    protected void _emptyPool() {
        pool = Mana.zero();
    }

    protected void _pay(Mana cost) {
        if (!canPay(cost)) {
            throw new IllegalActionException("Can't pay " + cost + ": not enough mana in pool (" + pool + ")");
        }
        pool = pool.minus(cost);
    }

    protected void _add(Mana mana) {
        pool = pool.plus(mana);
    }

    protected void _tap(String cardName) {
        int countOnBoard = board.count(cardName);
        if (countOnBoard == 0) {
            throw new IllegalActionException("Can't tap [" + cardName + "]: not on board");
        }
        int countTapped = tapped.count(cardName);
        if (countTapped >= countOnBoard) {
            throw new IllegalActionException("Can't tap [" + cardName + "]: all tapped");
        }
        tapped.add(cardName);
    }

    protected void _untap(String cardName) {
        if (!board.contains(cardName)) {
            throw new IllegalActionException("Can't untap [" + cardName + "]: not on board");
        }
        tapped.remove(cardName);
    }

    protected void _damageOpponent(int damage) {
        opponentLife -= damage;
    }

    protected void _move(String cardName, Area from, Area to, Side side) {
        Cards fromArea = area(from);
        if (!fromArea.contains(cardName)) {
            throw new IllegalActionException("Can't move [" + cardName + "]: not in " + from);
        }
        fromArea.remove(cardName);
        if (side == Side.top) {
            area(to).addFirst(cardName);
        } else {
            area(to).addLast(cardName);
        }
        if (from == Area.board) {
            tapped.remove(cardName);
        }
    }

    /**
     * Add a counter of the given type on the given cards
     *
     * @param type   counter type
     * @param card   card name
     * @param area   card area
     * @param number number of counters
     */
    public void addCounter(String type, String card, Area area, int number) {
        counters.add(Counters.builder().type(type).card(card).area(area).number(number).build());
    }

    /**
     * Returns all counters of the given type
     *
     * @param type counter type
     * @return list of counters
     */
    public List<Counters> counters(String type) {
        return counters.stream().filter(ct -> ct.getType().equals(type)).collect(Collectors.toList());
    }

    /**
     * Pay the given mana from mana pool
     *
     * @param cost mana cost
     */
    public void pay(Mana cost) {
        log("- pay " + cost);
        _pay(cost);
    }

    /**
     * Adds the given mana to pool
     *
     * @param mana mana to add
     */
    public void add(Mana mana) {
        log("- add " + mana + " to mana pool");
        _add(mana);
    }

    /**
     * Checks whether we have given mana in pool
     *
     * @param cost mana cost
     */
    public boolean canPay(Mana cost) {
        return pool.contains(cost);
    }

    /**
     * Tap the given cards
     *
     * @param cardName card name
     */
    public void tap(String cardName) {
        log("- tap [" + cardName + "]");
        _tap(cardName);
    }

    /**
     * Tap a land and produce mana
     *
     * @param cardName land name
     * @param mana     produced mana
     */
    public void tapLandForMana(String cardName, Mana mana) {
        log("- tap [" + cardName + "] and add " + mana + " to mana pool");
        _tap(cardName);
        _add(mana);
    }

    /**
     * Tap a creature to attack (damage the opponent)
     *
     * @param cardName creature name
     * @param strength creature strength
     */
    public void tapForAttack(String cardName, int strength) {
        log("- attack with [" + cardName + "] for " + strength + " (" + opponentLife + "->" + (opponentLife - strength) + ")");
        _damageOpponent(strength);
        _tap(cardName);
    }

    /**
     * Untap the given card
     *
     * @param cardName card name
     */
    public void untap(String cardName) {
        log("- untap [" + cardName + "]");
        _untap(cardName);
    }

    /**
     * Untap all permanents
     */
    public void untapAll() {
        log("- untap all");
        tapped.clear();
    }

    /**
     * Returns all untapped cards on the board matching the card names
     *
     * @param cards cards to select
     */
    public Cards getUntapped(String... cards) {
        Cards all = board.findAll(cards);
        tapped.forEach(all::remove);
        return all;
    }

    /**
     * Counts all untapped cards on the board matching the card names
     *
     * @param cards cards to select
     */
    public int countUntapped(String... cards) {
        return getUntapped(cards).size();
    }

//    /**
//     * Returns all tapped cards on the board matching the card names
//     *
//     * @param cards cards to select
//     */
//    public Cards getTapped(String... cards) {
//        return tapped.findAll(cards);
//    }
//
//    /**
//     * Counts all tapped cards on the board matching the card names
//     *
//     * @param cards cards to select
//     */
//    public int countTapped(String... cards) {
//        return tapped.count(cards);
//    }

    /**
     * Looks for the first untapped card matching one of the given names
     *
     * @param cards card names to look for
     * @return found card, or {@code null} if none was found
     */
    public Optional<String> findFirstUntapped(String... cards) {
        for (String card : cards) {
            if (board.count(card) > tapped.count(card)) {
                return Optional.of(card);
            }
        }
        return Optional.empty();
    }

    /**
     * Drop the given land
     *
     * @param cardName land card name
     */
    public void land(String cardName) {
        if (!hand.contains(cardName)) {
            throw new IllegalActionException("Can't land [" + cardName + "]: not in hand");
        }
        if (landed) {
            throw new IllegalActionException("Can't land [" + cardName + "]: can't land twice the same turn");
        }
        log("- land [" + cardName + "]");
        hand.remove(cardName);
        board.add(cardName);
        landed = true;
    }

    /**
     * Damage opponent
     *
     * @param damage damage amount
     */
    public void damageOpponent(int damage, String reason) {
        log("- damage" + (reason == null ? "" : " (" + reason + ")") + ": " + damage + " (" + opponentLife + "->" + (opponentLife - damage) + ")");
        _damageOpponent(damage);
    }

    /**
     * Damage opponent
     *
     * @param damage damage amount
     */
    public void damageOpponent(int damage) {
        damageOpponent(damage, null);
    }

    /**
     * Put poison counters on opponent
     *
     * @param counters number of poison counters
     */
    public void poisonOpponent(int counters) {
        opponentPoisonCounters += counters;
        log("- poison: " + counters + " (total: " + opponentPoisonCounters + ")");
    }

    /**
     * Shuffle the library
     */
    public void shuffleLibrary() {
        log("- shuffle library");
        library = library.shuffle();
    }

    /**
     * Draw several cards from library
     *
     * @param cards number of cards to draw
     */
    public Cards draw(int cards) {
        Cards drawn = library.draw(cards);
        log("- draw " + cards + ": " + drawn);
        hand.addAll(drawn);
        return drawn;
    }

    /**
     * Moves the given card from the given origin area to the given target area
     *
     * @param cardName name of the card to move
     * @param from     origin area
     * @param to       target area
     * @param side     side of the target area
     */
    public void move(String cardName, Area from, Area to, Side side) {
        log("- move [" + cardName + "] from " + from + " to " + (side == Side.top ? "" : "bottom of ") + to);
        _move(cardName, from, to, side);
    }

    /**
     * Moves the given card from the given origin area to the top of the given target area
     *
     * @param cardName name of the card to move
     * @param from     origin area
     * @param to       target area
     */
    public void move(String cardName, Area from, Area to) {
        move(cardName, from, to, Side.top);
    }

    /**
     * Cast the given spell
     *
     * @param cardName spell card name
     * @param from     origin area
     * @param to       target area
     * @param cost     mana cost
     */
    public void cast(String cardName, Area from, Area to, Mana cost) {
        log("- cast [" + cardName + "]" + (from == Area.hand ? "" : " from " + from) + (to == Area.graveyard ? "" : " to " + to) + " for " + cost);
        _pay(cost);
        _move(cardName, from, to, Side.top);
    }

    /**
     * Cast a permanent spell from the hand
     *
     * @param cardName spell name
     * @param cost     mana cost
     */
    public void castPermanent(String cardName, Mana cost) {
        cast(cardName, Area.hand, Area.board, cost);
    }

    /**
     * Cast a non-permanent spell from the hand
     *
     * @param cardName spell name
     * @param mana     cost
     */
    public void castNonPermanent(String cardName, Mana mana) {
        cast(cardName, Area.hand, Area.graveyard, mana);
    }

    /**
     * Cast a card from the hand
     *
     * @param cardName card name
     */
    public void discard(String cardName) {
        log("- discard [" + cardName + "]");
        _move(cardName, Area.hand, Area.graveyard, Side.top);
    }

    /**
     * Discard the first card of the given list that is found in hand
     *
     * @param cards cards to discard, ordered by preference
     * @return discarded card name, or {@code null} if none
     */
    public Optional<String> discardOneOf(String... cards) {
        Optional<String> selected = getHand().findFirst(cards);
        selected.ifPresent(this::discard);
        return selected;
    }

    /**
     * Sacrifice a permanent
     *
     * @param cardName permanent name
     */
    public void sacrifice(String cardName) {
        log("- sacrifice [" + cardName + "]");
        _move(cardName, Area.board, Area.graveyard, Side.top);
    }

    /**
     * Sacrifice a permanent
     *
     * @param cardName permanent name
     */
    public void destroy(String cardName) {
        log("- destroy [" + cardName + "]");
        _move(cardName, Area.board, Area.graveyard, Side.top);
    }

    /**
     * Put a card from the hand on the bottom of the library
     *
     * @param cardName card name
     */
    public void putOnBottomOfLibrary(String cardName) {
        move(cardName, Game.Area.hand, Game.Area.library, Game.Side.bottom);
    }

    /**
     * Put the first card of the given list that is found in hand on the bottom of the library
     *
     * @param cards cards to ret rid of, ordered by preference
     * @return discarded card name, or {@code null} if none
     */
    public Optional<String> putOnBottomOfLibraryOneOf(String... cards) {
        Optional<String> selected = getHand().findFirst(cards);
        selected.ifPresent(this::putOnBottomOfLibrary);
        return selected;
    }

    public void log(String message) {
        if (logs == null) {
            return;
        }
        logs.println(message);
    }
}
