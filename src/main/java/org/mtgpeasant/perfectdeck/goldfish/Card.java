package org.mtgpeasant.perfectdeck.goldfish;

import lombok.Data;

import java.util.*;
import java.util.function.Predicate;

/**
 * A card with its state
 */
@Data
public class Card {
    final String name;
    final Set<Game.CardType> types;
    boolean tapped = false;
    Boolean sickness = false;
    Set<String> tags = new HashSet<>();
    Map<String, Integer> counters = new HashMap<>();

    Card(String name, Game.CardType... types) {
        this.name = name;
        this.types = new HashSet<>(Arrays.asList(types));
    }

    /**
     * Creates a card with specified name and types
     */
    public static Card card(String name, Game.CardType... types) {
        return new Card(name, types);
    }

    /**
     * Returns the number of counters of given name
     */
    public int getCounter(String name) {
        return counters.getOrDefault(name, 0);
    }

    /**
     * Determines whether this card has the given type
     */
    public boolean hasType(Game.CardType type) {
        return types.contains(type);
    }

    /**
     * Determines whether this card has a counter of the given name
     */
    public boolean hasCounter(String name) {
        return counters.get(name) != null;
    }

    /**
     * Determines whether this card has the given tag
     */
    public boolean hasTag(String name) {
        return tags.contains(name);
    }

    /**
     * Adds a given number of named counter
     * <p>
     * <strong>Important</strong>: a tag starting with {@code '*'} is automatically cleaned-up at end of turn
     *
     * @param name  counter name
     * @param count number of counter to add
     */
    public Card addCounter(String name, int count) {
        Integer newCount = counters.getOrDefault(name, 0) + count;
        if (newCount == 0) {
            counters.remove(name);
        } else {
            counters.put(name, newCount);
        }
        return this;
    }

    /**
     * Increments the given counter by 1
     */
    public Card incrCounter(String name) {
        return addCounter(name, 1);
    }

    /**
     * Decrements the given counter by 1
     */
    public Card decrCounter(String name) {
        return addCounter(name, -1);
    }

    /**
     * Adds the given tag
     * <p>
     * <strong>Important</strong>: a tag starting with {@code '*'} is automatically cleaned-up at end of turn
     */
    public Card tag(String name) {
        tags.add(name);
        return this;
    }

    /**
     * Sets the card tapped state
     */
    public Card setTapped(boolean tapped) {
        this.tapped = tapped;
        return this;
    }

    /**
     * Sets the card summoning sickness
     */
    public Card setSickness(boolean sickness) {
        this.sickness = sickness;
        return this;
    }

    void cleanup() {
        // tags
        for (Iterator<String> it = tags.iterator(); it.hasNext(); ) {
            if (isTemporary(it.next())) {
                it.remove();
            }
        }
        // counter
        for (Iterator<Map.Entry<String, Integer>> it = counters.entrySet().iterator(); it.hasNext(); ) {
            if (isTemporary(it.next().getKey())) {
                it.remove();
            }
        }
        // unset summoning sickness
        sickness = null;
    }

    boolean isTemporary(String name) {
        return name.startsWith("*");
    }

    /*
     * haste: ⚡
     * tap: ⟳ ↱↴
     * sickness: 🌀😵🐌🌪🤢
     */
    public String toString() {
        StringBuilder sb = new StringBuilder();
        if (tapped) {
            sb.append("⟳"); // ↱↴🌀
        }
        if (sickness == Boolean.TRUE) {
            sb.append("\uD83C\uDF00");
        }
        sb.append(name);
        boolean first = true;
        if (!tags.isEmpty() || !counters.isEmpty()) {
            sb.append(" <");
            for (String tag : tags) {
                if (first) {
                    first = false;
                } else {
                    sb.append(", ");
                }
                sb.append("#" + tag);
            }
            for (Map.Entry<String, Integer> ctr : counters.entrySet()) {
                if (first) {
                    first = false;
                } else {
                    sb.append(", ");
                }
                sb.append(ctr.getKey() + ": " + ctr.getValue());
            }
            sb.append(">");
        }
        return sb.toString();
    }

    public static Predicate<Card> withName(String... names) {
        return card -> {
            for (String name : names) {
                if (name.equals(card.name)) {
                    return true;
                }
            }
            return false;
        };
    }

    /**
     * Composite filter
     */
    public static Predicate<Card> creatureThatCanAttack() {
        return withType(Game.CardType.creature).and(untapped()).and(withoutSickness());
    }

    public static Predicate<Card> withType(Game.CardType type) {
        return card -> card.hasType(type);
    }

    public static Predicate<Card> withTag(String name) {
        return card -> card.hasTag(name);
    }

    public static Predicate<Card> withSickness() {
        return card -> card.sickness == Boolean.TRUE;
    }

    public static Predicate<Card> withoutSickness() {
        return card -> card.sickness != Boolean.TRUE;
    }

    public static Predicate<Card> notWithTag(String name) {
        return withTag(name).negate();
    }

    public static Predicate<Card> tapped(boolean tapped) {
        return card -> card.isTapped() == tapped;
    }

    public static Predicate<Card> tapped() {
        return tapped(true);
    }

    public static Predicate<Card> untapped() {
        return tapped(false);
    }
}
