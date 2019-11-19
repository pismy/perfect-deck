package org.mtgpeasant.perfectdeck.goldfish;

import lombok.Data;

import java.util.*;
import java.util.function.Predicate;

/**
 * A card with its state
 */
@Data
public class Permanent implements Cloneable {
    final String card;
    final Set<Game.CardType> types;
    boolean tapped = false;
    Boolean sickness = false;
    Set<String> tags = new HashSet<>();
    Map<String, Integer> counters = new HashMap<>();

    Permanent(String card, Set<Game.CardType> types) {
        this.card = card;
        this.types = types;
    }

    /**
     * Deep cloning
     */
    @Override
    protected Permanent clone() {
        try {
            Permanent clone = (Permanent) super.clone();
            clone.tags = (Set<String>) ((HashSet) tags).clone();
            clone.counters = (Map<String, Integer>) ((HashMap) counters).clone();
            return clone;
        } catch (CloneNotSupportedException cnse) {
            Permanent copy = new Permanent(card, types)
                    .setSickness(sickness)
                    .setTapped(tapped);
            copy.setTags((Set<String>) ((HashSet) tags).clone());
            copy.setCounters((Map<String, Integer>) ((HashMap) counters).clone());
            return copy;
        }
    }

    /**
     * Creates a card with specified name and types
     */
    public static Permanent permanent(String name, Game.CardType... types) {
        return new Permanent(name, new HashSet<>(Arrays.asList(types)));
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
    public Permanent addCounter(String name, int count) {
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
    public Permanent incrCounter(String name) {
        return addCounter(name, 1);
    }

    /**
     * Decrements the given counter by 1
     */
    public Permanent decrCounter(String name) {
        return addCounter(name, -1);
    }

    /**
     * Adds the given tag
     * <p>
     * <strong>Important</strong>: a tag starting with {@code '*'} is automatically cleaned-up at end of turn
     */
    public Permanent tag(String name) {
        tags.add(name);
        return this;
    }

    /**
     * Sets the card tapped state
     */
    public Permanent setTapped(boolean tapped) {
        this.tapped = tapped;
        return this;
    }

    /**
     * Sets the card summoning sickness
     */
    public Permanent setSickness(boolean sickness) {
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
     * temporary:  ⌚ / ⌛ / ⧖ / ⧗
     */
    public String toString() {
        StringBuilder sb = new StringBuilder();
        if (tapped) {
            sb.append("⟳"); // ↱↴🌀
        }
        if (sickness == Boolean.TRUE) {
            sb.append("\uD83C\uDF00");
        }
        sb.append(card);
        boolean first = true;
        if (!tags.isEmpty() || !counters.isEmpty()) {
            sb.append(" <");
            for (String tag : tags) {
                if (first) {
                    first = false;
                } else {
                    sb.append(", ");
                }
                sb.append("#");
                if (isTemporary(tag)) {
                    sb.append("⌛");
                    sb.append(tag.substring(1));
                } else {
                    sb.append(tag);
                }
            }
            for (Map.Entry<String, Integer> ctr : counters.entrySet()) {
                if (first) {
                    first = false;
                } else {
                    sb.append(", ");
                }
                if (isTemporary(ctr.getKey())) {
                    sb.append("⌛");
                    sb.append(ctr.getKey().substring(1));
                } else {
                    sb.append(ctr.getKey());
                }
                sb.append(": ");
                sb.append(ctr.getValue());
            }
            sb.append(">");
        }
        return sb.toString();
    }

    public static Predicate<Permanent> withName(String... names) {
        return card -> {
            for (String name : names) {
                if (name.equals(card.card)) {
                    return true;
                }
            }
            return false;
        };
    }

    /**
     * Composite filter that selects untapped creatures without summoning sickness
     */
    public static Predicate<Permanent> creaturesThatCanBeTapped() {
        return withType(Game.CardType.creature).and(untapped()).and(withoutSickness());
    }

    public static Predicate<Permanent> withType(Game.CardType type) {
        return card -> card.hasType(type);
    }

    public static Predicate<Permanent> withTag(String name) {
        return card -> card.hasTag(name);
    }

    public static Predicate<Permanent> withSickness() {
        return card -> card.sickness == Boolean.TRUE;
    }

    public static Predicate<Permanent> withoutSickness() {
        return card -> card.sickness != Boolean.TRUE;
    }

    public static Predicate<Permanent> notWithTag(String name) {
        return withTag(name).negate();
    }

    public static Predicate<Permanent> tapped(boolean tapped) {
        return card -> card.isTapped() == tapped;
    }

    public static Predicate<Permanent> tapped() {
        return tapped(true);
    }

    public static Predicate<Permanent> untapped() {
        return tapped(false);
    }
}
