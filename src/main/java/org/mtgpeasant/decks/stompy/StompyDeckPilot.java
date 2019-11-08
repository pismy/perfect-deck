package org.mtgpeasant.decks.stompy;

import org.mtgpeasant.perfectdeck.common.Mana;
import org.mtgpeasant.perfectdeck.common.cards.Cards;
import org.mtgpeasant.perfectdeck.common.matchers.MulliganRules;
import org.mtgpeasant.perfectdeck.goldfish.DeckPilot;
import org.mtgpeasant.perfectdeck.goldfish.Game;

import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Optional;

/**
 * TODO:
 * <ul>
 *     <li>force hunger by sacrificing an eldrazi spawn</li>
 *     <li>use Quirion to untap Nettle or mana producers</li>
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

    private static MulliganRules rules;

    // game state
    private int boostCounters = 0;
    private int boostUntilEot = 0;

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
        boostUntilEot = 0;

        super.untapStep();

        // Nettles don't untap as normal
        game.getUntapped(NETTLE_SENTINEL).forEach(card -> game.tap(NETTLE_SENTINEL));
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

        if (game.getPool().ccm() > 0) {
            // still have mana in pool: we should consume before combat phase
            // TODO
            game.log(">> unused pool at end of first main phase: " + game.getPool());
        }
    }

    @Override
    public void combatPhase() {
        // boost all creatures and attack
        Cards creatures = game.getUntapped(CREATURES);
        if (creatures.isEmpty()) {
            return;
        }

        // play hunger if a creature is dead
        if(aCreatureIsDead) {
            while (playOneOf(HUNGER_OF_THE_HOWLPACK).isPresent()) {
            }
            creatures = game.getUntapped(CREATURES); // (a Nettle may have been untapped)
        }

        // attach with all creatures
        creatures.forEach(card -> game.tapForAttack(card, strength(card)));

        // add +1/+1 counters to each attacking creature for each curse in play
        boostCounters += game.getBoard().count(CURSE_OF_PREDATION) * creatures.size();
        if (boostCounters > 0) {
            game.damageOpponent(boostCounters, boostCounters + " +1/+1 counters");
        }

        // add rancors
        game.getBoard().findAll(RANCOR).forEach(card -> {
            game.tap(card);
            game.damageOpponent(2, "rancor");
        });

        // can I kill now with boosts ?
        if (game.getOpponentLife() > 0) {
            boolean canUseQuirion = game.getBoard().contains(QUIRION_RANGER) && game.getBoard().contains(FOREST) && !game.isLanded();
            Mana potentialPool = game.getPool()
                    .plus(Mana.of(
                            0,
                            0,
                            game.countUntapped(FOREST) + (canUseQuirion ? 1 : 0) + game.countUntapped(LLANOWAR_ELVES, FYNDHORN_ELVES),
                            0,
                            0,
                            game.getBoard().count(ELDRAZI_SPAWN))
                    );
            int potentialBoost = 0;
            int countHydra = game.getHand().count(ASPECT_OF_HYDRA);
            int devotion = devotion();
            while (potentialPool.contains(G) && countHydra > 0) {
                potentialBoost += devotion;
                countHydra--;
                potentialPool = potentialPool.minus(G);
            }
            int countVines = game.getHand().count(VINES_OF_VASTWOOD);
            while (potentialPool.contains(GG) && countVines > 0) {
                potentialBoost += 4;
                countVines--;
                potentialPool = potentialPool.minus(GG);
            }
            int countSwipes = game.getHand().count(SAVAGE_SWIPE);
            while (potentialPool.contains(G) && countSwipes > 0) {
                potentialBoost += 2;
                countSwipes--;
                potentialPool = potentialPool.minus(G);
            }
            if (potentialBoost >= game.getOpponentLife()) {
                game.log(">> I can rush now");
                while(playOneOf(ASPECT_OF_HYDRA, VINES_OF_VASTWOOD, SAVAGE_SWIPE).isPresent()) {
                }
            }
        }
        if(boostUntilEot > 0) {
            game.damageOpponent(boostUntilEot, boostUntilEot + " +1/+1 until EOT");
        }

        damageDealtThisTurn = true;
    }

    @Override
    public void secondMainPhase() {
        while(playOneOf(
                // at least one Quirion on board is top priority
                !game.getBoard().contains(QUIRION_RANGER) ? QUIRION_RANGER : "_",
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
        if (game.getHand().size() > 7) {
            discard(game.getHand().size() - 7);
        }
    }

    private void triggerNettle() {
        game.getTapped().findAll(NETTLE_SENTINEL).forEach(card -> game.untap(NETTLE_SENTINEL));
    }

    private int strength(String creature) {
        switch (creature) {
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

    private int devotion(String card) {
        switch (card) {
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
            if (game.getBoard().count(FOREST) + game.getHand().count(FOREST) > 3 && game.discardOneOf(FOREST).isPresent()) {
                continue;
            }
            // discard extra creatures
            if (game.getBoard().count(CREATURES) + game.getHand().count(CREATURES) > 2 && game.discardOneOf(CREATURES).isPresent()) {
                continue;
            }
            // TODO: choose better
            game.discard(game.getHand().getFirst());
        }
    }

    boolean canPay(Mana cost) {
        // potential mana pool is current pool + untapped lands + petals on board
        boolean canUseQuirion = game.getBoard().contains(QUIRION_RANGER) && game.getBoard().contains(FOREST) && !game.isLanded();
        Mana potentialPool = game.getPool()
                .plus(Mana.of(
                        0,
                        0,
                        game.countUntapped(FOREST) + (canUseQuirion ? 1 : 0) + game.countUntapped(LLANOWAR_ELVES, FYNDHORN_ELVES),
                        0,
                        0,
                        game.getBoard().count(ELDRAZI_SPAWN))
                );
        return potentialPool.contains(cost);
    }

    void produce(Mana cost) {
        while (!game.canPay(cost)) {
            Optional<String> land = game.findFirstUntapped(FOREST);
            if (land.isPresent()) {
                game.tapLandForMana(land.get(), G);
            } else if (game.countUntapped(LLANOWAR_ELVES, FYNDHORN_ELVES) > 0) {
                game.tap(game.findFirstUntapped(LLANOWAR_ELVES, FYNDHORN_ELVES).get());
                game.add(G);
            } else if (game.getBoard().contains(QUIRION_RANGER) && game.getBoard().contains(FOREST) && !game.isLanded()) {
                // use Quirion ability
                game.log("- use [Quirion Ranger] ability");
                game.move(FOREST, Game.Area.board, Game.Area.hand);
                game.land(FOREST);
                game.tapLandForMana(FOREST, G);
                // if possible untap a nettle or a mana producer ? TODO (manage summoning sickness)
            } else if (game.getBoard().count(ELDRAZI_SPAWN) > 0) {
                // sacrifice a spawn
                sacrificeASpawn();
            } else {
                // can't preparePool !!!
                return;
            }
        }
    }

    private void sacrificeASpawn() {
        game.sacrifice(ELDRAZI_SPAWN);
        game.add(ONE);
        aCreatureIsDead = true;
    }

    void maybeSacrificeForHunger() {
        if(!aCreatureIsDead && canPay(G) && game.getHand().contains(HUNGER_OF_THE_HOWLPACK) && game.getBoard().contains(ELDRAZI_SPAWN)) {
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
            case RANCOR:
                // need a target creature
                return game.getBoard().count(CREATURES) > 0 && canPay(G);
            case SAVAGE_SWIPE:
                // need a target creature with power 2
                // TODO: not exact (does not take counters into account)
                return game.getUntapped(CREATURES).stream().filter(crea -> strength(crea) == 2).findFirst().isPresent() && canPay(G);
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
                return game.getBoard().count(CREATURES) > 0 && canPay(GG);
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
                game.castNonPermanent(card, Mana.zero());
                game.draw(1);
                return true;
            case VAULT_SKIRGE:
                produce(ONE);
                game.castPermanent(card, ONE);
                game.tap(card); // summoning sickness
                return true;
            case QUIRION_RANGER:
            case LLANOWAR_ELVES:
            case FYNDHORN_ELVES:
            case NETTLE_SENTINEL:
            case YOUNG_WOLF:
                produce(G);
                game.castPermanent(card, G);
                game.tap(card); // summoning sickness
                triggerNettle();
                return true;
            case SKARRGAN_PIT_SKULK:
                produce(G);
                game.castPermanent(card, G);
                game.tap(card); // summoning sickness
                triggerNettle();
                if (damageDealtThisTurn) {
                    boostCounters += 1;
                }
                return true;
            case RANCOR:
                produce(G);
                game.castPermanent(card, G);
                triggerNettle();
                return true;
            case ASPECT_OF_HYDRA:
                produce(G);
                game.castNonPermanent(card, G);
                int devotion = devotion();
                boostUntilEot += devotion;
                triggerNettle();
                return true;
            case SAVAGE_SWIPE:
                produce(G);
                game.castNonPermanent(card, G);
                boostUntilEot += 2;
                triggerNettle();
                return true;
            case HUNGER_OF_THE_HOWLPACK:
                produce(G);
                game.castNonPermanent(card, G);
                boostCounters += aCreatureIsDead ? 3 : 1;
                triggerNettle();
                return true;
            case NEST_INVADER:
                produce(G1);
                game.castPermanent(card, G1);
                game.tap(card); // summoning sickness
                triggerNettle();
                game.getBoard().add(ELDRAZI_SPAWN);
                game.tap(ELDRAZI_SPAWN); // summoning sickness
                return true;
            case SAFEHOLD_ELITE:
            case RIVER_BOA:
            case SILHANA_LEDGEWALKER:
                produce(G1);
                game.castPermanent(card, G1);
                game.tap(card); // summoning sickness
                triggerNettle();
                return true;
            case BURNING_TREE_EMISSARY:
                produce(G1);
                game.castPermanent(card, G1);
                // tap to simulate invocation sickness
                game.tap(card);
                game.add(GR);
                triggerNettle();
                return true;
            case STRANGLEROOT_GEIST:
                produce(GG);
                game.castPermanent(card, GG);
                triggerNettle();
                return true;
            case VINES_OF_VASTWOOD:
                produce(GG);
                game.castNonPermanent(card, GG);
                boostUntilEot += 4;
                triggerNettle();
                return true;
            case CURSE_OF_PREDATION:
                produce(G2);
                game.castPermanent(card, G2);
                triggerNettle();
                return true;
        }
        game.log("oops, unsupported card [" + card + "]");
        return false;
    }
}
