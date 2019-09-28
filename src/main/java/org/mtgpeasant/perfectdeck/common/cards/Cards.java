package org.mtgpeasant.perfectdeck.common.cards;

import lombok.Builder;
import lombok.Singular;
import lombok.Value;

import java.util.*;


@Builder
@Value
public class Cards {
    @Singular
    final List<String> cards;

    public String toString() {
        return cards.toString();
    }

    public int size() {
        return cards.size();
    }

    public boolean isEmpty() {
        return cards.isEmpty();
    }

    public Cards copy() {
        return new Cards(new ArrayList<>(this.cards));
    }

    public Cards shuffle() {
        Cards copy = copy();
        Collections.shuffle(copy.cards);
        return copy;
    }

    public String draw() {
        return cards.remove(0);
    }

    public Cards draw(int number) {
        CardsBuilder bld = Cards.builder();
        for (int i = 0; i < number && !isEmpty(); i++) {
            bld.card(this.draw());
        }
        return bld.build();
    }

    public boolean has(String card) {
        return cards.contains(card);
    }

    public String hasOne(String... cards) {
        for(String card : cards) {
            if(this.cards.contains(card)) {
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
            if(!this.cards.contains(card)) {
                return false;
            }
        }
        return true;
    }

    public Cards select(String... cards) {
        Set<String> set = new HashSet<>(Arrays.asList(cards));
        List<String> selected = new ArrayList<>();
        int count = 0;
        for(String card : this.cards) {
            if(set.contains(card)) {
                selected.add(card);
            }
        }
        return new Cards(selected);
    }

    public Cards remove(String card) {
        Cards copy = copy();
        copy.cards.remove(card);
        return copy;
    }

    public Cards add(String card) {
        Cards copy = copy();
        copy.cards.add(card);
        return copy;
    }

    public static Cards from(String... cards) {
        return new Cards(Arrays.asList(cards));
    }

    public static Cards from(List<String> cards) {
        return new Cards(cards);
    }

    public static Cards none() {
        return new Cards(Collections.emptyList());
    }
}
