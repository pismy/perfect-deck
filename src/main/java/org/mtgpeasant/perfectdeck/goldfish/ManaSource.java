package org.mtgpeasant.perfectdeck.goldfish;

import lombok.Value;
import org.mtgpeasant.perfectdeck.common.mana.Mana;

import java.util.*;
import java.util.stream.Collectors;

import static org.mtgpeasant.perfectdeck.goldfish.Permanent.untapped;
import static org.mtgpeasant.perfectdeck.goldfish.Permanent.withName;

public interface ManaSource {
    /**
     * Returns the cost to activate this mana source
     */
    Mana cost(Game game);

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

    static List<ManaSource> getTapSources(Game game, String cardName, Mana cost, Set<Mana> produceable) {
        return game.getBattlefield().stream()
                .filter(withName(cardName).and(untapped())) // TODO: filter out creatures with summoning sickness
                .map(card -> tap(card, cost, produceable))
                .collect(Collectors.toList());
    }

//    static List<ManaSource> getTapSources(Game game, String cardName, Set<Mana> produceable) {
//        return getTapSources(game, cardName, Mana.zero(), produceable);
//    }

    static List<ManaSource> getSacrificeSources(Game game, String cardName, Mana cost, Set<Mana> produceable) {
        return game.getBattlefield().stream()
                .filter(withName(cardName).and(untapped()))
                .map(card -> sacrifice(card, cost, produceable))
                .collect(Collectors.toList());
    }

//    static List<ManaSource> getSacrificeSources(Game game, String cardName, Set<Mana> produceable) {
//        return getSacrificeSources(game, cardName, Mana.zero(), produceable);
//    }

    static List<ManaSource> getDiscardSources(Game game, String cardName, Set<Mana> produceable) {
        return game.getHand().stream()
                .filter(card -> card.equals(cardName))
                .map(card -> discard(card, produceable))
                .collect(Collectors.toList());
    }

    static List<ManaSource> getCastSources(Game game, String cardName, Game.Area from, Game.Area to, Mana cost, Set<Mana> produceable, Game.CardType... types) {
        return game.getHand().stream()
                .filter(card -> card.equals(cardName))
                .map(card -> cast(card, from, to, cost, produceable, types))
                .collect(Collectors.toList());
    }

    static List<ManaSource> getCastSorcerySources(Game game, String cardName, Mana cost, Set<Mana> produceable) {
        return getCastSources(game, cardName, Game.Area.hand, Game.Area.graveyard, cost, produceable, Game.CardType.sorcery);
    }

    static List<ManaSource> getCastInstantSources(Game game, String cardName, Mana cost, Set<Mana> produceable) {
        return getCastSources(game, cardName, Game.Area.hand, Game.Area.graveyard, cost, produceable, Game.CardType.instant);
    }

    /**
     * Mana source when a card is tapped (either a land or mana source such as Llanowar Elves)
     *
     * @param permanent   the card that produces mana when tapped
     * @param cost        mana cost to activate the mana source
     * @param produceable all possible amounts of mana that can be produced by the card when tapped
     */
    static ManaSource tap(Permanent permanent, Mana cost, Set<Mana> produceable) {
        return new ManaSource() {
            @Override
            public Mana cost(Game game) {
                return cost;
            }

            @Override
            public Set<Mana> produces(Game game) {
                return produceable;
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
     * Mana source when a card is tapped (either a land or mana source such as Llanowar Elves)
     *
     * @param permanent   the card that produces mana when tapped
     * @param produceable all possible amounts of mana that can be produced by the card when tapped
     */
    static ManaSource tap(Permanent permanent, Set<Mana> produceable) {
        return tap(permanent, Mana.zero(), produceable);
    }

    /**
     * Mana source when a card is sacrificed
     *
     * @param permanent   the card that produces mana when sacrificed
     * @param cost        mana cost to activate the mana source
     * @param produceable all possible amounts of mana that can be produced by the card when tapped
     */
    static ManaSource sacrifice(Permanent permanent, Mana cost, Set<Mana> produceable) {
        return new ManaSource() {
            @Override
            public Mana cost(Game game) {
                return cost;
            }

            @Override
            public Set<Mana> produces(Game game) {
                return produceable;
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
     * Mana source when a card is sacrificed
     *
     * @param permanent   the card that produces mana when sacrificed
     * @param produceable all possible amounts of mana that can be produced by the card when sacrificed
     */
    static ManaSource sacrifice(Permanent permanent, Set<Mana> produceable) {
        return sacrifice(permanent, Mana.zero(), produceable);
    }

    /**
     * Mana source when a card is discarded
     *
     * @param card        the card that produces mana when discarded
     * @param produceable all possible amounts of mana that can be produced by the card when discarded
     */
    static ManaSource discard(String card, Set<Mana> produceable) {
        return new ManaSource() {
            @Override
            public Mana cost(Game game) {
                return Mana.zero();
            }

            @Override
            public Set<Mana> produces(Game game) {
                return produceable;
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

    /**
     * Mana source when a card is cast
     *
     * @param card        spell card name
     * @param from        origin area
     * @param to          target area
     * @param cost        mana cost
     * @param produceable all possible amounts of mana that can be produced by the card when cast
     * @param types       card type(s)
     */
    static ManaSource cast(String card, Game.Area from, Game.Area to, Mana cost, Set<Mana> produceable, Game.CardType... types) {
        return new ManaSource() {
            @Override
            public Mana cost(Game game) {
                return cost;
            }

            @Override
            public Set<Mana> produces(Game game) {
                return produceable;
            }

            @Override
            public void doProduce(Game game, Mana cost, Mana produce) {
                game.cast(card, from, to, cost, types);
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
                this.produceable = oneOf(produceable);
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
        public Mana cost(Game game) {
            return Mana.zero();
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
            return "land one of " + Arrays.toString(options) + " and tap";
        }
    }

    static HashSet<Mana> oneOf(Mana... manas) {
        return new HashSet<>(Arrays.asList(manas));
    }

    /**
     * Mana source done by landing a land and using it for mana
     */
    static ManaSource landing(Landing.Option... options) {
        return new Landing(options);
    }

}
