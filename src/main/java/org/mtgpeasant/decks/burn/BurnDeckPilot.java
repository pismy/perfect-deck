package org.mtgpeasant.decks.burn;

import org.mtgpeasant.perfectdeck.common.cards.Cards;
import org.mtgpeasant.perfectdeck.common.mana.Mana;
import org.mtgpeasant.perfectdeck.goldfish.*;
import org.mtgpeasant.perfectdeck.goldfish.event.GameEvent;
import org.mtgpeasant.perfectdeck.goldfish.event.GameListener;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.mtgpeasant.perfectdeck.goldfish.Permanent.*;

/**
 * TODO:
 * <ul>
 * <li>manage [light up the stage]</li>
 * <li>manage [magma jet] scry</li>
 * <li>make [gitaxian probe] part of the turn simulation</li>
 * </ul>
 */
public class BurnDeckPilot extends DeckPilot<Game> implements BurnCards, GameListener, Seer.SpellsPlayer {

    public static final Mana R = Mana.of("R");
    public static final Mana R1 = Mana.of("1R");
    public static final Mana R2 = Mana.of("2R");
    public static final Mana RR = Mana.of("RR");
    public static final Mana RR1 = Mana.of("1RR");

    private static String[] CREATURES = {MONASTERY_SWIFTSPEAR, THERMO_ALCHEMIST, ELECTROSTATIC_FIELD, FIREBRAND_ARCHER, KELDON_MARAUDERS, GHITU_LAVARUNNER, ORCISH_HELLRAISER, VIASHINO_PYROMANCER, FURNACE_SCAMP};
    private static String[] LANDS = {MOUNTAIN, FORGOTTEN_CAVE};

    // all cards that could contribute to a kill in the turn
    private static String[] RUSH = {MONASTERY_SWIFTSPEAR, FIREBRAND_ARCHER, KELDON_MARAUDERS, GHITU_LAVARUNNER, VIASHINO_PYROMANCER, ELECTROSTATIC_FIELD,
            RIFT_BOLT, FIREBLAST, LAVA_SPIKE, LIGHTNING_BOLT, SKEWER_THE_CRITICS, LAVA_DART, LAVA_DART_FB, NEEDLE_DROP, CHAIN_LIGHTNING, FORKED_BOLT, SEARING_BLAZE, MAGMA_JET, VOLCANIC_FALLOUT, FLAME_RIFT, SEAL_OF_FIRE, FURNACE_SCAMP, RECKLESS_ABANDON};

    private transient Seer.VictoryRoute victoryRoute;

    public BurnDeckPilot(Game game) {
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
        mulligan(game.getMulligans());
    }

    @Override
    public void upkeepStep() {
        // decrement all time counters and apply effects
        game.getBattlefield().find(withName(KELDON_MARAUDERS)).forEach(card -> {
            card.decrCounter("vanishing");
            if (card.getCounter("vanishing") == 0) {
                game.sacrifice(card);
                game.damageOpponent(1, "keldon LTB trigger");
            }
        });
        game.getBattlefield().find(withName(ORCISH_HELLRAISER)).forEach(card -> {
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
        game.getBattlefield().find(withName(CURSE_OF_THE_PIERCED_HEART)).forEach(curse -> {
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
        while (playOneOf(GITAXIAN_PROBE).isPresent()) {
        }

        // then land
        playOneOf(MOUNTAIN, FORGOTTEN_CAVE);

        // is there a way to kill opponent this turn (only from turn 3)?
        if (game.getCurrentTurn() > 2) {
            victoryRoute = Seer.findRouteToVictory(this, RUSH).orElse(null);
            if (victoryRoute != null) {
                game.log(">>> I can kill now with: " + victoryRoute);
                // sacrifice all seals
                game.getBattlefield().find(withName(SEAL_OF_FIRE)).forEach(seal -> {
                    game.sacrifice(seal);
                    game.damageOpponent(2);
                });

                // then play spells
                victoryRoute.play(this);
                return;
            }
        }

        while (playBestCard()) {
        }
    }

    @Override
    public void combatPhase() {
        List<Permanent> creatures = game.getBattlefield().find(creaturesThatCanBeTapped().and(notWithTag(DEFENDER_SUBTYPE)));
        creatures.forEach(card -> game.tapForAttack(card, strength(card)));
        creatures.stream().filter(p -> p.getCard().equals(FURNACE_SCAMP)).forEach(
            card -> { game.sacrifice(card); game.damageOpponent(3, "Sacrifice " + FURNACE_SCAMP); });
    }

    @Override
    public void secondMainPhase() {
        if (victoryRoute != null) {
            victoryRoute.play(this);
        }

        while (playBestCard()) {
        }
    }

    @Override
    public void endingPhase() {
        // use untapped thermo a last time
        game.getBattlefield().find(withName(THERMO_ALCHEMIST).and(untapped()).and(withoutSickness())).forEach(card -> {
            game.tap(card);
            game.damageOpponent(1, "thermo");
        });

        // decrement LUTS counter
        game.getExile().stream().filter(withCounter(LIGHT_UP_THE_STAGE)).collect(Collectors.toList()).forEach(card -> {
            card.decrCounter(LIGHT_UP_THE_STAGE);
        });


        // then discard extra cards
        if (game.getHand().size() > 7) {
            discard(game.getHand().size() - 7);
        }
    }

    /**
     * Manages all triggers
     *
     * @param event game event
     */
    @Override
    public void onEvent(GameEvent event) {
        if (event.getType() == GameEvent.Type.cast) {
            GameEvent.SpellEvent spellEvent = (GameEvent.SpellEvent) event;
            // trigger instants and sorceries
            if (spellEvent.hasType(Game.CardType.instant) || spellEvent.hasType(Game.CardType.sorcery)) {
                // trigger all untapped thermo
                game.getBattlefield().find(withName(THERMO_ALCHEMIST).and(untapped()).and(withoutSickness())).forEach(crea -> game.damageOpponent(1, "thermo ability"));

                // trigger all electrostatic fields
                game.getBattlefield().find(withName(ELECTROSTATIC_FIELD)).forEach(crea -> game.damageOpponent(1, "electrostatic field trigger"));

                // untap ghitu lavarunners if at least 2 instant and sorceries ?
                int instantsAndSorceriesInGY = countInGraveyard(Game.CardType.instant, Game.CardType.sorcery);
                if (instantsAndSorceriesInGY >= 2) {
                    game.getBattlefield().find(withName(GHITU_LAVARUNNER).and(withSickness())).forEach(crea -> {
                        crea.setSickness(false);
                    });
                }

                // trigger all untapped kiln
                game.getBattlefield().find(withName(KILN_FIEND)).forEach(crea -> crea.addCounter(TURN_BOOST, 3));
            }

            // trigger non-creatures
            if (!spellEvent.hasType(Game.CardType.creature) && !spellEvent.hasType(Game.CardType.land)) {
                // trigger all archer
                game.getBattlefield().find(withName(FIREBRAND_ARCHER)).forEach(crea -> game.damageOpponent(1, "firebrand trigger"));

                // monastery prowess
                game.getBattlefield().find(withName(MONASTERY_SWIFTSPEAR)).forEach(crea -> crea.addCounter(TURN_BOOST, 1));
            }
        }
    }

    /**
     * Play the best card in case there is no kill option this turn
     */
    private boolean playBestCard() {
        boolean ghituHasHaste = countInGraveyard(Game.CardType.sorcery, Game.CardType.instant) >= 2;
        return playOneOf(
                MOUNTAIN,
                MONASTERY_SWIFTSPEAR,
                FURNACE_SCAMP,
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
                game.getDamageDealtThisTurn() > 0 ? LIGHT_UP_THE_STAGE : "_",
                RIFT_BOLT_SP,
                CHAIN_LIGHTNING,
                LAVA_SPIKE,
                LIGHTNING_BOLT,
                FORKED_BOLT,
                SEAL_OF_FIRE,
                MAGMA_JET,
                VOLCANIC_FALLOUT,
                GHITU_LAVARUNNER,
                LIGHT_UP_THE_STAGE,
                SKEWER_THE_CRITICS,
                FORGOTTEN_CAVE_CYCLE
        ).isPresent();
    }

    public int countInGraveyard(Game.CardType... types) {
        List<Game.CardType> typesList = Arrays.asList(types);
        return (int) game.getGraveyard().stream().filter(card -> typesList.contains(typeof(card))).count();
//        return Arrays.stream(types).mapToInt(type -> getGraveyard().count(this.cardsOfType(type))).sum();
    }

    private int strength(Permanent creature) {
        // strength is base strength + +1/1 counters + temporary boosts + 2 * rancors
        return baseStrength(creature)
                + creature.getCounters().getOrDefault(PERM_BOOST, 0)
                + creature.getCounters().getOrDefault(TURN_BOOST, 0);
    }

    private int baseStrength(Permanent creature) {
        switch (creature.getCard()) {
            case GHITU_LAVARUNNER:
                return countInGraveyard(Game.CardType.instant, Game.CardType.sorcery) >= 2 ? 2 : 1;
            case MONASTERY_SWIFTSPEAR:
            case KILN_FIEND:
            case FURNACE_SCAMP:
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
    Optional<String> playOneOf(String... cards) {
        for (String card : cards) {
            if (canPlay(card)) {
                play(card);
                return Optional.of(card);
            }
        }
        return Optional.empty();
    }

    boolean hasInHand(String card) {
        return game.getExile().findFirst(withName(card).and(withCounter(LIGHT_UP_THE_STAGE))).isPresent() || game.getHand().contains(card);
    }

    @Override
    public boolean canPlay(String card) {
        // first check has card
        switch (card) {
            case LAVA_DART_FB:
                if (!game.getGraveyard().contains(LAVA_DART)) {
                    return false;
                }
                break;
            case RIFT_BOLT_SP:
                if (!hasInHand(RIFT_BOLT)) {
                    return false;
                }
                break;
            case FORGOTTEN_CAVE_CYCLE:
                if (!hasInHand(FORGOTTEN_CAVE)) {
                    return false;
                }
                break;
            default:
                // for all other cards: must be in hand
                if (!hasInHand(card)) {
                    return false;
                }
                break;
        }
        switch (card) {
            case MOUNTAIN:
            case FORGOTTEN_CAVE:
                return !game.isLanded();
            case GITAXIAN_PROBE:
                return true;
            case FORGOTTEN_CAVE_CYCLE:
            case MONASTERY_SWIFTSPEAR:
            case GHITU_LAVARUNNER:
            case LAVA_SPIKE:
            case CHAIN_LIGHTNING:
            case LIGHTNING_BOLT:
            case LAVA_DART:
            case FORKED_BOLT:
            case SEAL_OF_FIRE:
            case FURNACE_SCAMP:
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
                return canPay(R2);
            case RIFT_BOLT_SP:
                return canPay(R);
            case VOLCANIC_FALLOUT:
                return canPay(RR1);
            case FIREBLAST:
                return ((int) game.getBattlefield().count(withName(MOUNTAIN)) >= 2);
            case LAVA_DART_FB:
                return ((int) game.getBattlefield().count(withName(MOUNTAIN)) >= 1);
            case SKEWER_THE_CRITICS:
                return game.getDamageDealtThisTurn() > 0 ? canPay(R) : canPay(R2);
            case LIGHT_UP_THE_STAGE:
                return game.getDamageDealtThisTurn() > 0 ? canPay(R) : canPay(R2);
            case NEEDLE_DROP:
                return game.getDamageDealtThisTurn() > 0 && canPay(R);
            case THUNDEROUS_WRATH:
                return false;
            case RECKLESS_ABANDON:
                return canPay(R) && ((int) game.getBattlefield().count(withType(Game.CardType.creature)) >= 1);
        }
        game.log("oops, unsupported card [" + card + "]");
        return false;
    }

    @Override
    public boolean play(String card) {
        // maybe move card from exile (LUTS)
        String real_card = RIFT_BOLT_SP.equals(card) ? RIFT_BOLT : card;
        Optional<Permanent> cardFromExile = game.getExile().findFirst(withName(real_card).and(withCounter(LIGHT_UP_THE_STAGE)));
        if (cardFromExile.isPresent()) {
            game.move(real_card, Game.Area.exile, Game.Area.hand);
        }

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

            case FORGOTTEN_CAVE_CYCLE:
                produce(R);
                game.move(FORGOTTEN_CAVE, Game.Area.hand, Game.Area.graveyard);
                game.draw(1);
                return true;

            // R permanents
            case MONASTERY_SWIFTSPEAR:
                produce(R);
                game.castCreature(card, R).setSickness(false);
                return true;
            case GHITU_LAVARUNNER:
                produce(R);
                Permanent crd = game.castCreature(card, R);
                if (countInGraveyard(Game.CardType.sorcery, Game.CardType.instant) >= 2) {
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
            case FURNACE_SCAMP:
                produce(R);
                game.castCreature(card, R);
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
                } else {
                    produce(R2);
                    game.castSorcery(card, R2);
                }
                game.damageOpponent(3, null);
                return true;
            case LIGHT_UP_THE_STAGE:
                if (game.getDamageDealtThisTurn() > 0) {
                    produce(R);
                    game.castSorcery(card, R);
                } else {
                    produce(R2);
                    game.castSorcery(card, R2);
                }
                // move the top 2 cards from library to exile
                game.move(game.getLibrary().getFirst(), Game.Area.library, Game.Area.exile).addCounter(LIGHT_UP_THE_STAGE, 2);
                game.move(game.getLibrary().getFirst(), Game.Area.library, Game.Area.exile).addCounter(LIGHT_UP_THE_STAGE, 2);
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
                // cast now
                produce(R2);
                game.castSorcery(card, R2);
                game.damageOpponent(3, null);
                return true;
            case RIFT_BOLT_SP:
                // suspend
                produce(R);
                game.pay(R);
                game.move(RIFT_BOLT, Game.Area.hand, Game.Area.exile).addCounter("suspend", 1);
                return true;
            case RECKLESS_ABANDON:
                produce(R);
                // TODO - find best creature to sacrifice
                Optional<Permanent> creature = game.getBattlefield().findFirst(withType(Game.CardType.creature));
                if(creature.isPresent()) {
                    game.sacrifice(creature.get());
                    game.castSorcery(card, R);
                    game.damageOpponent(4, null);
                    return true;
                } else {
                    return false;
                }
        }
        game.log("oops, unsupported card [" + card + "]");
        return false;
    }

    private void sacrificeAMoutain() {
        // preferably sacrifice a moutain that is tapped
        Permanent mountainToSac = game.getBattlefield().findFirst(withName(MOUNTAIN).and(tapped())).orElse(game.getBattlefield().findFirst(withName(MOUNTAIN)).get());
        if (!mountainToSac.isTapped()) {
            // produce mana before sac
            game.tapLandForMana(mountainToSac, R);
        }
        game.sacrifice(mountainToSac);
    }

    private void scry(int number) {
        Cards top_cards = Cards.empty();
        for(int i = 0; i < Math.min(number, game.getLibrary().size()); i++){
            top_cards.add(game.getLibrary().removeFirst());
        }
        for (String card: top_cards) {
            // remove extra lands
            if (typeof(card) == Game.CardType.land && game.getHand().count(LANDS) + game.getBattlefield().count(withType(Game.CardType.land)) > Math.max(2, 2 * game.getHand().count(FIREBLAST)) ) {
                game.getLibrary().addLast(card);
                continue;
            }
            // remove furnace scamp after turn 2
            if (FURNACE_SCAMP.equals(card) && game.getCurrentTurn() > 2) {
                game.getLibrary().addLast(card);
                continue;
            }
            // remove extra thermo or archer because instant or sorceries are better options
            if (THERMO_ALCHEMIST.equals(card) && (game.getHand().count(THERMO_ALCHEMIST) + game.getBattlefield().count(withName(THERMO_ALCHEMIST)) > 2 )) {
                game.getLibrary().addLast(card);
                continue;
            }
            if (FIREBRAND_ARCHER.equals(card) && (game.getHand().count(FIREBRAND_ARCHER) + game.getBattlefield().count(withName(FIREBRAND_ARCHER)) >= 2 )) {
                game.getLibrary().addLast(card);
                continue;
            }
            // remove fireblast if less than 2 mountains
            if (FIREBLAST.equals(card) && (game.getHand().count(MOUNTAIN) + game.getBattlefield().count(withName(MOUNTAIN)) < 2 )) {
                game.getLibrary().addLast(card);
                continue;
            }
            // otherwise keep card
            game.getLibrary().addFirst(card);

            // TOOD: manage best cards order
        }

    }

    void mulligan(int number) {
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
            if ((int) game.getBattlefield().count(withName(LANDS)) + game.getHand().count(LANDS) > 3 && game.discardOneOf(FORGOTTEN_CAVE, MOUNTAIN).isPresent()) {
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
                .plus(Mana.of(0, 0, 0, (int) game.getBattlefield().count(withName(LANDS).and(untapped())), 0, 0));
        return potentialPool.contains(cost);
    }

    void produce(Mana cost) {
        while (!game.canPay(cost)) {
            Optional<Permanent> producer = game.getBattlefield().findFirst(withName(LANDS).and(untapped()));
            if (producer.isPresent()) {
                game.tapLandForMana(producer.get(), R);
            } else {
                // can't produce !!!
                return;
            }
        }
    }
}
