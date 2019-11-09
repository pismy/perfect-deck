package org.mtgpeasant.decks.stompy;

import org.mtgpeasant.perfectdeck.common.Mana;
import org.mtgpeasant.perfectdeck.common.cards.Cards;
import org.mtgpeasant.perfectdeck.common.matchers.MulliganRules;
import org.mtgpeasant.perfectdeck.goldfish.Card;
import org.mtgpeasant.perfectdeck.goldfish.DeckPilot;
import org.mtgpeasant.perfectdeck.goldfish.Game;

import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;
import java.util.Optional;

import static org.mtgpeasant.perfectdeck.goldfish.Card.*;

/**
 * TODO:
 * <ul>
 * <li>force hunger by sacrificing an eldrazi spawn</li>
 * <li>use Quirion to untap Nettle or mana producers</li>
 * </ul>
 */
public class StompyDeckPilot extends DeckPilot<Game> {

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
    private static final String[] CREATURES = {QUIRION_RANGER, NETTLE_SENTINEL, SKARRGAN_PIT_SKULK, VAULT_SKIRGE, NEST_INVADER, BURNING_TREE_EMISSARY, SAFEHOLD_ELITE, SILHANA_LEDGEWALKER, YOUNG_WOLF, RIVER_BOA, STRANGLEROOT_GEIST, LLANOWAR_ELVES, FYNDHORN_ELVES, ELDRAZI_SPAWN};

    // BOOSTS
    private static final String RANCOR = "rancor";
    private static final String VINES_OF_VASTWOOD = "vines of vastwood"; // (instant) GG: +4/+4
    private static final String CURSE_OF_PREDATION = "curse of predation";
    private static final String HUNGER_OF_THE_HOWLPACK = "hunger of the howlpack";
    private static final String ASPECT_OF_HYDRA = "aspect of hydra";
    private static final String SAVAGE_SWIPE = "savage swipe";

    // OTHERS
    private static final String GITAXIAN_PROBE = "gitaxian probe";
    private static final String LAND_GRANT = "land grant";
    private static final String DISMEMBER = "dismember";

    private static final String SUMMONING_SICKNESS_TAG = "sickness";
    private static final String PERM_BOOST = "perm-boost";
    private static final String TEMP_BOOST = "temp-boost";


    private static MulliganRules rules;

    // turn state
    private boolean aCreatureIsDead = false;
    private boolean damageDealtThisTurn = false;


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
        damageDealtThisTurn = false;

        game.untapAll();

        // Nettles don't untap as normal
        game.find(withName(NETTLE_SENTINEL)).forEach(card -> card.setTapped(true));
    }

    @Override
    public void firstMainPhase() {
        while (playOneOf(
                // start by playing all probes
                GITAXIAN_PROBE,
                // then land or land grant
                FOREST,
                LAND_GRANT,
                // then chain burning trees
                BURNING_TREE_EMISSARY,
                // then curse
                CURSE_OF_PREDATION
        ).isPresent()) {

        }

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
                // at least one Quirion on board is top priority
                !game.findFirst(withName(QUIRION_RANGER)).isPresent() ? QUIRION_RANGER : "_",
                LLANOWAR_ELVES,
                FYNDHORN_ELVES,
                // skarrgan if damage dealt (+1/+1)
                damageDealtThisTurn ? SKARRGAN_PIT_SKULK : "_",
                NEST_INVADER,
                // then hunger if a creature is dead
                aCreatureIsDead ? HUNGER_OF_THE_HOWLPACK : "_",
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
        // boost all creatures and attack
        List<Card> creatures = game.find(withType(Game.CardType.creature).and(untapped()).and(notWithTag(SUMMONING_SICKNESS_TAG)));
        if (creatures.isEmpty()) {
            return;
        }

        // play hunger if a creature is dead
        if (aCreatureIsDead) {
            while (playOneOf(HUNGER_OF_THE_HOWLPACK).isPresent()) {
            }
            creatures = game.find(withType(Game.CardType.creature).and(untapped()).and(notWithTag(SUMMONING_SICKNESS_TAG))); // (a Nettle may have been untapped)
        }

        // foresee combat damage
        int curses = game.count(withName(CURSE_OF_PREDATION));
        int combatDamage = creatures.stream().mapToInt(card -> strength(card) + curses).sum();

        // can I kill now with instant boosts ?
        if (combatDamage < game.getOpponentLife()) {
            boolean canUseQuirion = game.findFirst(withName(QUIRION_RANGER)).isPresent() && game.findFirst(withName(FOREST)).isPresent() && !game.isLanded();
            Mana potentialPool = game.getPool()
                    .plus(Mana.of(
                            0,
                            0,
                            game.count(withName(FOREST).and(untapped())) + (canUseQuirion ? 1 : 0) + game.count(withName(LLANOWAR_ELVES, FYNDHORN_ELVES).and(untapped())),
                            0,
                            0,
                            game.count(withName(ELDRAZI_SPAWN)))
                    );
            int instantBoosts = 0;

            int countHydra = game.getHand().count(ASPECT_OF_HYDRA);
            int devotion = devotion();
            while (potentialPool.contains(G) && countHydra > 0) {
                instantBoosts += devotion;
                countHydra--;
                potentialPool = potentialPool.minus(G);
            }
            int countVines = game.getHand().count(VINES_OF_VASTWOOD);
            while (potentialPool.contains(GG) && countVines > 0) {
                instantBoosts += 4;
                countVines--;
                potentialPool = potentialPool.minus(GG);
            }
            int countSwipes = game.getHand().count(SAVAGE_SWIPE);
            while (potentialPool.contains(G) && countSwipes > 0) {
                instantBoosts += 2;
                countSwipes--;
                potentialPool = potentialPool.minus(G);
            }
            if (combatDamage + instantBoosts >= game.getOpponentLife()) {
                game.log(">> I can rush now");
                while (playOneOf(ASPECT_OF_HYDRA, VINES_OF_VASTWOOD, SAVAGE_SWIPE).isPresent()) {
                }
            }
        }

        // attack with all creatures
        creatures.forEach(card -> {
            // add one +1/+1 counter per curse
            card.addCounter(PERM_BOOST, curses);
            game.tapForAttack(card, strength(card));
        });

        damageDealtThisTurn = true;
    }

    @Override
    public void secondMainPhase() {
        while (playOneOf(
                // at least one Quirion on board is top priority
                !game.findFirst(withName(QUIRION_RANGER)).isPresent() ? QUIRION_RANGER : "_",
                LLANOWAR_ELVES,
                FYNDHORN_ELVES,
                // skarrgan if damage dealt (+1/+1)
                damageDealtThisTurn ? SKARRGAN_PIT_SKULK : "_",
                NEST_INVADER,
                // then hunger if a creature is dead
                aCreatureIsDead ? HUNGER_OF_THE_HOWLPACK : "_",
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
        // reset temporary boosts and summoning sickness
        game.find(withType(Game.CardType.creature)).forEach(card -> {
            card.getCounters().remove(TEMP_BOOST);
            card.getTags().remove(SUMMONING_SICKNESS_TAG);
        });

        if (game.getHand().size() > 7) {
            discard(game.getHand().size() - 7);
        }
    }

    private void triggerNettle() {
        game.find(withName(NETTLE_SENTINEL).and(tapped())).forEach(card -> card.setTapped(false));
    }

    private int strength(Card creature) {
        // strength is base strength + +1/1 counters + temporary boosts + 2 * rancors
        return baseStrength(creature)
                + creature.getCounter(PERM_BOOST)
                + creature.getCounter(TEMP_BOOST)
                + 2 * creature.getCounter(RANCOR);
    }

    private int baseStrength(Card creature) {
        switch (creature.getName()) {
            case ELDRAZI_SPAWN:
                return 0;
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
                return 2;

            case STRANGLEROOT_GEIST:
                return 3;
            default:
                return 0;
        }
    }

    private int devotion() {
        return game.getBoard().stream().mapToInt(this::devotion).sum();
    }

    private int devotion(Card card) {
        switch (card.getName()) {
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
            if (game.count(withName(FOREST)) + game.getHand().count(FOREST) > 3 && game.discardOneOf(FOREST).isPresent()) {
                continue;
            }
            // discard extra creatures
            if (game.count(withName(CREATURES)) + game.getHand().count(CREATURES) > 2 && game.discardOneOf(CREATURES).isPresent()) {
                continue;
            }
            // TODO: choose better
            game.discard(game.getHand().getFirst());
        }
    }

    boolean canPay(Mana cost) {
        // potential mana pool is current pool + untapped lands + petals on board
        boolean canUseQuirion = game.findFirst(withName(QUIRION_RANGER)).isPresent() && game.findFirst(withName(FOREST)).isPresent() && !game.isLanded();
        Mana potentialPool = game.getPool()
                .plus(Mana.of(
                        0,
                        0,
                        game.count(withName(FOREST).and(untapped())) + (canUseQuirion ? 1 : 0) + game.count(withName(LLANOWAR_ELVES, FYNDHORN_ELVES).and(untapped())),
                        0,
                        0,
                        game.count(withName(ELDRAZI_SPAWN)))
                );
        return potentialPool.contains(cost);
    }

    void produce(Mana cost) {
        while (!game.canPay(cost)) {
            Optional<Card> land = game.findFirst(withName(FOREST).and(untapped()));
            if (land.isPresent()) {
                game.tapLandForMana(land.get(), G);
            } else if (game.count(withName(LLANOWAR_ELVES, FYNDHORN_ELVES).and(untapped())) > 0) {
                game.tap(game.findFirst(withName(LLANOWAR_ELVES, FYNDHORN_ELVES).and(untapped())).get());
                game.add(G);
            } else if (game.findFirst(withName(QUIRION_RANGER)).isPresent() && game.findFirst(withName(FOREST)).isPresent() && !game.isLanded()) {
                // use Quirion ability
                game.log("- use [Quirion Ranger] ability");
                game.move(FOREST, Game.Area.board, Game.Area.hand);
                Card forest = game.land(FOREST);
                game.tapLandForMana(forest, G);
                // if possible untap a nettle or a mana producer ? TODO (manage summoning sickness)
            } else if (game.count(withName(ELDRAZI_SPAWN)) > 0) {
                // sacrifice a spawn
                sacrificeASpawn();
            } else {
                // can't preparePool !!!
                return;
            }
        }
    }

    private void sacrificeASpawn() {
        game.findFirst(withName(ELDRAZI_SPAWN)).ifPresent(card -> {
            game.sacrifice(card);
            game.add(ONE);
            aCreatureIsDead = true;
        });
    }

    void maybeSacrificeForHunger() {
        if (!aCreatureIsDead && canPay(G) && game.getHand().contains(HUNGER_OF_THE_HOWLPACK) && game.findFirst(withName(ELDRAZI_SPAWN)).isPresent()) {
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
            if (game.getHand().contains(card) && canPlay(card)) {
                play(card);
                return Optional.of(card);
            }
        }
        return Optional.empty();
    }

    boolean canPlay(String card) {
        switch (card) {
            case FOREST:
                return !game.isLanded();
            case LAND_GRANT:
                return game.getHand().count(FOREST) == 0;
            case GITAXIAN_PROBE:
                return true;
            case VAULT_SKIRGE:
                return canPay(ONE);
            case QUIRION_RANGER:
            case LLANOWAR_ELVES:
            case FYNDHORN_ELVES:
            case SKARRGAN_PIT_SKULK:
            case NETTLE_SENTINEL:
            case YOUNG_WOLF:
                return canPay(G);
            case ASPECT_OF_HYDRA:
            case HUNGER_OF_THE_HOWLPACK:
                // need an untapped target creature
                return game.findFirst(withType(Game.CardType.creature)
                        .and(untapped())
                        .and(notWithTag(SUMMONING_SICKNESS_TAG))
                ).isPresent() && canPay(G);
            case RANCOR:
                // need a target creature (any)
                return game.findFirst(withType(Game.CardType.creature)).isPresent() && canPay(G);
            case SAVAGE_SWIPE:
                // need a target creature with power 2
                return game.findFirst(withType(Game.CardType.creature)
                        .and(untapped())
                        .and(notWithTag(SUMMONING_SICKNESS_TAG))
                        .and(crea -> strength(crea) == 2)
                ).isPresent()
                        && canPay(G);
            case NEST_INVADER:
            case SAFEHOLD_ELITE:
            case RIVER_BOA:
            case SILHANA_LEDGEWALKER:
                return canPay(G1);
            case BURNING_TREE_EMISSARY:
                return canPay(G1); // TODO: not quite exact
            case STRANGLEROOT_GEIST:
                return canPay(GG);
            case VINES_OF_VASTWOOD:
                // need a target creature
                return game.count(withName(CREATURES)) > 0 && canPay(GG);
            case CURSE_OF_PREDATION:
                return canPay(G2);
        }
        game.log("oops, unsupported card [" + card + "]");
        return false;
    }

    boolean play(String card) {
        switch (card) {
            case FOREST:
                game.land(card);
                return true;
            case LAND_GRANT:
                // put a forest from library to the board
                game.discard(card);
                game.move(FOREST, Game.Area.library, Game.Area.hand);
                triggerNettle();
                return true;
            case GITAXIAN_PROBE:
                game.castSorcery(card, Mana.zero());
                game.draw(1);
                return true;
            case VAULT_SKIRGE:
                produce(ONE);
                game.castCreature(card, ONE).tag(SUMMONING_SICKNESS_TAG);
                return true;
            case QUIRION_RANGER:
            case LLANOWAR_ELVES:
            case FYNDHORN_ELVES:
            case NETTLE_SENTINEL:
            case YOUNG_WOLF:
                produce(G);
                game.castCreature(card, G).tag(SUMMONING_SICKNESS_TAG);
                triggerNettle();
                return true;
            case SKARRGAN_PIT_SKULK:
                produce(G);
                Card crea = game.castCreature(card, G).tag(SUMMONING_SICKNESS_TAG);
                if (damageDealtThisTurn) {
                    crea.addCounter(PERM_BOOST, 1);
                }
                triggerNettle();
                return true;
            case RANCOR: {
                // target preferably a creature ready to attack, or else any creature
                Card targetCreature = game.findFirst(withType(Game.CardType.creature)
                        .and(untapped())
                        .and(notWithTag(SUMMONING_SICKNESS_TAG)))
                        .orElseGet(() -> game.findFirst(withType(Game.CardType.creature)).get());
                produce(G);
                game.castEnchantment(card, G).tag("on:" + targetCreature.getName());
                targetCreature.incrCounter(RANCOR);
                triggerNettle();
                return true;
            }
            case ASPECT_OF_HYDRA: {
                // target a creature ready to attack
                Card targetCreature = game.findFirst(withType(Game.CardType.creature)
                        .and(untapped())
                        .and(notWithTag(SUMMONING_SICKNESS_TAG))).get();
                produce(G);
                game.castInstant(card, G);
                int devotion = devotion();
                targetCreature.addCounter(TEMP_BOOST, devotion);
                triggerNettle();
                return true;
            }
            case SAVAGE_SWIPE: {
                // target a creature ready to attack with power 2
                Card targetCreature = game.findFirst(withType(Game.CardType.creature)
                        .and(untapped())
                        .and(notWithTag(SUMMONING_SICKNESS_TAG))
                        .and(c -> strength(c) == 2)
                ).get();
                produce(G);
                game.castSorcery(card, G);
                targetCreature.addCounter(TEMP_BOOST, 2);
                triggerNettle();
                return true;
            }
            case HUNGER_OF_THE_HOWLPACK: {
                // target a creature ready to attack
                Card targetCreature = game.findFirst(withType(Game.CardType.creature)
                        .and(untapped())
                        .and(notWithTag(SUMMONING_SICKNESS_TAG))).get();
                produce(G);
                game.castInstant(card, G);
                targetCreature.addCounter(PERM_BOOST, aCreatureIsDead ? 3 : 1);
                triggerNettle();
                return true;
            }
            case VINES_OF_VASTWOOD: {
                // target a creature ready to attack
                Card targetCreature = game.findFirst(withType(Game.CardType.creature)
                        .and(untapped())
                        .and(notWithTag(SUMMONING_SICKNESS_TAG))).get();
                produce(GG);
                game.castInstant(card, GG);
                targetCreature.addCounter(TEMP_BOOST, 4);
                triggerNettle();
                return true;
            }
            case NEST_INVADER: {
                produce(G1);
                game.castCreature(card, G1).tag(SUMMONING_SICKNESS_TAG);
                triggerNettle();
                game.createToken(ELDRAZI_SPAWN, Game.CardType.creature).tag(SUMMONING_SICKNESS_TAG);
                return true;
            }
            case SAFEHOLD_ELITE:
            case RIVER_BOA:
            case SILHANA_LEDGEWALKER:
                produce(G1);
                game.castCreature(card, G1).tag(SUMMONING_SICKNESS_TAG);
                triggerNettle();
                return true;
            case BURNING_TREE_EMISSARY:
                produce(G1);
                game.castCreature(card, G1).tag(SUMMONING_SICKNESS_TAG);
                game.add(GR);
                triggerNettle();
                return true;
            case STRANGLEROOT_GEIST:
                produce(GG);
                game.castCreature(card, GG);
                triggerNettle();
                return true;
            case CURSE_OF_PREDATION:
                produce(G2);
                game.castEnchantment(card, G2);
                triggerNettle();
                return true;
        }
        game.log("oops, unsupported card [" + card + "]");
        return false;
    }
}
