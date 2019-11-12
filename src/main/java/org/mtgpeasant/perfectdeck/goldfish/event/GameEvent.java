package org.mtgpeasant.perfectdeck.goldfish.event;

import lombok.Value;
import lombok.experimental.NonFinal;
import org.mtgpeasant.perfectdeck.common.Mana;
import org.mtgpeasant.perfectdeck.goldfish.Game;
import org.mtgpeasant.perfectdeck.goldfish.Permanent;

import java.util.Set;

@Value
@NonFinal
public class GameEvent {
    public enum Type {
        cast,
        enter,
        leave,
        discard,
        sacrifice,
        destroy
    }

    private final Type type;

    @Value
    @NonFinal
    public static class SpellEvent extends GameEvent {

        private final String card;

        private final Mana cost;

        private final Set<Game.CardType> types;

        public SpellEvent(Type type, String card, Mana cost, Set<Game.CardType> types) {
            super(type);
            this.card = card;
            this.cost = cost;
            this.types = types;
        }

        /**
         * Determines whether this card has the given type
         */
        public boolean hasType(Game.CardType type) {
            return types.contains(type);
        }
    }

    @Value
    @NonFinal
    public static class CardEvent extends GameEvent {

        private final Object permanentOrCard;

        public CardEvent(Type type, Object permanentOrCard) {
            super(type);
            this.permanentOrCard = permanentOrCard;
        }

        public String getCardName() {
            return permanentOrCard instanceof String ? (String) permanentOrCard : ((Permanent) permanentOrCard).getCard();
        }

        public Permanent getPermanent() {
            return permanentOrCard instanceof Permanent ? (Permanent) permanentOrCard : null;
        }
    }

    @Value
    @NonFinal
    public static class CardMovedEvent extends CardEvent {

        private final Game.Area area;

        public CardMovedEvent(Type type, Game.Area area, Object permanentOrCard) {
            super(type, permanentOrCard);
            this.area = area;
        }
    }
}
