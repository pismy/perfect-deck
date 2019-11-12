package org.mtgpeasant.perfectdeck.goldfish;

import lombok.Getter;
import lombok.ToString;
import org.mtgpeasant.perfectdeck.common.Mana;
import org.mtgpeasant.perfectdeck.common.cards.Cards;
import org.mtgpeasant.perfectdeck.goldfish.event.GameEvent;
import org.mtgpeasant.perfectdeck.goldfish.event.GameListener;

import java.io.PrintWriter;
import java.util.*;

import static org.mtgpeasant.perfectdeck.goldfish.Permanent.*;

@Getter
@ToString(exclude = "library")
public class Game {

    public enum Phase {
        beginning("beg"),
        first_main("m#1"),
        combat("cmb"),
        second_main("m#2"),
        ending("end");

        final String symbol;

        Phase(String symbol) {
            this.symbol = symbol;
        }
    }

    public enum Area {hand, library, battlefield, exile, graveyard}

    public enum Side {top, bottom}

    public enum CardType {artifact, creature, enchantment, instant, land, planeswalker, sorcery, token}

    // game state
    protected final boolean onThePlay;
    protected final PrintWriter logs;
    protected int mulligans = 0;
    protected int currentTurn = 0;
    protected int opponentLife = 20;
    protected int opponentPoisonCounters = 0;

    protected Cards library;
    protected Cards hand;

    protected Permanents battlefield = new Permanents();
    protected Permanents exile = new Permanents();
    protected Cards graveyard = Cards.empty();

    // turn and phase state
    protected Phase currentPhase;
    protected boolean landed = false;
    protected Mana pool = Mana.zero();
    private int damageDealtThisTurn = 0;

    // listeners
    protected Set<GameListener> listeners = new HashSet<>();


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
    protected Collection<? extends Object> area(Area area) {
        switch (area) {
            case hand:
                return hand;
            case library:
                return library;
            case battlefield:
                return battlefield;
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
        damageDealtThisTurn = 0;
        // cleanup cards state
        battlefield.forEach(Permanent::cleanup);

        // log
        currentPhase = null;
        log("=== Turn " + currentTurn + " ===");
        log("> opponent life: " + opponentLife);
        if (opponentPoisonCounters > 0) {
            log("> opponent poison counters: " + opponentPoisonCounters);
        }
        log("> hand: " + hand);
        log("> battlefield: " + battlefield);
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

    protected void _damageOpponent(int damage) {
        opponentLife -= damage;
        damageDealtThisTurn += damage;
    }

    protected Permanent _move(Object permanentOrCardName, Area from, Area to, Side side, CardType... types) {
        // remove from
        Object fromArea = area(from);
        String cardName = permanentOrCardName instanceof String ? (String) permanentOrCardName : ((Permanent) permanentOrCardName).getCard();
        if (fromArea instanceof Cards) {
            Cards cards = (Cards) fromArea;
            if (!cards.contains(cardName)) {
                throw new IllegalActionException("Can't move [" + cardName + "]: not in " + from);
            }
            cards.remove(cardName);
        } else {
            List<Permanent> permanents = (List<Permanent>) fromArea;
            Optional<Permanent> card = permanentOrCardName instanceof String ? permanents.stream().filter(withName(cardName)).findFirst() : Optional.of((Permanent) permanentOrCardName);
            if (!card.isPresent()) {
                throw new IllegalActionException("Can't move [" + cardName + "]: not in " + from);
            }
            permanents.remove(card.get());
        }

        // special: a token can't be moved
        if (permanentOrCardName instanceof Permanent && ((Permanent) permanentOrCardName).hasType(CardType.token)) {
            // trigger event
            trigger(new GameEvent.CardMovedEvent(GameEvent.Type.leave, from, permanentOrCardName));
            return null;
        } else {
            // add to
            Object toArea = area(to);
            Permanent moved = null;
            if (toArea instanceof Cards) {
                Cards cards = (Cards) toArea;
                if (side == Side.top) {
                    cards.addFirst(cardName);
                } else {
                    cards.addLast(cardName);
                }
            } else {
                List<Permanent> permanents = (List<Permanent>) toArea;
                moved = permanent(cardName, types);
                if (side == Side.top) {
                    permanents.add(0, moved);
                } else {
                    permanents.add(moved);
                }
                // set summoning sickness on creatures (by default)
                if (to == Area.battlefield && moved.hasType(CardType.creature)) {
                    moved.setSickness(true);
                }
            }
            // trigger events
            trigger(new GameEvent.CardMovedEvent(GameEvent.Type.leave, from, permanentOrCardName));
            trigger(new GameEvent.CardMovedEvent(GameEvent.Type.enter, to, moved == null ? cardName : moved));
            return moved;
        }
    }

    /**
     * Triggers a game event
     */
    public void trigger(GameEvent event) {
        listeners.forEach(listener -> listener.onEvent(event));
    }

    public void addListener(GameListener listener) {
        listeners.add(listener);
    }

    public void removeListener(GameListener listener) {
        listeners.remove(listener);
    }

    /**
     * Pay the given mana from mana pool
     *
     * @param cost mana cost
     */
    public void pay(Mana cost) {
        log("pay " + cost);
        _pay(cost);
    }

    /**
     * Adds the given mana to pool
     *
     * @param mana mana to add
     */
    public void add(Mana mana) {
        _add(mana);
        log("add " + mana + " to mana pool (" + pool + ")");
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
     * @param permanent card
     */
    public void tap(Permanent permanent) {
        log("tap [" + permanent + "]");
        if (permanent.isTapped()) {
            throw new IllegalActionException("Can't tap [" + permanent + "]: already tapped");
        }
        permanent.setTapped(true);
    }

    /**
     * Tap a land and produce mana
     *
     * @param permanent land card
     * @param mana      produced mana
     */
    public void tapLandForMana(Permanent permanent, Mana mana) {
        log("tap [" + permanent + "] and add " + mana + " to mana pool");
        if (permanent.isTapped()) {
            throw new IllegalActionException("Can't tap [" + permanent + "]: already tapped");
        }
        permanent.setTapped(true);
        _add(mana);
    }

    /**
     * Tap a creature to attack (damage the opponent)
     *
     * @param permanent creature card
     * @param strength  creature strength
     */
    public void tapForAttack(Permanent permanent, int strength) {
        log("attack with [" + permanent + "] for " + strength + " (" + (opponentLife - strength) + ")");
        if (permanent.isTapped()) {
            throw new IllegalActionException("Can't tap [" + permanent + "]: already tapped");
        }
        _damageOpponent(strength);
        permanent.setTapped(true);
    }

//    /**
//     * Untap the given card
//     *
//     * @param cardName card name
//     */
//    public void untap(String cardName) {
//        log("untap [" + cardName + "]");
//        _untap(cardName);
//    }

    /**
     * Untap all permanents
     */
    public void untapAll() {
        log("untap all");
        battlefield.stream().filter(tapped()).forEach(card -> card.setTapped(false));
    }

    /**
     * Drop the given land
     *
     * @param cardName land card name
     */
    public Permanent land(String cardName) {
        if (landed) {
            throw new IllegalActionException("Can't land [" + cardName + "]: can't land twice the same turn");
        }
        log("land [" + cardName + "]");
        Permanent land = _move(cardName, Area.hand, Area.battlefield, Side.top, CardType.land);
//        hand.remove(cardName);
//        Permanent permanent = permanent(cardName, CardType.land);
//        battlefield.add(permanent);
        landed = true;
        return land;
    }

    /**
     * Create a token
     *
     * @param name the token card name
     */
    public Permanent createToken(String name, CardType... types) {
        log("create token [" + name + "]");
        CardType[] types2 = new CardType[types.length + 1];
        types2[0] = CardType.token;
        System.arraycopy(types, 0, types2, 1, types.length);
        Permanent token = permanent(name, types2);
        battlefield.add(token);
        // set summoning sickness on creatures (by default)
        if (token.hasType(CardType.creature)) {
            token.setSickness(true);
        }
        // trigger event
        trigger(new GameEvent.CardMovedEvent(GameEvent.Type.enter, Area.battlefield, token));
        return token;
    }

    /**
     * Damage opponent
     *
     * @param damage damage amount
     */
    public void damageOpponent(int damage, String reason) {
        log("damage" + (reason == null ? "" : " (" + reason + ")") + ": " + damage + " (" + (opponentLife - damage) + ")");
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
        log("poison: " + counters + " (total: " + opponentPoisonCounters + ")");
    }

    /**
     * Shuffle the library
     */
    public void shuffleLibrary() {
        log("shuffle library");
        library = library.shuffle();
    }

    /**
     * Draw several cards from library
     *
     * @param cards number of cards to draw
     */
    public Cards draw(int cards) {
        Cards drawn = library.draw(cards);
        log("draw " + cards + ": " + drawn);
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
    public Permanent move(String cardName, Area from, Area to, Side side, CardType... types) {
        log("move [" + cardName + "] from " + from + " to " + (side == Side.top ? "" : "bottom of ") + to);
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
    public Permanent move(String cardName, Area from, Area to, CardType... types) {
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
    public Permanent cast(String cardName, Area from, Area to, Mana cost, CardType... types) {
        log("cast [" + cardName + "]" + (from == Area.hand ? "" : " from " + from) + (to == Area.graveyard ? "" : " to " + to) + " for " + cost);
        _pay(cost);
        // trigger event
        trigger(new GameEvent.SpellEvent(GameEvent.Type.cast, cardName, cost, new HashSet<>(Arrays.asList(types))));
        return _move(cardName, from, to, Side.top, types);
    }

    /**
     * Cast a permanent spell from the hand
     *
     * @param cardName spell name
     * @param cost     mana cost
     * @param types    card type(s)
     */
    protected Permanent castPermanent(String cardName, Mana cost, CardType... types) {
        return cast(cardName, Area.hand, Area.battlefield, cost, types);
    }

    /**
     * Cast an artifact spell from the hand
     *
     * @param cardName spell name
     * @param cost     mana cost
     */
    public Permanent castArtifact(String cardName, Mana cost) {
        return castPermanent(cardName, cost, CardType.artifact);
    }

    /**
     * Cast a creature spell from the hand
     *
     * @param cardName spell name
     * @param cost     mana cost
     */
    public Permanent castCreature(String cardName, Mana cost) {
        return castPermanent(cardName, cost, CardType.creature);
    }

    /**
     * Cast an enchantment spell from the hand
     *
     * @param cardName spell name
     * @param cost     mana cost
     */
    public Permanent castEnchantment(String cardName, Mana cost) {
        return castPermanent(cardName, cost, CardType.enchantment);
    }

    /**
     * Cast a planeswalker spell from the hand
     *
     * @param cardName spell name
     * @param cost     mana cost
     */
    public Permanent castPlaneswalker(String cardName, Mana cost) {
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
        log("discard [" + cardName + "]");
        _move(cardName, Area.hand, Area.graveyard, Side.top);
        // trigger event
        trigger(new GameEvent.CardEvent(GameEvent.Type.discard, cardName));
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
     * @param permanent permanent card
     */
    public void sacrifice(Permanent permanent) {
        log("sacrifice [" + permanent + "]");
        // trigger event
        trigger(new GameEvent.CardEvent(GameEvent.Type.sacrifice, permanent));
        _move(permanent, Area.battlefield, Area.graveyard, Side.top);
    }

    /**
     * Sacrifice a permanent
     *
     * @param permanent permanent name
     */
    public void destroy(Permanent permanent) {
        log("destroy [" + permanent + "]");
        // trigger event
        trigger(new GameEvent.CardEvent(GameEvent.Type.destroy, permanent));
        _move(permanent, Area.battlefield, Area.graveyard, Side.top);
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
        if (currentPhase != null) {
            logs.print(currentPhase.symbol);
            logs.print("> ");
        }
        logs.println(message);
    }
}
