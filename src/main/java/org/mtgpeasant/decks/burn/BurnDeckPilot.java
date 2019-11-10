package org.mtgpeasant.decks.burn;

import lombok.ToString;
import org.mtgpeasant.perfectdeck.common.Mana;
import org.mtgpeasant.perfectdeck.common.cards.Cards;
import org.mtgpeasant.perfectdeck.common.utils.Permutations;
import org.mtgpeasant.perfectdeck.goldfish.Card;
import org.mtgpeasant.perfectdeck.goldfish.DeckPilot;
import org.mtgpeasant.perfectdeck.goldfish.Game;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.mtgpeasant.perfectdeck.goldfish.Card.*;

/**
 * TODO:
 * <ul>
 * <li>manage [light up the stage]</li>
 * <li>manage [forgotten cave] cycling</li>
 * <li>manage [magma jet] scry</li>
 * <li>manage [seal of fire]</li>
 * <li>make [gitaxian probe] part of the turn simulation</li>
 * </ul>
 */
public class BurnDeckPilot extends DeckPilot<BurnGame> implements BurnCards {

    public static final Mana R = Mana.of("R");
    public static final Mana R1 = Mana.of("1R");
    public static final Mana R2 = Mana.of("2R");
    public static final Mana RR = Mana.of("RR");
    public static final Mana RR1 = Mana.of("1RR");

    private static String[] CREATURES = {MONASTERY_SWIFTSPEAR, THERMO_ALCHEMIST, ELECTROSTATIC_FIELD, FIREBRAND_ARCHER, KELDON_MARAUDERS, GHITU_LAVARUNNER, ORCISH_HELLRAISER, VIASHINO_PYROMANCER};
    private static String[] LANDS = {MOUNTAIN, FORGOTTEN_CAVE};

    // all cards that could contribute to a kill in the turn
    private static String[] RUSH = {MONASTERY_SWIFTSPEAR, FIREBRAND_ARCHER, KELDON_MARAUDERS, GHITU_LAVARUNNER, VIASHINO_PYROMANCER, ELECTROSTATIC_FIELD,
            RIFT_BOLT, FIREBLAST, LAVA_SPIKE, LIGHTNING_BOLT, SKEWER_THE_CRITICS, LAVA_DART, NEEDLE_DROP, CHAIN_LIGHTNING, FORKED_BOLT, SEARING_BLAZE, MAGMA_JET, VOLCANIC_FALLOUT, FLAME_RIFT, SEAL_OF_FIRE};

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
        game.find(withName(KELDON_MARAUDERS)).forEach(card -> {
            card.decrCounter("vanishing");
            if (card.getCounter("vanishing") == 0) {
                game.sacrifice(card);
                game.damageOpponent(1, "keldon LTB trigger");
            }
        });
        game.find(withName(ORCISH_HELLRAISER)).forEach(card -> {
            card.decrCounter("echo");
            if (card.getCounter("echo") == 0) {
                // TODO: pay echo or let die ?
                game.sacrifice(card);
                game.damageOpponent(2, "orcish LTB trigger");
            }
        });
        game.getExile().stream().filter(withName(RIFT_BOLT)).collect(Collectors.toList()).forEach(card -> {
            if (card.getCounter("suspend") == 1) {
                game.cast(RIFT_BOLT, Game.Area.exile, Game.Area.graveyard, Mana.zero());
                game.damageOpponent(3, null);
            }
        });
        // TODO: manage light up the stage ?

        // trigger curses
        game.find(withName(CURSE_OF_THE_PIERCED_HEART)).forEach(curse -> {
            game.damageOpponent(1, "curse trigger(s)");
        });
    }

    /**
     * Override draw step to manage thunderous wrath (with miracle cost)
     */
    @Override
    public void drawStep() {
        Cards drawn = game.draw(1);
        // pay thunderous wrath miracle cost
        if (drawn.getFirst().equals(THUNDEROUS_WRATH) && canPay(R)) {
            produce(R);
            game.castInstant(THUNDEROUS_WRATH, R);
            game.damageOpponent(5, "a miracle!");
        }
    }

    @Override
    public void firstMainPhase() {
        // start by playing all probes
        // TODO: maybe not always optimal. Casting a Swiftspear before could be better
        while (playOneOf(false, GITAXIAN_PROBE).isPresent()) {
        }

        // then land
        playOneOf(false, MOUNTAIN, FORGOTTEN_CAVE);

        // is there a way to kill opponent this turn (only from turn 3)?
        if (game.getCurrentTurn() > 2) {
            List<Card> creatures = game.find(creatureThatCanAttack().and(notWithTag(DEFENDER_SUBTYPE)));
            int forseenDamage = creatures.stream().mapToInt(this::strength).sum()
                    // +1 per thermo (EOT)
                    + game.count(withName(THERMO_ALCHEMIST).and(untapped())) * 1;

            if (game.getOpponentLife() > forseenDamage) {
                Mana potentialPool = game.getPool()
                        .plus(Mana.of(0, 0, 0, game.count(withName(MOUNTAIN, FORGOTTEN_CAVE).and(untapped())), 0, 0));

                Stream<Stream<String>> allSpellsOrderCombinations = Permutations.of(new ArrayList<>(game.getHand().findAll(RUSH)));
                Optional<TurnSimulation> optimalSpellsOrder = allSpellsOrderCombinations
                        .map(boosts -> simulate(potentialPool, boosts.collect(Collectors.toList())))
                        .sorted(Comparator.reverseOrder())
                        .findFirst();

                if (optimalSpellsOrder.isPresent() && optimalSpellsOrder.get().damage + forseenDamage >= game.getOpponentLife()) {
                    game.log(">>> I can kill now with: " + optimalSpellsOrder.get());
                    // sacrifice all seals
                    game.find(withName(SEAL_OF_FIRE)).forEach(seal -> {
                        game.sacrifice(seal);
                        game.damageOpponent(2);
                    });

                    // then play spells
                    optimalSpellsOrder.get().playedSpells.forEach(card -> play(true, card));
                    return;
                }
            }
        }

        while (playBestCard()) {
        }
    }

    /**
     * Play the best card in case there is no kill option this turn
     */
    private boolean playBestCard() {
        boolean ghituHasHaste = game.countInGraveyard(Game.CardType.sorcery, Game.CardType.instant) >= 2;
        return playOneOf(false,
                MOUNTAIN,
                MONASTERY_SWIFTSPEAR,
                GITAXIAN_PROBE,
                NEEDLE_DROP,
                FORGOTTEN_CAVE,
                KILN_FIEND,
                FIREBRAND_ARCHER,
                THERMO_ALCHEMIST,
                ELECTROSTATIC_FIELD,
                KELDON_MARAUDERS,
                VIASHINO_PYROMANCER,
                ORCISH_HELLRAISER,
                CURSE_OF_THE_PIERCED_HEART,
                ghituHasHaste ? GHITU_LAVARUNNER : "_",
                FLAME_RIFT,
                game.isLanded() ? SEARING_BLAZE : "_",
                game.getDamageDealtThisTurn() > 0 ? SKEWER_THE_CRITICS : "_",
                RIFT_BOLT,
                CHAIN_LIGHTNING,
                LAVA_SPIKE,
                LIGHTNING_BOLT,
                FORKED_BOLT,
                SEAL_OF_FIRE,
                MAGMA_JET,
                VOLCANIC_FALLOUT,
                GHITU_LAVARUNNER,
                SKEWER_THE_CRITICS
        ).isPresent();
    }

    @Override
    public void combatPhase() {
        List<Card> creatures = game.find(creatureThatCanAttack().and(notWithTag(DEFENDER_SUBTYPE)));
        creatures.forEach(card -> game.tapForAttack(card, strength(card)));
    }

    @Override
    public void secondMainPhase() {
        // what is the best order to play my spells ?
        if (game.getCurrentTurn() > 2) {
            Mana potentialPool = game.getPool()
                    .plus(Mana.of(0, 0, 0, game.count(withName(MOUNTAIN, FORGOTTEN_CAVE).and(untapped())), 0, 0));

            int forseenDamage =
                    // +1 per thermo (EOT)
                    +game.count(withName(THERMO_ALCHEMIST).and(untapped())) * 1;

            if (game.getOpponentLife() > forseenDamage) {
                Stream<Stream<String>> allSpellsOrderCombinations = Permutations.of(new ArrayList<>(game.getHand().findAll(RUSH)));
                Optional<TurnSimulation> optimalSpellsOrder = allSpellsOrderCombinations
                        .map(boosts -> simulate(potentialPool, boosts.collect(Collectors.toList())))
                        .sorted(Comparator.reverseOrder())
                        .findFirst();


                if (optimalSpellsOrder.isPresent() && optimalSpellsOrder.get().damage + forseenDamage >= game.getOpponentLife()) {
                    game.log(">>> I can kill now with: " + optimalSpellsOrder.get());
                    // draw all mana I can from pool
                    optimalSpellsOrder.get().playedSpells.forEach(card -> play(true, card));
                    return;
                }
            }
        }

        while (playBestCard()) {
        }

        // use untapped thermo a last time
        game.find(withName(THERMO_ALCHEMIST).and(untapped()).and(withoutSickness())).forEach(card -> {
            game.tap(card);
            game.damageOpponent(1, "thermo");
        });
    }

    @ToString
    class TurnSimulation implements Comparable<TurnSimulation> {
        List<String> playedSpells = new ArrayList<>();

        int damage = 0;

        int thermosOnBoard = game.count(withName(THERMO_ALCHEMIST).and(untapped()));
        int kilnOnBoard = game.count(withName(KILN_FIEND).and(untapped()));
        int archersOnBoard = game.count(withName(FIREBRAND_ARCHER));
        int fieldsOnBoard = game.count(withName(ELECTROSTATIC_FIELD));
        int swiftspearsOnBoard = game.count(withName(MONASTERY_SWIFTSPEAR));
        int moutainsOnBoard = game.count(withName(MOUNTAIN));
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
                    + swiftspearsOnBoard
                    + kilnOnBoard * 3);
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
        // sacrifice all seals
        int sealsOnBoard = game.count(withName(SEAL_OF_FIRE));
        sim.damage(sealsOnBoard * 2);

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
                case SEAL_OF_FIRE:
                    if (potentialPool.contains(R)) {
                        potentialPool = potentialPool.minus(R);
                        sim.playedSpells.add(spell);
                        sim.damageWithNonPermanent(2); // TODO: not exactly that...
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

    private int strength(Card creature) {
        // strength is base strength + +1/1 counters + temporary boosts + 2 * rancors
        return baseStrength(creature)
                + creature.getCounters().getOrDefault(PERM_BOOST, 0)
                + creature.getCounters().getOrDefault(TURN_BOOST, 0);
    }

    private int baseStrength(Card creature) {
        switch (creature.getName()) {
            case GHITU_LAVARUNNER:
                return game.countInGraveyard(Game.CardType.instant, Game.CardType.sorcery) >= 2 ? 2 : 1;
            case MONASTERY_SWIFTSPEAR:
            case KILN_FIEND:
                return 1;
            case VIASHINO_PYROMANCER:
            case FIREBRAND_ARCHER:
                return 2;
            case KELDON_MARAUDERS:
            case ORCISH_HELLRAISER:
                return 3;
            default:
                return 0;
        }
    }

    /**
     * Casts the first possible card from the list
     *
     * @param cards cards ordered by preference
     * @return the successfully cast card
     */
    Optional<String> playOneOf(boolean rush, String... cards) {
        for (String card : cards) {
            if (game.getHand().contains(card) && canPlay(rush, card)) {
                play(rush, card);
                return Optional.of(card);
            }
        }
        return Optional.empty();
    }

    boolean canPlay(boolean rush, String card) {
        switch (card) {
            case MOUNTAIN:
            case FORGOTTEN_CAVE:
                return !game.isLanded();
            case GITAXIAN_PROBE:
                return true;
            case MONASTERY_SWIFTSPEAR:
            case GHITU_LAVARUNNER:
            case LAVA_SPIKE:
            case CHAIN_LIGHTNING:
            case LIGHTNING_BOLT:
            case LAVA_DART:
            case FORKED_BOLT:
            case SEAL_OF_FIRE:
                return canPay(R);
            case THERMO_ALCHEMIST:
            case KELDON_MARAUDERS:
            case ORCISH_HELLRAISER:
            case VIASHINO_PYROMANCER:
            case FIREBRAND_ARCHER:
            case ELECTROSTATIC_FIELD:
            case KILN_FIEND:
            case MAGMA_JET:
            case FLAME_RIFT:
            case CURSE_OF_THE_PIERCED_HEART:
                return canPay(R1);
            case SEARING_BLAZE:
                return canPay(RR);
            case RIFT_BOLT:
                return rush ? canPay(R) : canPay(R2);
            case VOLCANIC_FALLOUT:
                return canPay(RR1);
            case FIREBLAST:
                return (game.count(withName(MOUNTAIN)) >= 2);
            case LAVA_DART_FB:
                return (game.count(withName(MOUNTAIN)) >= 1);
            case SKEWER_THE_CRITICS:
                return game.getDamageDealtThisTurn() > 0 ? canPay(R) : canPay(R2);
            case NEEDLE_DROP:
                return game.getDamageDealtThisTurn() > 0 && canPay(R);
            case THUNDEROUS_WRATH:
                return false;
        }
        game.log("oops, unsupported card [" + card + "]");
        return false;
    }

    boolean play(boolean rush, String card) {
        switch (card) {
            case MOUNTAIN:
                game.land(card);
                return true;
            case FORGOTTEN_CAVE:
                game.land(card).setTapped(true);
                return true;

            case GITAXIAN_PROBE:
                game.castSorcery(card, Mana.zero());
                game.draw(1);
                return true;

            // R permanents
            case MONASTERY_SWIFTSPEAR:
                produce(R);
                game.castCreature(card, R).setSickness(false);
                return true;
            case GHITU_LAVARUNNER:
                produce(R);
                Card crd = game.castCreature(card, R);
                if (game.countInGraveyard(Game.CardType.sorcery, Game.CardType.instant) >= 2) {
                    crd.setSickness(false);
                }
                return true;
            case SEAL_OF_FIRE:
                produce(R);
                game.castEnchantment(card, R);
                return true;
            // bolts-like
            case LAVA_SPIKE:
            case CHAIN_LIGHTNING:
                produce(R);
                game.castSorcery(card, R);
                game.damageOpponent(3);
                return true;
            case LIGHTNING_BOLT:
                produce(R);
                game.castInstant(card, R);
                game.damageOpponent(3);
                return true;
            case FORKED_BOLT:
                produce(R);
                game.castSorcery(card, R);
                game.damageOpponent(2);
                return true;
            // 1R permanents
            case FIREBRAND_ARCHER:
            case KILN_FIEND:
                produce(R1);
                game.castCreature(card, R1);
                return true;
            case THERMO_ALCHEMIST:
            case ELECTROSTATIC_FIELD:
                produce(R1);
                game.castCreature(card, R1).tag(DEFENDER_SUBTYPE);
                return true;
            case CURSE_OF_THE_PIERCED_HEART:
                produce(R1);
                game.castEnchantment(card, R1);
                return true;
            case KELDON_MARAUDERS:
                produce(R1);
                game.castCreature(card, R1)

                        // 2 vanishing counters
                        .addCounter("vanishing", 2);
                game.damageOpponent(1, "Keldon ETB");
                return true;
            case VIASHINO_PYROMANCER:
                produce(R1);
                game.castCreature(card, R1);
                game.damageOpponent(2, "Viashino ETB");
                return true;
            case ORCISH_HELLRAISER:
                produce(R1);
                game.castCreature(card, R1)

                        // time counter for echo
                        .addCounter("echo", 1);
                return true;
            case MAGMA_JET:
                produce(R1);
                game.castInstant(card, R1);
                game.damageOpponent(2, null);
                scry(2);
                return true;
            case FLAME_RIFT:
                produce(R1);
                game.castInstant(card, R1);
                game.damageOpponent(4, null);
                return true;
            case FIREBLAST:
                sacrificeAMoutain();
                sacrificeAMoutain();
                game.castInstant(card, Mana.zero());
                game.damageOpponent(4, null);
                return true;
            case LAVA_DART:
                produce(R);
                game.castInstant(card, R);
                game.damageOpponent(1);
                return true;
            case LAVA_DART_FB:
                sacrificeAMoutain();
                game.cast(LAVA_DART, Game.Area.graveyard, Game.Area.exile, Mana.zero(), Game.CardType.instant);
                game.damageOpponent(1, null);
                return true;
            case SKEWER_THE_CRITICS:
                if (game.getDamageDealtThisTurn() > 0) {
                    produce(R);
                    game.castSorcery(card, R);
                    game.damageOpponent(3, null);
                } else {
                    produce(R2);
                    game.castSorcery(card, R);
                    game.damageOpponent(3, null);
                }
                return true;
            case NEEDLE_DROP:
                if (game.getDamageDealtThisTurn() > 0) {
                    produce(R);
                    game.castInstant(card, R);
                    game.damageOpponent(1, null);
                    game.draw(1);
                    return true;
                } else {
                    return false;
                }
            case SEARING_BLAZE:
                produce(RR);
                game.castInstant(card, RR);
                int damage = (game.isLanded() ? 3 : 1);
                game.damageOpponent(damage, null);
                return true;
            case VOLCANIC_FALLOUT:
                produce(RR1);
                game.castInstant(card, RR1);
                game.damageOpponent(2, null);
                return true;
            case RIFT_BOLT:
                if (rush) {
                    // cast now
                    produce(R2);
                    game.castSorcery(card, R2);
                    game.damageOpponent(3, null);
                } else {
                    // suspend
                    produce(R);
                    game.pay(R);
                    game.move(card, Game.Area.hand, Game.Area.exile).addCounter("suspend", 1);
                }
                return true;
        }
        game.log("oops, unsupported card [" + card + "]");
        return false;
    }

    private void sacrificeAMoutain() {
        // preferably sacrifice a moutain that is tapped
        Card mountainToSac = game.findFirst(withName(MOUNTAIN).and(tapped())).orElse(game.findFirst(withName(MOUNTAIN)).get());
        if (!mountainToSac.isTapped()) {
            // produce mana before sac
            game.tapLandForMana(mountainToSac, R);
        }
        game.sacrifice(mountainToSac);
    }

    private void scry(int number) {
        // TODO
    }

    @Override
    public void endingPhase() {
        // reset temporary boosts and summoning sickness
        game.find(withType(Game.CardType.creature)).forEach(card -> {
            card.getCounters().remove(TURN_BOOST);
        });

        // then discard extra cards
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
            if (game.count(withName(LANDS)) + game.getHand().count(LANDS) > 3 && game.discardOneOf(FORGOTTEN_CAVE, MOUNTAIN).isPresent()) {
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
        // potential mana pool is current pool + untapped lands
        Mana potentialPool = game.getPool()
                .plus(Mana.of(0, 0, 0, game.count(withName(LANDS).and(untapped())), 0, 0));
        return potentialPool.contains(cost);
    }

    void produce(Mana cost) {
        while (!game.canPay(cost)) {
            Optional<Card> producer = game.findFirst(withName(LANDS).and(untapped()));
            if (producer.isPresent()) {
                game.tapLandForMana(producer.get(), R);
            } else {
                // can't produce !!!
                return;
            }
        }
    }
}
