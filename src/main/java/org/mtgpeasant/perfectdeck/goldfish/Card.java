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
    final List<Game.CardType> types;
    boolean tapped = false;
    Set<String> tags = new HashSet<>();
    Map<String, Integer> counters = new HashMap<>();

    public Card(String name, Game.CardType... types) {
        this.name = name;
        this.types = Arrays.asList(types);
    }

    public int getCounter(String name) {
        return counters.getOrDefault(name, 0);
    }

    public boolean hasCounter(String name) {
        return counters.get(name) != null;
    }

    public boolean hasTag(String name) {
        return tags.contains(name);
    }

    public Card addCounter(String name, int count) {
        Integer newCount = counters.getOrDefault(name, 0) + count;
        if (newCount == 0) {
            counters.remove(name);
        } else {
            counters.put(name, newCount);
        }
        return this;
    }

    public Card incrCounter(String name) {
        return addCounter(name, 1);
    }

    public Card decrCounter(String name) {
        return addCounter(name, -1);
    }

    public Card tag(String name) {
        tags.add(name);
        return this;
    }

    public Card setTapped(boolean tapped) {
        this.tapped = tapped;
        return this;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder(name);
        boolean first = true;
        if (/*tapped ||*/ !tags.isEmpty() || !counters.isEmpty()) {
            sb.append(" <");
//            if (tapped) {
//                if(first) {
//                    first = false;
//                } else {
//                    sb.append(", ");
//                }
//                sb.append("tapped");
//            }
            for (String tag : tags) {
                if(first) {
                    first = false;
                } else {
                    sb.append(", ");
                }
                sb.append("#" + tag);
            }
            for (Map.Entry<String, Integer> ctr : counters.entrySet()) {
                if(first) {
                    first = false;
                } else {
                    sb.append(", ");
                }
                sb.append(ctr.getKey()+":"+ctr.getValue());
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

    public static Predicate<Card> withType(Game.CardType type) {
        return card -> card.getTypes().contains(type);
    }

    public static Predicate<Card> withTag(String name) {
        return card -> card.hasTag(name);
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
