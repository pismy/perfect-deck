package org.mtgpeasant.perfectdeck.common.cards;

import lombok.Builder;
import lombok.Singular;
import lombok.Value;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;


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
