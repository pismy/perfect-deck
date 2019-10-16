package org.mtgpeasant.perfectdeck.goldfish;

import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class Counters {
    final String type;
    final String card;
    @Builder.Default
    final Game.Area area = Game.Area.board;
    int number;

    public int decrement() {
        return --number;
    }

    public int increment() {
        return ++number;
    }

    public int add(int counters) {
        this.number += counters;
        return this.number;
    }

}
