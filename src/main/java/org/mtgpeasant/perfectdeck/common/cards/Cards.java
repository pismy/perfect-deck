package org.mtgpeasant.perfectdeck.common.cards;

import java.util.*;


public class Cards extends ArrayDeque<String> {

    Cards() {}

    Cards(Collection<? extends String> collection) {
        super(collection);
    }

    public Cards clone() {
        return new Cards(this);
    }

    public Cards shuffle() {
        List<String> copy = new ArrayList<>(this);
        Collections.shuffle(copy);
        return new Cards(copy);
    }

    public String draw() {
        return removeFirst();
    }

    public Cards draw(int number) {
        Cards selected = new Cards();
        for (int i = 0; i < number && !isEmpty(); i++) {
            selected.add(this.draw());
        }
        return selected;
    }

    public String hasOne(String... cards) {
        for(String card : cards) {
            if(this.contains(card)) {
                return card;
            }
        }
        return null;
    }

    public int count(String... cards) {
        return select(cards).size();
    }

    public boolean hasAll(String... cards) {
        for(String card : cards) {
            if(!this.contains(card)) {
                return false;
            }
        }
        return true;
    }

    public Cards select(String... cards) {
        Set<String> set = new HashSet<>(Arrays.asList(cards));
        Cards selected = new Cards();
        for(String card : this) {
            if(set.contains(card)) {
                selected.add(card);
            }
        }
        return selected;
    }

    public static Cards from(String... cards) {
        return new Cards(Arrays.asList(cards));
    }

    public static Cards from(List<String> cards) {
        return new Cards(cards);
    }

    public static Cards none() {
        return new Cards();
    }
}
