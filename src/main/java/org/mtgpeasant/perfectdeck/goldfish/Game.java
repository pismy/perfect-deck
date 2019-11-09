package org.mtgpeasant.perfectdeck.goldfish;

import lombok.Getter;
import lombok.ToString;
import org.mtgpeasant.perfectdeck.common.Mana;
import org.mtgpeasant.perfectdeck.common.cards.Cards;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static org.mtgpeasant.perfectdeck.goldfish.Card.card;
import static org.mtgpeasant.perfectdeck.goldfish.Card.tapped;
import static org.mtgpeasant.perfectdeck.goldfish.Card.withName;

@Getter
@ToString(exclude = "library")
public class Game {

    public enum Phase {beginning, first_main, combat, second_main, ending}

    public enum Area {hand, library, board, exile, graveyard}

    public enum Side {top, bottom}

    public enum CardType {artifact, creature, enchantment, instant, land, planeswalker, sorcery, token}

    // game state
    private final boolean onThePlay;
    private final PrintWriter logs;
    private int mulligans = 0;
    private int currentTurn = 0;
    private int opponentLife = 20;
    private int opponentPoisonCounters = 0;

    private Cards library;
    private Cards hand;

    private List<Card> board = new ArrayList<>();
    private List<Card> exile = new ArrayList<>();
    private Cards graveyard = Cards.none();

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
    protected Object area(Area area) {
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

//    protected void _tap(String cardName) {
//        Optional<Card> untapped = findFirst(withName(cardName).and(untapped()));
//        if (untapped.isPresent()) {
//            untapped.get().setTapped(true);
//        } else {
//            throw new IllegalActionException("Can't tap [" + cardName + "]: not on board or all tapped");
//        }
//    }

//    protected void _untap(String cardName) {
//        Optional<Card> tapped = findFirst(withName(cardName).and(tapped()));
//        if (tapped.isPresent()) {
//            tapped.get().setTapped(false);
//        }
//    }

    protected void _damageOpponent(int damage) {
        opponentLife -= damage;
    }

    protected Card _move(Object cardOrName, Area from, Area to, Side side, CardType... types) {
        // remove from
        Object fromArea = area(from);
        String cardName = cardOrName instanceof String ? (String) cardOrName : ((Card) cardOrName).getName();
        if (fromArea instanceof Cards) {
            Cards cards = (Cards) fromArea;
            if (!cards.contains(cardName)) {
                throw new IllegalActionException("Can't move [" + cardName + "]: not in " + from);
            }
            cards.remove(cardName);
        } else {
            List<Card> cards = (List<Card>) fromArea;
            Optional<Card> card = cardOrName instanceof String ? cards.stream().filter(withName(cardName)).findFirst() : Optional.of((Card) cardOrName);
            if (!card.isPresent()) {
                throw new IllegalActionException("Can't move [" + cardName + "]: not in " + from);
            }
            cards.remove(card.get());
        }
        // add to
        Object toArea = area(to);
        if (toArea instanceof Cards) {
            Cards cards = (Cards) toArea;
            if (side == Side.top) {
                cards.addFirst(cardName);
            } else {
                cards.addLast(cardName);
            }
            return null;
        } else {
            List<Card> cards = (List<Card>) toArea;
            Card moved = card(cardName, types);
            if (side == Side.top) {
                cards.add(0, moved);
            } else {
                cards.add(moved);
            }
            return moved;
        }
    }

    /**
     * Finds first card on board matching the given filter
     *
     * @param filter card filter
     * @return matching card
     */
    public Optional<Card> findFirst(Predicate<Card> filter) {
        return board.stream().filter(filter).findFirst();
    }

    /**
     * Finds all cards on board matching the given filter
     *
     * @param filter card filter
     * @return matching cards
     */
    public List<Card> find(Predicate<Card> filter) {
        return board.stream().filter(filter).collect(Collectors.toList());
    }

    /**
     * Counts all cards on board matching the given filter
     *
     * @param filter card filter
     * @return matching cards count
     */
    public int count(Predicate<Card> filter) {
        return (int) board.stream().filter(filter).count();
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
        _add(mana);
        log("- add " + mana + " to mana pool (" + pool + ")");
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
     * @param card card
     */
    public void tap(Card card) {
        log("- tap [" + card + "]");
        if (card.isTapped()) {
            throw new IllegalActionException("Can't tap [" + card + "]: already tapped");
        }
        card.setTapped(true);
    }

    /**
     * Tap a land and produce mana
     *
     * @param card land card
     * @param mana produced mana
     */
    public void tapLandForMana(Card card, Mana mana) {
        log("- tap [" + card + "] and add " + mana + " to mana pool");
        if (card.isTapped()) {
            throw new IllegalActionException("Can't tap [" + card + "]: already tapped");
        }
        card.setTapped(true);
        _add(mana);
    }

    /**
     * Tap a creature to attack (damage the opponent)
     *
     * @param card     creature card
     * @param strength creature strength
     */
    public void tapForAttack(Card card, int strength) {
        log("- attack with [" + card + "] for " + strength + " (" + (opponentLife - strength) + ")");
        if (card.isTapped()) {
            throw new IllegalActionException("Can't tap [" + card + "]: already tapped");
        }
        _damageOpponent(strength);
        card.setTapped(true);
    }

//    /**
//     * Untap the given card
//     *
//     * @param cardName card name
//     */
//    public void untap(String cardName) {
//        log("- untap [" + cardName + "]");
//        _untap(cardName);
//    }

    /**
     * Untap all permanents
     */
    public void untapAll() {
        log("- untap all");
        board.stream().filter(tapped()).forEach(card -> card.setTapped(false));
    }

    /**
     * Drop the given land
     *
     * @param cardName land card name
     */
    public Card land(String cardName) {
        if (!hand.contains(cardName)) {
            throw new IllegalActionException("Can't land [" + cardName + "]: not in hand");
        }
        if (landed) {
            throw new IllegalActionException("Can't land [" + cardName + "]: can't land twice the same turn");
        }
        log("- land [" + cardName + "]");
        hand.remove(cardName);
        Card card = card(cardName, CardType.land);
        board.add(card);
        landed = true;
        return card;
    }

    /**
     * Create a token
     *
     * @param name the token card name
     */
    public Card createToken(String name, CardType... types) {
        log("- create token [" + name + "]");
        CardType[] types2 = new CardType[types.length + 1];
        types2[0] = CardType.token;
        System.arraycopy(types, 0, types2, 1, types.length);
        Card card = card(name, types2);
        board.add(card);
        return card;
    }

    /**
     * Damage opponent
     *
     * @param damage damage amount
     */
    public void damageOpponent(int damage, String reason) {
        log("- damage" + (reason == null ? "" : " (" + reason + ")") + ": " + damage + " (" + (opponentLife - damage) + ")");
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
     * @param types    card type(s)
     */
    public Card move(String cardName, Area from, Area to, Side side, CardType... types) {
        log("- move [" + cardName + "] from " + from + " to " + (side == Side.top ? "" : "bottom of ") + to);
        return _move(cardName, from, to, side, types);
    }

    /**
     * Moves the given card from the given origin area to the top of the given target area
     *
     * @param cardName name of the card to move
     * @param from     origin area
     * @param to       target area
     * @param types    card type(s)
     */
    public Card move(String cardName, Area from, Area to, CardType... types) {
        return move(cardName, from, to, Side.top, types);
    }

    /**
     * Cast the given spell
     *
     * @param cardName spell card name
     * @param from     origin area
     * @param to       target area
     * @param cost     mana cost
     * @param types    card type(s)
     */
    public Card cast(String cardName, Area from, Area to, Mana cost, CardType... types) {
        log("- cast [" + cardName + "]" + (from == Area.hand ? "" : " from " + from) + (to == Area.graveyard ? "" : " to " + to) + " for " + cost);
        _pay(cost);
        return _move(cardName, from, to, Side.top, types);
    }

    /**
     * Cast a permanent spell from the hand
     *
     * @param cardName spell name
     * @param cost     mana cost
     * @param types    card type(s)
     */
    protected Card castPermanent(String cardName, Mana cost, CardType... types) {
        return cast(cardName, Area.hand, Area.board, cost, types);
    }

    /**
     * Cast an artifact spell from the hand
     *
     * @param cardName spell name
     * @param cost     mana cost
     */
    public Card castArtifact(String cardName, Mana cost) {
        return castPermanent(cardName, cost, CardType.artifact);
    }

    /**
     * Cast a creature spell from the hand
     *
     * @param cardName spell name
     * @param cost     mana cost
     */
    public Card castCreature(String cardName, Mana cost) {
        return castPermanent(cardName, cost, CardType.creature);
    }

    /**
     * Cast an enchantment spell from the hand
     *
     * @param cardName spell name
     * @param cost     mana cost
     */
    public Card castEnchantment(String cardName, Mana cost) {
        return castPermanent(cardName, cost, CardType.enchantment);
    }

    /**
     * Cast a planeswalker spell from the hand
     *
     * @param cardName spell name
     * @param cost     mana cost
     */
    public Card castPlaneswalker(String cardName, Mana cost) {
        return castPermanent(cardName, cost, CardType.planeswalker);
    }

    /**
     * Cast a non-permanent spell from the hand
     *
     * @param cardName spell name
     * @param mana     cost
     * @param types    card type(s)
     */
    protected void castNonPermanent(String cardName, Mana mana, CardType... types) {
        cast(cardName, Area.hand, Area.graveyard, mana, types);
    }

    /**
     * Cast an instant spell from the hand
     *
     * @param cardName spell name
     * @param mana     cost
     */
    public void castInstant(String cardName, Mana mana) {
        castNonPermanent(cardName, mana, CardType.instant);
    }

    /**
     * Cast a sorcery spell from the hand
     *
     * @param cardName spell name
     * @param mana     cost
     */
    public void castSorcery(String cardName, Mana mana) {
        castNonPermanent(cardName, mana, CardType.sorcery);
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
     * @param card permanent card
     */
    public void sacrifice(Card card) {
        log("- sacrifice [" + card + "]");
        _move(card, Area.board, Area.graveyard, Side.top);
    }

    /**
     * Sacrifice a permanent
     *
     * @param card permanent name
     */
    public void destroy(Card card) {
        log("- destroy [" + card + "]");
        _move(card, Area.board, Area.graveyard, Side.top);
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
