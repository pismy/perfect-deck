package org.mtgpeasant.decks.stompy;

import org.mtgpeasant.perfectdeck.common.cards.Cards;
import org.mtgpeasant.perfectdeck.common.mana.Mana;
import org.mtgpeasant.perfectdeck.common.matchers.MulliganRules;
import org.mtgpeasant.perfectdeck.goldfish.DeckPilot;
import org.mtgpeasant.perfectdeck.goldfish.Game;
import org.mtgpeasant.perfectdeck.goldfish.Permanent;
import org.mtgpeasant.perfectdeck.goldfish.Seer;
import org.mtgpeasant.perfectdeck.goldfish.event.GameEvent;
import org.mtgpeasant.perfectdeck.goldfish.event.GameListener;

import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

import static org.mtgpeasant.perfectdeck.common.mana.Mana.zero;
import static org.mtgpeasant.perfectdeck.goldfish.Permanent.*;

/**
 * TODO:
 * <ul>
 * <li>force hunger by sacrificing an eldrazi spawn</li>
 * <li>use Quirion to untap Nettle or mana producers</li>
 * </ul>
 */
public class StompyDeckPilot extends DeckPilot<Game> implements GameListener, Seer.SpellsPlayer {

    private static final Mana ONE = Mana.of("1");
    private static final Mana G = Mana.of("G");
    private static final Mana G1 = Mana.of("1G");
    private static final Mana GR = Mana.of("GR");
    private static final Mana G2 = Mana.of("2G");
    private static final Mana GG = Mana.of("GG");

    // LANDS
    private static final String FOREST = "forest";

    // CREATURES
    private static final String QUIRION_RANGER = "quirion ranger";
    private static final String NETTLE_SENTINEL = "nettle sentinel";
    private static final String SKARRGAN_PIT_SKULK = "skarrgan pit-skulk";
    private static final String VAULT_SKIRGE = "vault skirge";
    private static final String NEST_INVADER = "nest invader";
    private static final String BURNING_TREE_EMISSARY = "burning-tree emissary";
    private static final String SAFEHOLD_ELITE = "safehold elite";
    private static final String SILHANA_LEDGEWALKER = "silhana ledgewalker";
    private static final String YOUNG_WOLF = "young wolf";
    private static final String RIVER_BOA = "river boa";
    private static final String STRANGLEROOT_GEIST = "strangleroot geist";
    private static final String LLANOWAR_ELVES = "llanowar elves";
    private static final String FYNDHORN_ELVES = "fyndhorn elves";
    private static final String ELDRAZI_SPAWN = "eldrazi spawn";
    public static final String SYR_FAREN_THE_HENGEHAMMER = "syr faren, the hengehammer";

    private static final String[] CREATURES = {QUIRION_RANGER, NETTLE_SENTINEL, SKARRGAN_PIT_SKULK, VAULT_SKIRGE, NEST_INVADER, BURNING_TREE_EMISSARY, SAFEHOLD_ELITE, SILHANA_LEDGEWALKER, YOUNG_WOLF, RIVER_BOA, STRANGLEROOT_GEIST, SYR_FAREN_THE_HENGEHAMMER, LLANOWAR_ELVES, FYNDHORN_ELVES, ELDRAZI_SPAWN};

    // BOOSTS
    private static final String RANCOR = "rancor";
    private static final String VINES_OF_VASTWOOD = "vines of vastwood"; // (instant) GG: +4/+4
    private static final String CURSE_OF_PREDATION = "curse of predation";
    private static final String HUNGER_OF_THE_HOWLPACK = "hunger of the howlpack";
    private static final String ASPECT_OF_HYDRA = "aspect of hydra";
    private static final String SAVAGE_SWIPE = "savage swipe";

    private static final String[] BOOSTS = {STRANGLEROOT_GEIST, CURSE_OF_PREDATION, HUNGER_OF_THE_HOWLPACK, ASPECT_OF_HYDRA, SAVAGE_SWIPE, RANCOR, VINES_OF_VASTWOOD};

    // OTHERS
    private static final String GITAXIAN_PROBE = "gitaxian probe";
    private static final String LAND_GRANT = "land grant";
    private static final String DISMEMBER = "dismember";

    private static final String PERM_BOOST = "+1/+1";
    private static final String TEMP_BOOST = "*+1/+1";


    private static MulliganRules rules;

    // turn state
    private boolean aCreatureIsDead = false;


    static {
        try {
            rules = MulliganRules.parse(new InputStreamReader(StompyDeckPilot.class.getResourceAsStream("/stompy-rules.txt")));
        } catch (IOException e) {
            rules = null;
            System.err.println(e);
        }
    }

    public StompyDeckPilot(Game game) {
        super(game);
    }

    @Override
    public boolean keepHand(Cards hand) {
        if (game.getMulligans() >= 3) {
            return true;
        }
        return rules.firstMatch(hand).isPresent();
    }

    @Override
    public void start() {
        putOnBottomOfLibrary(game.getMulligans());
    }

    @Override
    public void untapStep() {
        aCreatureIsDead = false;

        game.untapAll();

        // Nettles don't untap as normal
        game.getBattlefield().find(withName(NETTLE_SENTINEL)).forEach(card -> card.setTapped(true));
    }

    @Override
    public void firstMainPhase() {
        while (playOneOf(
                // start by playing all probes
                GITAXIAN_PROBE,
                // then land or land grant
                FOREST,
                LAND_GRANT
        ).isPresent()) {
        }

        // simulate if I can rush now
        if (game.getCurrentTurn() > 2) {
            Optional<Seer.VictoryRoute> victoryRoute = Seer.findRouteToVictory(this, BOOSTS);
            if (victoryRoute.isPresent()) {
                game.log(">> I can rush now with: " + victoryRoute);
                maybeSacrificeForHunger();
                victoryRoute.get().play(this);
            }
        }

        while (playOneOf(
                // chain burning trees
                BURNING_TREE_EMISSARY,
                // then curse
                CURSE_OF_PREDATION
        ).isPresent()) {
        }

        /*
         * what to do now ?
         * - play a green spell to untap Nettle without sickness
         * - sac a spawn before casting a hunger
         * - attack before casting any Skarrgan
         * - consume any mana still in the pool (due to emissary)
         * // TODO
         */

        maybeSacrificeForHunger();

        while (playOneOf(
                // then hunger if a creature is dead
                aCreatureIsDead ? HUNGER_OF_THE_HOWLPACK : "_",
                // then rancor
                RANCOR,
                // then haste
                STRANGLEROOT_GEIST
        ).isPresent()) {

        }

        // TODO: play more (for e.g. to untap nettle before combat)
        while (playOneOf(
                // at least one Quirion on battlefield is top priority
                !game.getBattlefield().findFirst(withName(QUIRION_RANGER)).isPresent() ? QUIRION_RANGER : "_",
                LLANOWAR_ELVES,
                FYNDHORN_ELVES,
                // skarrgan if damage dealt (+1/+1)
                game.getDamageDealtThisTurn() > 0 ? SKARRGAN_PIT_SKULK : "_",
                NEST_INVADER,
                // then hunger if a creature is dead
                aCreatureIsDead ? HUNGER_OF_THE_HOWLPACK : "_",
                SYR_FAREN_THE_HENGEHAMMER,
                SAFEHOLD_ELITE,
                NETTLE_SENTINEL,
                RIVER_BOA,
                SILHANA_LEDGEWALKER,
                QUIRION_RANGER,
                VAULT_SKIRGE,
                SKARRGAN_PIT_SKULK,
                YOUNG_WOLF
        ).isPresent()) {

        }

        if (game.getPool().ccm() > 0) {
            // still have mana in pool: we should consume before combat phase
            // TODO
            game.log(">> unused pool at end of first main phase: " + game.getPool());
        }
    }

    @Override
    public void combatPhase() {
        List<Permanent> attackingCreatures = game.getBattlefield().find(creaturesThatCanBeTapped());
        if (attackingCreatures.isEmpty()) {
            return;
        }
        int curses = game.getBattlefield().count(withName(CURSE_OF_PREDATION));

        // trigger Syr Faren effect
        attackingCreatures.stream().filter(withName(SYR_FAREN_THE_HENGEHAMMER)).forEach(syr -> {
            // find target (first "other" attacking creature)
            attackingCreatures.stream().filter(crea -> crea != syr).findFirst().ifPresent(crea -> crea.addCounter(TEMP_BOOST, strength(syr)));
        });

        // attack with all creatures
        attackingCreatures.forEach(card -> {
            // add one +1/+1 counter per curse
            card.addCounter(PERM_BOOST, curses);
            game.tapForAttack(card, strength(card));
        });
    }

    @Override
    public void secondMainPhase() {
        while (playOneOf(
                // at least one Quirion on battlefield is top priority
                !game.getBattlefield().findFirst(withName(QUIRION_RANGER)).isPresent() ? QUIRION_RANGER : "_",
                LLANOWAR_ELVES,
                FYNDHORN_ELVES,
                // skarrgan if damage dealt (+1/+1)
                game.getDamageDealtThisTurn() > 0 ? SKARRGAN_PIT_SKULK : "_",
                NEST_INVADER,
                // then hunger if a creature is dead
                aCreatureIsDead ? HUNGER_OF_THE_HOWLPACK : "_",
                SYR_FAREN_THE_HENGEHAMMER,
                SAFEHOLD_ELITE,
                NETTLE_SENTINEL,
                RIVER_BOA,
                SILHANA_LEDGEWALKER,
                QUIRION_RANGER,
                VAULT_SKIRGE,
                SKARRGAN_PIT_SKULK,
                YOUNG_WOLF
        ).isPresent()) {

        }
    }

    @Override
    public void endingPhase() {
        // discard to 7
        if (game.getHand().size() > 7) {
            discard(game.getHand().size() - 7);
        }
    }

    private int strength(Permanent creature) {
        // strength is base strength + +1/1 counters + temporary boosts + 2 * rancors
        return baseStrength(creature)
                + creature.getCounter(PERM_BOOST)
                + creature.getCounter(TEMP_BOOST)
                + 2 * creature.getCounter(RANCOR);
    }

    private int baseStrength(Permanent creature) {
        switch (creature.getCard()) {
            case QUIRION_RANGER:
            case SKARRGAN_PIT_SKULK:
            case VAULT_SKIRGE:
            case SILHANA_LEDGEWALKER:
            case YOUNG_WOLF:
            case LLANOWAR_ELVES:
            case FYNDHORN_ELVES:
                return 1;
            case NETTLE_SENTINEL:
            case NEST_INVADER:
            case BURNING_TREE_EMISSARY:
            case SAFEHOLD_ELITE:
            case RIVER_BOA:
            case STRANGLEROOT_GEIST:
            case SYR_FAREN_THE_HENGEHAMMER:
                return 2;

            case ELDRAZI_SPAWN:
            default:
                return 0;
        }
    }

    private int devotion() {
        return game.getBattlefield().stream().mapToInt(this::devotion).sum();
    }

    private int devotion(Permanent permanent) {
        switch (permanent.getCard()) {
            case QUIRION_RANGER:
            case NETTLE_SENTINEL:
            case SKARRGAN_PIT_SKULK:
            case NEST_INVADER:
            case SAFEHOLD_ELITE:
            case SILHANA_LEDGEWALKER:
            case YOUNG_WOLF:
            case RIVER_BOA:
            case LLANOWAR_ELVES:
            case FYNDHORN_ELVES:
            case RANCOR:
            case CURSE_OF_PREDATION:
                return 1;

            case BURNING_TREE_EMISSARY:
            case STRANGLEROOT_GEIST:
            case SYR_FAREN_THE_HENGEHAMMER:
                return 2;

            default:
                return 0;
        }
    }

    void putOnBottomOfLibrary(int number) {
        for (int i = 0; i < number; i++) {
            if (game.putOnBottomOfLibraryOneOf(DISMEMBER).isPresent()) {
                continue;
            }
            // discard extra lands
            if (game.getHand().count(FOREST) > 2 && game.putOnBottomOfLibraryOneOf(FOREST).isPresent()) {
                continue;
            }
            // discard extra creatures
            if (game.getHand().count(CREATURES) > 2 && game.putOnBottomOfLibraryOneOf(CREATURES).isPresent()) {
                continue;
            }
            // TODO: choose better
            game.putOnBottomOfLibrary(game.getHand().getFirst());
        }
    }

    void discard(int number) {
        for (int i = 0; i < number; i++) {
            if (game.discardOneOf(DISMEMBER).isPresent()) {
                continue;
            }
            // discard extra lands
            if (game.getBattlefield().count(withName(FOREST)) + game.getHand().count(FOREST) > 3 && game.discardOneOf(FOREST).isPresent()) {
                continue;
            }
            // discard extra creatures
            if (game.getBattlefield().count(withName(CREATURES)) + game.getHand().count(CREATURES) > 2 && game.discardOneOf(CREATURES).isPresent()) {
                continue;
            }
            // TODO: choose better
            game.discard(game.getHand().getFirst());
        }
    }

    boolean canPay(Mana cost) {
        // potential mana pool is current pool + untapped lands + petals on battlefield
        boolean canUseQuirion = game.getBattlefield().findFirst(withName(QUIRION_RANGER)).isPresent() && game.getBattlefield().findFirst(withName(FOREST)).isPresent() && !game.isLanded();
        Mana potentialPool = game.getPool()
                .plus(Mana.of(
                        0,
                        0,
                        game.getBattlefield().count(withName(FOREST).and(untapped())) + (canUseQuirion ? 1 : 0) + game.getBattlefield().count(withName(LLANOWAR_ELVES, FYNDHORN_ELVES).and(untapped())),
                        0,
                        0,
                        game.getBattlefield().count(withName(ELDRAZI_SPAWN)))
                );
        return potentialPool.contains(cost);
    }

    void produce(Mana cost) {
        while (!game.canPay(cost)) {
            Optional<Permanent> land = game.getBattlefield().findFirst(withName(FOREST).and(untapped()));
            if (land.isPresent()) {
                game.tapLandForMana(land.get(), G);
            } else if (game.getBattlefield().count(withName(LLANOWAR_ELVES, FYNDHORN_ELVES).and(untapped())) > 0) {
                game.tap(game.getBattlefield().findFirst(withName(LLANOWAR_ELVES, FYNDHORN_ELVES).and(untapped())).get());
                game.add(G);
            } else if (game.getBattlefield().findFirst(withName(QUIRION_RANGER)).isPresent() && game.getBattlefield().findFirst(withName(FOREST)).isPresent() && !game.isLanded()) {
                // use Quirion ability
                game.log("use [Quirion Ranger] ability");
                game.move(FOREST, Game.Area.battlefield, Game.Area.hand);
                Permanent forest = game.land(FOREST);
                game.tapLandForMana(forest, G);
                // if possible untap a nettle or a mana producer ? TODO (manage summoning sickness)
            } else if (game.getBattlefield().count(withName(ELDRAZI_SPAWN)) > 0) {
                // sacrifice a spawn
                sacrificeASpawn();
            } else {
                // can't preparePool !!!
                return;
            }
        }
    }

    private void sacrificeASpawn() {
        game.getBattlefield().findFirst(withName(ELDRAZI_SPAWN)).ifPresent(card -> {
            game.sacrifice(card);
            game.add(ONE);
            aCreatureIsDead = true;
        });
    }

    void maybeSacrificeForHunger() {
        if (!aCreatureIsDead
                && canPay(G)
                && game.getHand().contains(HUNGER_OF_THE_HOWLPACK)
                && game.getBattlefield().findFirst(withName(ELDRAZI_SPAWN)).isPresent()
                && game.getBattlefield().findFirst(creatureThatCanAttackOrNettle()).isPresent()
        ) {
            sacrificeASpawn();
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

    Predicate<Permanent> creatureThatCanAttackOrNettle() {
        return creaturesThatCanBeTapped().or(withName(NETTLE_SENTINEL).and(withoutSickness()));
    }

    @Override
    public boolean canPlay(String card) {
        // first check card is in hand
        if (!game.getHand().contains(card)) {
            return false;
        }
        switch (card) {
            case FOREST:
                return !game.isLanded();
            case LAND_GRANT:
                return game.getHand().count(FOREST) == 0;
            case GITAXIAN_PROBE:
                return true;

            // creatures
            case VAULT_SKIRGE:
                return canPay(ONE);
            case QUIRION_RANGER:
            case LLANOWAR_ELVES:
            case FYNDHORN_ELVES:
            case SKARRGAN_PIT_SKULK:
            case NETTLE_SENTINEL:
            case YOUNG_WOLF:
                return canPay(G);
            case NEST_INVADER:
            case SAFEHOLD_ELITE:
            case RIVER_BOA:
            case SILHANA_LEDGEWALKER:
            case BURNING_TREE_EMISSARY: // TODO: not quite exact
                return canPay(G1);
            case STRANGLEROOT_GEIST:
            case SYR_FAREN_THE_HENGEHAMMER:
                return canPay(GG);

            // enchantments
            case RANCOR:
                // need a target creature (any)
                return game.getBattlefield().findFirst(withType(Game.CardType.creature)).isPresent() && canPay(G);
            case CURSE_OF_PREDATION:
                return canPay(G2);

            // boosts
            case ASPECT_OF_HYDRA:
            case HUNGER_OF_THE_HOWLPACK:
                // need an untapped target creature (or a Nettle without sickness)
                return game.getBattlefield().findFirst(creatureThatCanAttackOrNettle()).isPresent() && canPay(G);
            case SAVAGE_SWIPE:
                // need a target creature with power 2
                return game.getBattlefield().findFirst(creatureThatCanAttackOrNettle().and(crea -> strength(crea) == 2)).isPresent()
                        && canPay(G);
            case VINES_OF_VASTWOOD:
                // need a target creature
                return game.getBattlefield().findFirst(creatureThatCanAttackOrNettle()).isPresent() && canPay(GG);
        }
        game.log("oops, unsupported card [" + card + "]");
        return false;
    }

    @Override
    public boolean play(String card) {
        switch (card) {
            case FOREST:
                game.land(card);
                return true;
            case LAND_GRANT:
                // put a forest from library to the battlefield
                game.castSorcery(card, zero());
                game.move(FOREST, Game.Area.library, Game.Area.hand);
                return true;

            case GITAXIAN_PROBE:
                game.castSorcery(card, zero());
                game.draw(1);
                return true;

            // === creatures
            case VAULT_SKIRGE:
                produce(ONE);
                game.castCreature(card, ONE);
                return true;
            case QUIRION_RANGER:
            case LLANOWAR_ELVES:
            case FYNDHORN_ELVES:
            case NETTLE_SENTINEL:
            case YOUNG_WOLF:
                produce(G);
                game.castCreature(card, G);
                return true;
            case SKARRGAN_PIT_SKULK:
                produce(G);
                Permanent crea = game.castCreature(card, G);
                if (game.getDamageDealtThisTurn() > 0) {
                    crea.addCounter(PERM_BOOST, 1);
                }
                return true;
            case NEST_INVADER: {
                produce(G1);
                game.castCreature(card, G1);
                game.createToken(ELDRAZI_SPAWN, Game.CardType.creature);
                return true;
            }
            case SAFEHOLD_ELITE:
            case RIVER_BOA:
            case SILHANA_LEDGEWALKER:
                produce(G1);
                game.castCreature(card, G1);
                return true;
            case BURNING_TREE_EMISSARY:
                produce(G1);
                game.castCreature(card, G1);
                game.add(GR);
                return true;
            case STRANGLEROOT_GEIST:
                produce(GG);
                game.castCreature(card, GG).setSickness(false);
                return true;
            case SYR_FAREN_THE_HENGEHAMMER:
                produce(GG);
                game.castCreature(card, GG);
                return true;

            // === enchantments
            case RANCOR: {
                // target preferably a creature ready to attack, or else any creature
                Permanent targetCreature =
                        // first choice: Syr Faren
                        game.getBattlefield().findFirst(creaturesThatCanBeTapped().and(withName(SYR_FAREN_THE_HENGEHAMMER)))
                                // second choice: any creature ready to attack
                                .orElseGet(() -> game.getBattlefield().findFirst(creatureThatCanAttackOrNettle())
                                        // last choice: any creature (will attack next turn)
                                        .orElseGet(() -> game.getBattlefield().findFirst(withType(Game.CardType.creature)).get()));
                produce(G);
                game.castEnchantment(card, G).tag("on:" + targetCreature.getCard());
                targetCreature.incrCounter(RANCOR);
                return true;
            }
            case CURSE_OF_PREDATION:
                produce(G2);
                game.castEnchantment(card, G2);
                return true;

            // === boosts
            case ASPECT_OF_HYDRA: {
                // target a creature ready to attack
                Permanent targetCreature =
                        // first choice: Syr Faren
                        game.getBattlefield().findFirst(creaturesThatCanBeTapped().and(withName(SYR_FAREN_THE_HENGEHAMMER)))
                                // second choice: any creature ready to attack
                                .orElseGet(() -> game.getBattlefield().findFirst(creatureThatCanAttackOrNettle()).get());
                produce(G);
                game.castInstant(card, G);
                int devotion = devotion();
                targetCreature.addCounter(TEMP_BOOST, devotion);
                return true;
            }
            case SAVAGE_SWIPE: {
                // target a creature ready to attack with power 2
                Permanent targetCreature =
                        // first choice: Syr Faren (with power 2)
                        game.getBattlefield().findFirst(creaturesThatCanBeTapped().and(withName(SYR_FAREN_THE_HENGEHAMMER).and(c -> strength(c) == 2)))
                                // second choice: any creature ready to attack (with power 2)
                                .orElseGet(() -> game.getBattlefield().findFirst(creatureThatCanAttackOrNettle().and(c -> strength(c) == 2)).get());
                produce(G);
                game.castSorcery(card, G);
                targetCreature.addCounter(TEMP_BOOST, 2);
                return true;
            }
            case HUNGER_OF_THE_HOWLPACK: {
                // target a creature ready to attack
                Permanent targetCreature =
                        // first choice: Syr Faren
                        game.getBattlefield().findFirst(creaturesThatCanBeTapped().and(withName(SYR_FAREN_THE_HENGEHAMMER)))
                                // second choice: any creature ready to attack
                                .orElseGet(() -> game.getBattlefield().findFirst(creatureThatCanAttackOrNettle()).get());
                produce(G);
                game.castInstant(card, G);
                targetCreature.addCounter(PERM_BOOST, aCreatureIsDead ? 3 : 1);
                return true;
            }
            case VINES_OF_VASTWOOD: {
                // target a creature ready to attack
                Permanent targetCreature =
                        // first choice: Syr Faren
                        game.getBattlefield().findFirst(creaturesThatCanBeTapped().and(withName(SYR_FAREN_THE_HENGEHAMMER)))
                                // second choice: any creature ready to attack
                                .orElseGet(() -> game.getBattlefield().findFirst(creatureThatCanAttackOrNettle()).get());
                produce(GG);
                game.castInstant(card, GG);
                targetCreature.addCounter(TEMP_BOOST, 4);
                return true;
            }
        }
        game.log("oops, unsupported card [" + card + "]");
        return false;
    }

    @Override
    public void onEvent(GameEvent event) {
        if (event.getType() == GameEvent.Type.cast) {
            // untap nettles on green spells
            GameEvent.SpellEvent spellEvent = (GameEvent.SpellEvent) event;
            if (spellEvent.getCost().getG() > 0 || spellEvent.getCard().equals(LAND_GRANT)) {
                game.getBattlefield().find(withName(NETTLE_SENTINEL).and(tapped())).forEach(card -> {
                    card.setTapped(false);
                });
            }
        }
    }
}
