package org.mtgpeasant.perfectdeck.goldfish;

import lombok.Value;
import org.mtgpeasant.perfectdeck.common.Mana;

import java.util.*;
import java.util.stream.Collectors;

import static org.mtgpeasant.perfectdeck.goldfish.Permanent.untapped;
import static org.mtgpeasant.perfectdeck.goldfish.Permanent.withName;

public interface ManaSource {
    /**
     * Returns the possible costs to activate this mana source
     */
    default Set<Mana> costs(Game game) {
        return Collections.emptySet();
    }

    /**
     * Returns the possible ammounts of mana this mana source can produce
     */
    Set<Mana> produces(Game game);

    /**
     * Makes the mana source produce the required mana at the given cost
     *
     * @param game    game
     * @param cost    cost
     * @param produce mana to produce
     */
    void doProduce(Game game, Mana cost, Mana produce);

    static List<ManaSource> getTapSources(Game game, String cardName, Mana... produceable) {
        return game.getBattlefield().stream()
                .filter(withName(cardName).and(untapped())) // TODO: filter out creatures with summoning sickness
                .map(card -> tap(card, produceable))
                .collect(Collectors.toList());
    }

    static List<ManaSource> getSacrificeSources(Game game, String cardName, Mana... produceable) {
        return game.getBattlefield().stream()
                .filter(withName(cardName).and(untapped()))
                .map(card -> sacrifice(card, produceable))
                .collect(Collectors.toList());
    }

    static List<ManaSource> getDiscardSources(Game game, String cardName, Mana... produceable) {
        return game.getHand().stream()
                .filter(card -> card.equals(cardName))
                .map(card -> discard(card, produceable))
                .collect(Collectors.toList());
    }

    /**
     * Mana source when a card is tapped (either a land or mana source such as Llanowar Elves)
     *
     * @param permanent        the card that produces mana when tapped
     * @param produceable mana that can be produced by the card when tapped
     */
    static ManaSource tap(Permanent permanent, Mana... produceable) {
        Set<Mana> mana = new HashSet<>(Arrays.asList(produceable));
        return new ManaSource() {
            @Override
            public Set<Mana> produces(Game game) {
                return mana;
            }

            @Override
            public void doProduce(Game game, Mana cost, Mana produce) {
                if (permanent.hasType(Game.CardType.land)) {
                    game.tapLandForMana(permanent, produce);
                } else {
                    game.tap(permanent);
                    game.add(produce);
                }
            }

            @Override
            public String toString() {
                return "tap [" + permanent + "]";
            }
        };
    }

    /**
     * Mana source when a card is sacrificed
     *
     * @param permanent        the card that produces mana when sacrificed
     * @param produceable mana that can be produced by the card when sacrificed
     */
    static ManaSource sacrifice(Permanent permanent, Mana... produceable) {
        Set<Mana> mana = new HashSet<>(Arrays.asList(produceable));
        return new ManaSource() {
            @Override
            public Set<Mana> produces(Game game) {
                return mana;
            }

            @Override
            public void doProduce(Game game, Mana cost, Mana produce) {
                game.sacrifice(permanent);
                game.add(produce);
            }

            @Override
            public String toString() {
                return "sacrifice [" + permanent + "]";
            }
        };
    }

    /**
     * Mana source when a card is discarded
     *
     * @param card        the card that produces mana when discarded
     * @param produceable mana that can be produced by the card when discarded
     */
    static ManaSource discard(String card, Mana... produceable) {
        Set<Mana> mana = new HashSet<>(Arrays.asList(produceable));
        return new ManaSource() {
            @Override
            public Set<Mana> produces(Game game) {
                return mana;
            }

            @Override
            public void doProduce(Game game, Mana cost, Mana produce) {
                game.discard(card);
                game.add(produce);
            }

            @Override
            public String toString() {
                return "discard [" + card + "]";
            }
        };
    }

    @Value
    class Landing implements ManaSource {
        @Value
        public static class Option {
            final String card;
            final Set<Mana> produceable;

            Option(String card, Mana... produceable) {
                this.card = card;
                this.produceable = new HashSet<>(Arrays.asList(produceable));
            }

            @Override
            public String toString() {
                return card;
            }
        }

        public static Option with(String name, Mana... produceable) {
            return new Option(name, produceable);
        }

        final Option[] options;

        private List<Option> landable(Game game) {
            // TODO: check current phase ?
            // check if we hve already landed
            if (game.isLanded()) {
                return Collections.emptyList();
            }
            // look for each land in hand
            List<Option> applicable = new ArrayList<>();
            for (Option opt : options) {
                if (game.getHand().contains(opt.card)) {
                    applicable.add(opt);
                }
            }
            return applicable;
        }

        @Override
        public Set<Mana> produces(Game game) {
            List<Option> landable = landable(game);

            if (landable.size() == 0) {
                return Collections.emptySet();
            } else if (landable.size() == 1) {
                return landable.get(0).produceable;
            } else {
                // combine all options
                HashSet<Mana> combined = new HashSet<>();
                for (Option opt : landable) {
                    combined.addAll(opt.produceable);
                }
                return combined;
            }
        }

        @Override
        public void doProduce(Game game, Mana cost, Mana produce) {
            List<Option> landable = landable(game);
            for (Option opt : landable) {
                if (opt.produceable.contains(produce)) {
                    Permanent land = game.land(opt.card);
                    game.tapLandForMana(land, produce);
                    return;
                }
            }
            throw new IllegalStateException("Could not produce " + produce + " with any of the available lands in hand");
        }

        @Override
        public String toString() {
            return "land one of " + Arrays.toString(options);
        }

    }

    /**
     * Mana source done by landing a land and using it for mana
     */
    static ManaSource landing(Landing.Option... options) {
        return new Landing(options);
    }

}
