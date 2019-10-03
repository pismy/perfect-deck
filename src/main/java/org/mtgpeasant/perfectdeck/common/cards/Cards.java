package org.mtgpeasant.perfectdeck.common.cards;

import java.util.*;


public class Cards extends ArrayDeque<String> {

    Cards() {
    }

    Cards(Collection<? extends String> collection) {
        super(collection);
    }

    /**
     * Makes a copy of this cards list
     */
    @Override
    public Cards clone() {
        return new Cards(this);
    }

    /**
     * Returns a copy of this, randomly shuffled
     */
    public Cards shuffle() {
        List<String> copy = new ArrayList<>(this);
        Collections.shuffle(copy);
        return new Cards(copy);
    }

    /**
     * Looks a given number of cards from the top
     *
     * @param number number of cards to look
     * @return looked cards
     */
    public Cards look(int number) {
        if (number > size()) {
            throw new IllegalArgumentException("Can't look more cards than size");
        }
        Cards selected = new Cards();
        int i = 0;
        Iterator<String> cards = this.iterator();
        while (i < number && cards.hasNext()) {
            selected.add(cards.next());
            i++;
        }
        return selected;
    }

    /**
     * Draws a given number of cards from the top
     *
     * @param number number of cards to draw
     * @return drawn cards
     */
    public Cards draw(int number) {
        if (number > size()) {
            throw new IllegalArgumentException("Can't draw cards than size");
        }
        Cards selected = new Cards();
        for (int i = 0; i < number; i++) {
            selected.add(removeFirst());
        }
        return selected;
    }

    /**
     * Looks for the first card matching one of the given names
     *
     * @param cards card names to look for
     * @return found card, or {@code null} if none was found
     */
    public Optional<String> findFirst(String... cards) {
        for (String card : cards) {
            if (this.contains(card)) {
                return Optional.of(card);
            }
        }
        return Optional.empty();
    }

    /**
     * Returns the number of cards matching the given names
     *
     * @param cards card names to look for
     * @return number of found cards
     */
    public int count(String... cards) {
        Set<String> set = new HashSet<>(Arrays.asList(cards));
        int count = 0;
        for (String card : this) {
            if (set.contains(card)) {
                count++;
            }
        }
        return count;
    }

    /**
     * Looks for all cards matching the given names
     *
     * @param cards card names to look for
     * @return found cards
     */
    public Cards findAll(String... cards) {
        Set<String> set = new HashSet<>(Arrays.asList(cards));
        Cards selected = new Cards();
        for (String card : this) {
            if (set.contains(card)) {
                selected.add(card);
            }
        }
        return selected;
    }

    public static Cards of(String... cards) {
        return new Cards(Arrays.asList(cards));
    }

    public static Cards of(List<String> cards) {
        return new Cards(cards);
    }

    public static Cards none() {
        return new Cards();
    }
}
