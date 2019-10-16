package org.mtgpeasant.decks.burn;

import lombok.ToString;
import org.mtgpeasant.perfectdeck.common.Mana;
import org.mtgpeasant.perfectdeck.common.cards.Cards;
import org.mtgpeasant.perfectdeck.common.utils.Permutations;
import org.mtgpeasant.perfectdeck.goldfish.DeckPilot;
import org.mtgpeasant.perfectdeck.goldfish.Game;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * TODO:
 * <ul>
 * <li>manage [light up the stage]</li>
 * <li>manage [ghitu lavarunner] haste</li>
 * <li>manage [forgotten cave] cycling</li>
 * <li>make [gitaxian probe] part of the turn simulation</li>
 * </ul>
 */
public class BurnDeckPilot extends DeckPilot<BurnGame> implements BurnCards {

    public static final Mana R = Mana.of("R");
    public static final Mana R1 = Mana.of("1R");
    public static final Mana R2 = Mana.of("2R");
    public static final Mana RR = Mana.of("RR");
    public static final Mana RR1 = Mana.of("1RR");

    private static String[] CREATURES = {MONASTERY_SWIFTSPEAR, THERMO_ALCHEMIST, FIREBRAND_ARCHER, KELDON_MARAUDERS, GHITU_LAVARUNNER, ORCISH_HELLRAISER, VIASHINO_PYROMANCER};
    private static String[] LANDS = {MOUNTAIN, FORGOTTEN_CAVE};

    // all cards that could contribute to a kill in the turn
    private static String[] RUSH = {MONASTERY_SWIFTSPEAR, FIREBRAND_ARCHER, KELDON_MARAUDERS, GHITU_LAVARUNNER, VIASHINO_PYROMANCER, ELECTROSTATIC_FIELD,
            RIFT_BOLT, FIREBLAST, LAVA_SPIKE, LIGHTNING_BOLT, SKEWER_THE_CRITICS, LAVA_DART, NEEDLE_DROP, CHAIN_LIGHTNING, FORKED_BOLT, SEARING_BLAZE, MAGMA_JET, VOLCANIC_FALLOUT, FLAME_RIFT};

    public BurnDeckPilot(BurnGame game) {
        super(game);
    }

    @Override
    public boolean keepHand(Cards hand) {
        int lands = hand.count(LANDS);
        switch (game.getMulligans()) {
            case 0:
                if (game.isOnThePlay()) {
                    return lands >= 2 && lands <= 4;
                } else {
                    return lands >= 1 && lands <= 4;
                }
            case 1:
                return lands >= 1 && lands <= 4;
            default:
                // always keep
                return true;
        }
    }

    @Override
    public void start() {
        putOnBottomOfLibrary(game.getMulligans());
    }

    @Override
    public void upkeepStep() {
        // decrement all time counters and apply effects
        game.counters("time").forEach(counters -> {
            if (counters.decrement() == 0) {
                switch (counters.getCard()) {
                    case KELDON_MARAUDERS:
                        game.sacrifice(KELDON_MARAUDERS);
                        game.damageOpponent(1, "keldon LTB trigger");
                        break;
                    case RIFT_BOLT:
                        game.cast(RIFT_BOLT, Game.Area.exile, Game.Area.graveyard, Mana.zero());
                        game.damageOpponent(3, null);
                        break;
                    case ORCISH_HELLRAISER:
                        // TODO: pay echo or let die ?
                        game.sacrifice(ORCISH_HELLRAISER);
                        game.damageOpponent(2, "orcish LTB trigger");
                        break;
                }
                // TODO: manage light up the stage ?
                game.getCounters().remove(counters);
            }
        });

        // trigger curses
        game.getBoard().findAll(CURSE_OF_THE_PIERCED_HEART).forEach(curse -> {
            game.damageOpponent(1, "curse trigger(s)");
        });
    }

    @Override
    public void firstMainPhase() {
        // whichever the situation, if I have a probe in hand: play it
        // TODO: maybe not always optimal. Casting a Swiftspear before would be better
        while (game.getHand().contains(GITAXIAN_PROBE)) {
            game.castNonPermanent(GITAXIAN_PROBE, Mana.zero());
            game.draw(1);
        }

        // if mountain: land
        if (game.getHand().contains(MOUNTAIN)) {
            game.land(MOUNTAIN);
        } else if (game.getHand().contains(FORGOTTEN_CAVE) && game.getBoard().count(MOUNTAIN, FORGOTTEN_CAVE) < 3) {
            game.land(FORGOTTEN_CAVE);
            game.tap(FORGOTTEN_CAVE);
        }

        // is there a way to kill opponent this turn (only from turn 3)?
        if (game.getCurrentTurn() > 2) {
            Mana potentialPool = game.getPool()
                    .plus(Mana.of(0, 0, 0, game.countUntapped(MOUNTAIN, FORGOTTEN_CAVE), 0, 0));

            int ghituStrength = game.countInGraveyard(Game.CardType.instant, Game.CardType.sorcery) >= 2 ? 2 : 1;
            int forseenCombatDamage =
                    game.countUntapped(MONASTERY_SWIFTSPEAR) * 1
                            + game.getProwessBoost()
                            + game.countUntapped(KELDON_MARAUDERS) * 3
                            + game.countUntapped(GHITU_LAVARUNNER) * ghituStrength
                            + game.countUntapped(VIASHINO_PYROMANCER) * 2
                            + game.countUntapped(FIREBRAND_ARCHER) * 2
                            + game.countUntapped(ORCISH_HELLRAISER) * 3
                            // +1 per thermo (EOT)
                            + game.countUntapped(THERMO_ALCHEMIST) * 1;

            Stream<Stream<String>> allSpellsOrderCombinations = Permutations.of(new ArrayList<>(game.getHand().findAll(RUSH)));
            Optional<TurnSimulation> optimalSpellsOrder = allSpellsOrderCombinations
                    .map(boosts -> simulate(potentialPool, boosts.collect(Collectors.toList())))
                    .sorted(Comparator.reverseOrder())
                    .findFirst();

            if (optimalSpellsOrder.isPresent() && optimalSpellsOrder.get().damage + forseenCombatDamage >= game.getOpponentLife()) {
                game.log(">>> I can kill now with: " + optimalSpellsOrder.get());
                // draw all mana I can from pool
                optimalSpellsOrder.get().playedSpells.forEach(this::cast);
                return;
            }
        }

        while (play()) {
        }
    }

    /**
     * Play the best spell in case there is no kill option this turn
     */
    private boolean play() {
        if (game.getHand().contains(FIREBRAND_ARCHER) && canPay(R1)) {
            produce(R1);
            game.castPermanent(FIREBRAND_ARCHER, R1);
            return true;
        } else if (game.getHand().contains(THERMO_ALCHEMIST) && canPay(R1)) {
            produce(R1);
            game.castPermanent(THERMO_ALCHEMIST, R1);
            return true;
        } else if (game.getHand().contains(ELECTROSTATIC_FIELD) && canPay(R1)) {
            produce(R1);
            game.castPermanent(ELECTROSTATIC_FIELD, R1);
            return true;
        } else if (game.getHand().contains(KELDON_MARAUDERS) && canPay(R1)) {
            produce(R1);
            game.castPermanent(KELDON_MARAUDERS, R1);
            game.damageOpponent(1, "keldon ETB trigger");
            // 2 vanishing counters
            game.addCounter("time", KELDON_MARAUDERS, Game.Area.board, 2);
            return true;
        } else if (game.getHand().contains(VIASHINO_PYROMANCER) && canPay(R1)) {
            produce(R1);
            game.castPermanent(VIASHINO_PYROMANCER, R1);
            game.damageOpponent(2, "viashino ETB trigger");
            return true;
        } else if (game.getHand().contains(ORCISH_HELLRAISER) && canPay(R1)) {
            produce(R1);
            game.castPermanent(ORCISH_HELLRAISER, R1);
            // time counter for echo
            game.addCounter("time", ORCISH_HELLRAISER, Game.Area.board, 1);
            return true;
        } else if (game.getHand().contains(CURSE_OF_THE_PIERCED_HEART) && canPay(R1)) {
            produce(R1);
            game.castPermanent(CURSE_OF_THE_PIERCED_HEART, R1);
            return true;
        } else if (game.getHand().contains(MONASTERY_SWIFTSPEAR) && canPay(R)) {
            produce(R);
            game.castPermanent(MONASTERY_SWIFTSPEAR, R);
            return true;
        } else if (game.getHand().contains(GHITU_LAVARUNNER) && canPay(R) && game.countInGraveyard(Game.CardType.sorcery, Game.CardType.instant) >= 2) {
            // play with this priority if haste
            produce(R);
            game.castPermanent(GHITU_LAVARUNNER, R);
            return true;
        } else if (game.getHand().contains(FLAME_RIFT) && canPay(R1)) {
            produce(R1);
            game.castNonPermanent(FLAME_RIFT, R1);
            game.damageOpponent(4, null);
            return true;
        } else if (game.isLanded() && game.getHand().contains(SEARING_BLAZE) && canPay(RR)) {
            // only play if landed (landfall)
            produce(RR);
            game.castNonPermanent(SEARING_BLAZE, RR);
            game.damageOpponent(3, null);
            return true;
        } else if (game.getDamageDealtThisTurn() > 0 && game.getHand().contains(SKEWER_THE_CRITICS) && canPay(R)) {
            produce(R);
            game.castNonPermanent(SKEWER_THE_CRITICS, R);
            game.damageOpponent(3, null);
            return true;
        } else if (game.getHand().contains(RIFT_BOLT) && canPay(R)) {
            // suspend
            produce(R);
            game.pay(R);
            game.move(RIFT_BOLT, Game.Area.hand, Game.Area.exile);
            game.addCounter("time", RIFT_BOLT, Game.Area.exile, 1);
            return true;
        } else if (game.getDamageDealtThisTurn() > 0 && game.getHand().contains(NEEDLE_DROP) && canPay(R)) {
            produce(R);
            game.castNonPermanent(NEEDLE_DROP, R);
            game.damageOpponent(1, null);
            game.draw(1);
            return true;
        } else if (game.getHand().contains(LIGHTNING_BOLT) && canPay(R)) {
            produce(R);
            game.castNonPermanent(LIGHTNING_BOLT, R);
            game.damageOpponent(3, null);
            return true;
        } else if (game.getHand().contains(CHAIN_LIGHTNING) && canPay(R)) {
            produce(R);
            game.castNonPermanent(CHAIN_LIGHTNING, R);
            game.damageOpponent(3, null);
            return true;
        } else if (game.getHand().contains(LAVA_SPIKE) && canPay(R)) {
            produce(R);
            game.castNonPermanent(LAVA_SPIKE, R);
            game.damageOpponent(3, null);
            return true;
        } else if (game.getHand().contains(FORKED_BOLT) && canPay(R)) {
            produce(R);
            game.castNonPermanent(FORKED_BOLT, R);
            game.damageOpponent(2, null);
            return true;
        } else if (game.getHand().contains(LAVA_DART) && canPay(R)) {
            produce(R);
            game.castNonPermanent(LAVA_DART, R);
            game.damageOpponent(1, null);
            game.draw(1);
            return true;
        } else if (game.getHand().contains(GHITU_LAVARUNNER) && canPay(R)) {
            // play with low priority if not haste
            produce(R);
            game.castPermanent(GHITU_LAVARUNNER, R);
            return true;
        } else {
            return false;
        }
    }

    @Override
    public void combatPhase() {
        game.getUntapped(MONASTERY_SWIFTSPEAR).forEach(card -> game.tapForAttack(card, 1));
        if (game.getProwessBoost() > 0) {
            game.damageOpponent(game.getProwessBoost(), "prowess");
        }

        game.getUntapped(KELDON_MARAUDERS).forEach(card -> game.tapForAttack(card, 3));

        int ghituStrength = game.countInGraveyard(Game.CardType.instant, Game.CardType.sorcery) >= 2 ? 2 : 1;
        game.getUntapped(GHITU_LAVARUNNER).forEach(card -> game.tapForAttack(card, ghituStrength));

        game.getUntapped(VIASHINO_PYROMANCER).forEach(card -> game.tapForAttack(card, 2));

        game.getUntapped(FIREBRAND_ARCHER).forEach(card -> game.tapForAttack(card, 2));

        game.getUntapped(ORCISH_HELLRAISER).forEach(card -> game.tapForAttack(card, 3));
    }

    @Override
    public void secondMainPhase() {
        // what is the best order to play my spells ?
        if (game.getCurrentTurn() > 2) {
            Mana potentialPool = game.getPool()
                    .plus(Mana.of(0, 0, 0, game.countUntapped(MOUNTAIN, FORGOTTEN_CAVE), 0, 0));

            Stream<Stream<String>> allSpellsOrderCombinations = Permutations.of(new ArrayList<>(game.getHand().findAll(RUSH)));
            Optional<TurnSimulation> optimalSpellsOrder = allSpellsOrderCombinations
                    .map(boosts -> simulate(potentialPool, boosts.collect(Collectors.toList())))
                    .sorted(Comparator.reverseOrder())
                    .findFirst();

            int forseenDamage =
                    // +1 per thermo (EOT)
                    +game.countUntapped(THERMO_ALCHEMIST) * 1;

            if (optimalSpellsOrder.isPresent() && optimalSpellsOrder.get().damage + forseenDamage >= game.getOpponentLife()) {
                game.log(">>> I can kill now with: " + optimalSpellsOrder.get());
                // draw all mana I can from pool
                optimalSpellsOrder.get().playedSpells.forEach(this::cast);
                return;
            }
        }

        while (play()) {
        }

        // use untapped thermo a last time
        game.getUntapped(THERMO_ALCHEMIST).forEach(card -> {
            game.tap(card);
            game.damageOpponent(1, "thermo");
        });
    }

    @ToString
    class TurnSimulation implements Comparable<TurnSimulation> {
        List<String> playedSpells = new ArrayList<>();

        int damage = 0;

        int thermosOnBoard = game.getBoard().count(THERMO_ALCHEMIST);
        int archersOnBoard = game.getBoard().count(FIREBRAND_ARCHER);
        int fieldsOnBoard = game.getBoard().count(ELECTROSTATIC_FIELD);
        int swiftspearsOnBoard = game.getBoard().count(MONASTERY_SWIFTSPEAR);
        int moutainsOnBoard = game.getBoard().count(MOUNTAIN);
        int dartsInGy = game.getGraveyard().count(LAVA_DART);

        void damage(int damage) {
            this.damage += damage;
        }

        boolean haveSpectacle() {
            return damageDealtThisTurn() > 0;
        }

        int damageDealtThisTurn() {
            return game.getDamageDealtThisTurn() + damage;
        }

        void damageWithNonPermanent(int damage) {
            damage(damage
                    + thermosOnBoard
                    + fieldsOnBoard
                    + archersOnBoard
                    + swiftspearsOnBoard);
        }

        @Override
        public int compareTo(TurnSimulation other) {
            return damage - other.damage;
        }

        public boolean maybeCastDartFlashback() {
            if (dartsInGy > 0 && moutainsOnBoard > 0) {
                playedSpells.add(LAVA_DART_FB);
                dartsInGy--;
                moutainsOnBoard--;
                damageWithNonPermanent(1);
                return true;
            }
            return false;
        }

    }

    private TurnSimulation simulate(Mana potentialPool, List<String> spells) {
        TurnSimulation sim = new TurnSimulation();
        for (String spell : spells) {
            switch (spell) {
                // CREATURES
                case MONASTERY_SWIFTSPEAR:
                    if (potentialPool.contains(R)) {
                        potentialPool = potentialPool.minus(R);
                        sim.playedSpells.add(spell);
                        sim.swiftspearsOnBoard += 1;
                        sim.damage(1); // haste -> additional combat damage
                    }
                    break;
                case KELDON_MARAUDERS:
                    if (potentialPool.contains(R1)) {
                        potentialPool = potentialPool.minus(R1);
                        sim.playedSpells.add(spell);
                        sim.damage(1);
                    }
                    break;
                case GHITU_LAVARUNNER:
                    if (potentialPool.contains(R)) {
                        potentialPool = potentialPool.minus(R);
                        sim.playedSpells.add(spell);
                        // haste and +1/+0
                        // TODO
                        sim.damage(2); // haste -> additional combat damage
                    }
                    break;
                case VIASHINO_PYROMANCER:
                    if (potentialPool.contains(R1)) {
                        potentialPool = potentialPool.minus(R1);
                        sim.playedSpells.add(spell);
                        sim.damage(2);
                    }
                    break;
                case FIREBRAND_ARCHER:
                    if (potentialPool.contains(R1)) {
                        potentialPool = potentialPool.minus(R1);
                        sim.playedSpells.add(spell);
                        sim.archersOnBoard++;
                    }
                    break;
                case ELECTROSTATIC_FIELD:
                    if (potentialPool.contains(R1)) {
                        potentialPool = potentialPool.minus(R1);
                        sim.playedSpells.add(spell);
                        sim.fieldsOnBoard++;
                    }
                    break;
                // BURN
                case FIREBLAST:
                    if (sim.moutainsOnBoard >= 2) {
                        sim.moutainsOnBoard -= 2;
                        sim.playedSpells.add(spell);
                        sim.damageWithNonPermanent(4);
                    }
                    break;
                case LAVA_SPIKE:
                case CHAIN_LIGHTNING:
                case LIGHTNING_BOLT:
                    if (potentialPool.contains(R)) {
                        potentialPool = potentialPool.minus(R);
                        sim.playedSpells.add(spell);
                        sim.damageWithNonPermanent(3);
                    }
                    break;
                case SKEWER_THE_CRITICS:
                    if (!sim.haveSpectacle() && potentialPool.contains(R)) {
                        // can I cast a dart from GY to have spectacle ?
                        sim.maybeCastDartFlashback();
                    }
                    if (sim.haveSpectacle() && potentialPool.contains(R)) {
                        potentialPool = potentialPool.minus(R);
                        sim.playedSpells.add(spell);
                        sim.damageWithNonPermanent(3);
                    } else if (potentialPool.contains(R2)) {
                        potentialPool = potentialPool.minus(R2);
                        sim.playedSpells.add(spell);
                        sim.damageWithNonPermanent(3);
                    }
                    break;
                case LAVA_DART:
                    // TODO: simulate play from GY
                    if (potentialPool.contains(R)) {
                        potentialPool = potentialPool.minus(R);
                        sim.playedSpells.add(spell);
                        sim.dartsInGy++;
                        sim.damageWithNonPermanent(1);
                    }
                    break;
                case NEEDLE_DROP:
                    if (sim.damageDealtThisTurn() == 0 && potentialPool.contains(R)) {
                        // can I cast a dart from GY to have spectacle ?
                        sim.maybeCastDartFlashback();
                    }
                    if (sim.damageDealtThisTurn() > 0 && potentialPool.contains(R)) {
                        potentialPool = potentialPool.minus(R);
                        sim.playedSpells.add(spell);
                        sim.damageWithNonPermanent(1);
                    }
                    break;
                case FORKED_BOLT:
                    if (potentialPool.contains(R)) {
                        potentialPool = potentialPool.minus(R);
                        sim.playedSpells.add(spell);
                        sim.damageWithNonPermanent(2);
                    }
                    break;
                case SEARING_BLAZE:
                    if (potentialPool.contains(RR)) {
                        potentialPool = potentialPool.minus(RR);
                        sim.playedSpells.add(spell);
                        sim.damageWithNonPermanent((game.isLanded() ? 3 : 1));
                    }
                    break;
                case MAGMA_JET:
                    if (potentialPool.contains(R1)) {
                        potentialPool = potentialPool.minus(R1);
                        sim.playedSpells.add(spell);
                        sim.damageWithNonPermanent(2);
                    }
                    break;
                case VOLCANIC_FALLOUT:
                    if (potentialPool.contains(RR1)) {
                        potentialPool = potentialPool.minus(RR1);
                        sim.playedSpells.add(spell);
                        sim.damageWithNonPermanent(2);
                    }
                    break;
                case FLAME_RIFT:
                    if (potentialPool.contains(R1)) {
                        potentialPool = potentialPool.minus(R1);
                        sim.playedSpells.add(spell);
                        sim.damageWithNonPermanent(4);
                    }
                    break;
                case RIFT_BOLT:
                    if (potentialPool.contains(R2)) {
                        potentialPool = potentialPool.minus(R2);
                        sim.playedSpells.add(spell);
                        sim.damageWithNonPermanent(3);
                    }
                    break;
            }
        }

        // simulate playing darts from graveyard
        int playableDarts = Math.min(sim.dartsInGy, sim.moutainsOnBoard);
        if (playableDarts > 0) {
            for (int i = 0; i < playableDarts; i++) {
                sim.playedSpells.add(LAVA_DART_FB);
                sim.dartsInGy--;
                sim.moutainsOnBoard--;
                sim.damageWithNonPermanent(1);
            }
        }

        return sim;
    }

    boolean cast(String card) {
        switch (card) {
            // CREATURES
            case MONASTERY_SWIFTSPEAR:
                if (canPay(R)) {
                    produce(R);
                    game.castPermanent(card, R);
                    return true;
                }
                break;
            case KELDON_MARAUDERS:
                if (canPay(R1)) {
                    produce(R1);
                    game.castPermanent(card, R1);
                    game.damageOpponent(1, "keldon ETB trigger");
                    // 2 vanishing counters
                    game.addCounter("time", KELDON_MARAUDERS, Game.Area.board, 2);
                    return true;
                }
                break;
            case GHITU_LAVARUNNER:
                if (canPay(R)) {
                    produce(R);
                    game.castPermanent(card, R);
                    return true;
                }
                break;
            case VIASHINO_PYROMANCER:
                if (canPay(R1)) {
                    produce(R1);
                    game.castPermanent(card, R1);
                    game.damageOpponent(2, "viashino ETB trigger");
                    return true;
                }
                break;
            case FIREBRAND_ARCHER:
                if (canPay(R1)) {
                    produce(R1);
                    game.castPermanent(card, R1);
                    return true;
                }
                break;
            case ELECTROSTATIC_FIELD:
                if (canPay(R1)) {
                    produce(R1);
                    game.castPermanent(card, R1);
                    return true;
                }
                break;
            // BURN
            case FIREBLAST:
                if (game.getBoard().count(MOUNTAIN) >= 2) {
                    // add R to pool before sacrifice
                    while (game.getTapped().count(MOUNTAIN) < 2) {
                        game.tapLandForMana(MOUNTAIN, R);
                    }
                    game.sacrifice(MOUNTAIN);
                    game.sacrifice(MOUNTAIN);
                    game.castNonPermanent(card, Mana.zero());
                    game.damageOpponent(4, null);
                    return true;
                }
                break;
            case LAVA_DART_FB:
                if (game.getBoard().count(MOUNTAIN) >= 1) {
                    // add R to pool before sacrifice
                    while (game.getTapped().count(MOUNTAIN) < 1) {
                        game.tapLandForMana(MOUNTAIN, R);
                    }
                    game.sacrifice(MOUNTAIN);
                    game.cast(LAVA_DART, Game.Area.graveyard, Game.Area.exile, Mana.zero());
                    game.damageOpponent(1, null);
                    return true;
                }
                break;
            case LAVA_SPIKE:
            case CHAIN_LIGHTNING:
            case LIGHTNING_BOLT:
                if (canPay(R)) {
                    produce(R);
                    game.castNonPermanent(card, R);
                    game.damageOpponent(3, null);
                    return true;
                }
                break;
            case SKEWER_THE_CRITICS:
                if (game.getDamageDealtThisTurn() > 0 && canPay(R)) {
                    produce(R);
                    game.castNonPermanent(card, R);
                    game.damageOpponent(3, null);
                    return true;
                } else if (canPay(R2)) {
                    produce(R2);
                    game.castNonPermanent(card, R);
                    game.damageOpponent(3, null);
                    return true;
                }
                break;
            case LAVA_DART:
                if (canPay(R)) {
                    produce(R);
                    game.castNonPermanent(card, R);
                    game.damageOpponent(1, null);
                    return true;
                }
                break;
            case NEEDLE_DROP:
                if (game.getDamageDealtThisTurn() > 0 && canPay(R)) {
                    produce(R);
                    game.castNonPermanent(card, R);
                    game.damageOpponent(1, null);
                    game.draw(1); // TODO: replan a turn ?
                    return true;
                }
                break;
            case FORKED_BOLT:
                if (canPay(R)) {
                    produce(R);
                    game.castNonPermanent(card, R);
                    game.damageOpponent(2, null);
                    return true;
                }
                break;
            case SEARING_BLAZE:
                if (canPay(RR)) {
                    produce(RR);
                    game.castNonPermanent(card, RR);
                    int damage = (game.isLanded() ? 3 : 1);
                    game.damageOpponent(damage, null);
                    return true;
                }
                break;
            case MAGMA_JET:
                if (canPay(R1)) {
                    produce(R1);
                    game.castNonPermanent(card, R1);
                    game.damageOpponent(2, null);
                    return true;
                }
                break;
            case VOLCANIC_FALLOUT:
                if (canPay(RR1)) {
                    produce(RR1);
                    game.castNonPermanent(card, RR1);
                    game.damageOpponent(2, null);
                    return true;
                }
                break;
            case FLAME_RIFT:
                if (canPay(R1)) {
                    produce(R1);
                    game.castNonPermanent(card, R1);
                    game.damageOpponent(4, null);
                    return true;
                }
                break;
            case RIFT_BOLT:
                if (canPay(R2)) {
                    produce(R2);
                    game.castNonPermanent(card, R2);
                    game.damageOpponent(3, null);
                    return true;
                }
                break;
        }
        game.log("oops, can't play [" + card + "]");
        return false;
    }

    @Override
    public void endingPhase() {
        if (game.getHand().size() > 7) {
            discard(game.getHand().size() - 7);
        }
    }

    void putOnBottomOfLibrary(int number) {
        for (int i = 0; i < number; i++) {
            // discard extra lands
            if (game.getHand().count(LANDS) > 2 && game.putOnBottomOfLibraryOneOf(FORGOTTEN_CAVE, MOUNTAIN).isPresent()) {
                continue;
            }
            // discard extra creatures
            if (game.getHand().count(CREATURES) > 2 && game.putOnBottomOfLibraryOneOf(ORCISH_HELLRAISER, KELDON_MARAUDERS, VIASHINO_PYROMANCER, THERMO_ALCHEMIST, ELECTROSTATIC_FIELD, FIREBRAND_ARCHER, GHITU_LAVARUNNER, MONASTERY_SWIFTSPEAR).isPresent()) {
                continue;
            }
            if (game.putOnBottomOfLibraryOneOf(SEARING_BLAZE, MAGMA_JET, VOLCANIC_FALLOUT, FLAME_RIFT, CURSE_OF_THE_PIERCED_HEART, GITAXIAN_PROBE, LIGHT_UP_THE_STAGE).isPresent()) {
                continue;
            }
            // TODO: choose better
            game.putOnBottomOfLibrary(game.getHand().getFirst());
        }
    }

    void discard(int number) {
        for (int i = 0; i < number; i++) {
            // discard extra lands
            if (game.getBoard().count(LANDS) + game.getHand().count(LANDS) > 3 && game.discardOneOf(FORGOTTEN_CAVE, MOUNTAIN).isPresent()) {
                continue;
            }
            // discard extra creatures
            if (game.getHand().count(CREATURES) + game.getHand().count(CREATURES) > 2 && game.discardOneOf(ORCISH_HELLRAISER, KELDON_MARAUDERS, VIASHINO_PYROMANCER, THERMO_ALCHEMIST, ELECTROSTATIC_FIELD, FIREBRAND_ARCHER, GHITU_LAVARUNNER, MONASTERY_SWIFTSPEAR).isPresent()) {
                continue;
            }
            if (game.discardOneOf(SEARING_BLAZE, MAGMA_JET, VOLCANIC_FALLOUT, FLAME_RIFT, CURSE_OF_THE_PIERCED_HEART, GITAXIAN_PROBE, LIGHT_UP_THE_STAGE).isPresent()) {
                continue;
            }
            // TODO: choose better
            game.discard(game.getHand().getFirst());
        }
    }

    boolean canPay(Mana cost) {
        // potential mana pool is current pool + untapped lands + petals on board
        Mana potentialPool = game.getPool()
                .plus(Mana.of(0, 0, 0, game.countUntapped(LANDS), 0, 0));
        return potentialPool.contains(cost);
    }

    void produce(Mana cost) {
        while (!game.canPay(cost)) {
            Optional<String> producer = game.findFirstUntapped(LANDS);
            if (producer.isPresent()) {
                game.tapLandForMana(producer.get(), R);
            } else {
                // can't produce !!!
                return;
            }
        }
    }
}
