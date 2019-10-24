package org.mtgpeasant.decks.stompy;

import org.mtgpeasant.perfectdeck.common.Mana;
import org.mtgpeasant.perfectdeck.common.cards.Cards;
import org.mtgpeasant.perfectdeck.common.matchers.MulliganRules;
import org.mtgpeasant.perfectdeck.goldfish.DeckPilot;
import org.mtgpeasant.perfectdeck.goldfish.Game;

import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Optional;


public class StompyDeckPilot extends DeckPilot<Game> {

    private static final Mana ONE = Mana.of("1");
    public static final Mana G = Mana.of("G");
    public static final Mana G1 = Mana.of("1G");
    public static final Mana G2 = Mana.of("2G");
    public static final Mana GG = Mana.of("GG");
    public static final Mana TWO = Mana.of("2");

    // LANDS
    public static final String FOREST = "forest";

    // CREATURES
    public static final String QUIRION_RANGER = "quirion ranger";
    public static final String NETTLE_SENTINEL = "nettle sentinel";
    public static final String SKARRGAN_PIT_SKULK = "skarrgan pit-skulk";
    public static final String VAULT_SKIRGE = "vault skirge";
    public static final String NEST_INVADER = "nest invader";
    public static final String BURNING_TREE_EMISSARY = "burning-tree emissary";
    public static final String SAFEHOLD_ELITE = "safehold elite";
    public static final String SILHANA_LEDGEWALKER = "silhana ledgewalker";
    public static final String YOUNG_WOLF = "young wolf";
    public static final String RIVER_BOA = "river boa";
    public static final String STRANGLEROOT_GEIST = "strangleroot geist";
    public static final String LLANOWAR_ELVES = "llanowar elves";
    public static final String FYNDHORN_ELVES = "fyndhorn elves";
    public static final String ELDRAZI_SPAWN = "eldrazi spawn";
    private static final String[] CREATURES = {QUIRION_RANGER, NETTLE_SENTINEL, SKARRGAN_PIT_SKULK, VAULT_SKIRGE, NEST_INVADER, BURNING_TREE_EMISSARY, SAFEHOLD_ELITE, SILHANA_LEDGEWALKER, YOUNG_WOLF, RIVER_BOA, STRANGLEROOT_GEIST, LLANOWAR_ELVES, FYNDHORN_ELVES, ELDRAZI_SPAWN};

    // BOOSTS
    public static final String RANCOR = "rancor";
    public static final String VINES_OF_VASTWOOD = "vines of vastwood"; // (instant) GG: +4/+4
    public static final String CURSE_OF_PREDATION = "curse of predation";
    public static final String HUNGER_OF_THE_HOWLPACK = "hunger of the howlpack";
    public static final String ASPECT_OF_HYDRA = "aspect of hydra";
    public static final String SAVAGE_SWIPE = "savage swipe";

    // OTHERS
    public static final String GITAXIAN_PROBE = "gitaxian probe";
    public static final String LAND_GRANT = "land grant";
    public static final String DISMEMBER = "dismember";

    private static MulliganRules rules;

    // game state
    private int boostCounters = 0;

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

        super.untapStep();

        // Nettles don't untap as normal
        game.getUntapped(NETTLE_SENTINEL).forEach(card -> game.tap(NETTLE_SENTINEL));
    }

    @Override
    public void firstMainPhase() {
        // whichever the situation, if I have a probe in hand: play it
        while (game.getHand().contains(GITAXIAN_PROBE)) {
            game.castNonPermanent(GITAXIAN_PROBE, Mana.zero()).draw(1);
        }

        // land
        if (game.getHand().contains(FOREST)) {
            game.land(FOREST);
        } else if (game.getHand().contains(LAND_GRANT)) {
            // put a forest from library to the board
            game.discard(LAND_GRANT);
            game.move(FOREST, Game.Area.library, Game.Area.hand);
            game.land(FOREST);
            triggerNettle();
        }

        // 1st burning tree
        while (game.getHand().contains(BURNING_TREE_EMISSARY) && canPay(G1)) {
            produce(G1);
            game.castPermanent(BURNING_TREE_EMISSARY, G1)
                    // tap to simulate invocation sickness
                    .tap(BURNING_TREE_EMISSARY)
                    .add(G1);
            triggerNettle();
        }

        // then curse
        while (game.getHand().contains(CURSE_OF_PREDATION) && canPay(G2)) {
            produce(G2);
            game.castPermanent(CURSE_OF_PREDATION, G2);
            triggerNettle();
        }

        // then rancors
        while (game.getHand().contains(RANCOR) && game.getBoard().count(CREATURES) > 0 && canPay(G)) {
            produce(G);
            game.castPermanent(RANCOR, G);
            triggerNettle();
        }

        while (game.getHand().contains(STRANGLEROOT_GEIST) && canPay(GG)) {
            produce(GG);
            game.castPermanent(STRANGLEROOT_GEIST, GG);
            triggerNettle();
        }

        if (game.getPool().ccm() > 0) {
            // still have mana in pool: we must consume before combat phase
            // TODO
            game.log(">> unused pool at end of first main phase: " + game.getPool());
        }
    }

    private void triggerNettle() {
        game.getTapped().findAll(NETTLE_SENTINEL).forEach(card -> game.untap(NETTLE_SENTINEL));
    }

    @Override
    public void combatPhase() {
        // boost all creatures and attack
        Cards creatures = game.getUntapped(CREATURES);
        if (creatures.isEmpty()) {
            return;
        }

        // play hunger
        while (aCreatureIsDead && game.getHand().contains(HUNGER_OF_THE_HOWLPACK) && canPay(G)) {
            produce(G);
            game.castNonPermanent(HUNGER_OF_THE_HOWLPACK, G);
            boostCounters += 3;
            triggerNettle();
            creatures = game.getUntapped(CREATURES);
        }

        // attach with all creatures
        creatures.forEach(card -> game.tapForAttack(card, strength(card)));

        // add +1/+1 counters to each attacking creature for each curse in play
        boostCounters += game.getBoard().count(CURSE_OF_PREDATION) * creatures.size();
        if (boostCounters > 0) {
            game.damageOpponent(boostCounters, boostCounters + " +1/+1 counters");
        }

        // add rancors
        game.getBoard().findAll(RANCOR).forEach(card -> game.tap(card).damageOpponent(2, "rancor"));

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
            if (potentialBoost >= game.getOpponentLife()) {
                game.log(">> I can rush now");
                while (game.getHand().contains(ASPECT_OF_HYDRA) && canPay(G)) {
                    produce(G);
                    game.castNonPermanent(ASPECT_OF_HYDRA, G)
                            .damageOpponent(devotion, "aspect of hydra boost");
                    triggerNettle();
                }
                while (game.getHand().contains(VINES_OF_VASTWOOD) && canPay(GG)) {
                    produce(GG);
                    game.castNonPermanent(VINES_OF_VASTWOOD, GG)
                            .damageOpponent(4, "vines boost");
                    triggerNettle();
                }
            }
        }

        damageDealtThisTurn = true;
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

    @Override
    public void secondMainPhase() {
        // one Quirion on board is top priority
        if (!game.getBoard().contains(QUIRION_RANGER) && game.getHand().contains(QUIRION_RANGER) && canPay(G)) {
            produce(G);
            game.castPermanent(QUIRION_RANGER, G);
            triggerNettle();
        }
        while (game.getHand().contains(LLANOWAR_ELVES) && canPay(G)) {
            produce(G);
            game.castPermanent(LLANOWAR_ELVES, G);
            triggerNettle();
        }
        while (game.getHand().contains(FYNDHORN_ELVES) && canPay(G)) {
            produce(G);
            game.castPermanent(FYNDHORN_ELVES, G);
            triggerNettle();
        }
        while (damageDealtThisTurn && game.getHand().contains(SKARRGAN_PIT_SKULK) && canPay(G)) {
            produce(G);
            game.castPermanent(SKARRGAN_PIT_SKULK, G);
            triggerNettle();
            boostCounters++;
        }
        while (game.getHand().contains(NEST_INVADER) && canPay(G1)) {
            produce(G1);
            game.castPermanent(NEST_INVADER, G1);
            triggerNettle();
            game.getBoard().add(ELDRAZI_SPAWN);
        }
        while (game.getHand().contains(SAFEHOLD_ELITE) && canPay(G1)) {
            produce(G1);
            game.castPermanent(SAFEHOLD_ELITE, G1);
            triggerNettle();
        }
        while (game.getHand().contains(NETTLE_SENTINEL) && canPay(G)) {
            produce(G);
            game.castPermanent(NETTLE_SENTINEL, G);
            triggerNettle();
        }
        while (game.getHand().contains(RIVER_BOA) && canPay(G1)) {
            produce(G1);
            game.castPermanent(RIVER_BOA, G1);
            triggerNettle();
        }
        while (game.getHand().contains(SILHANA_LEDGEWALKER) && canPay(G1)) {
            produce(G1);
            game.castPermanent(SILHANA_LEDGEWALKER, G1);
            triggerNettle();
        }
        while (game.getHand().contains(QUIRION_RANGER) && canPay(G)) {
            produce(G);
            game.castPermanent(QUIRION_RANGER, G);
            triggerNettle();
        }
        while (game.getHand().contains(VAULT_SKIRGE) && canPay(ONE)) {
            produce(ONE);
            game.castPermanent(VAULT_SKIRGE, ONE);
        }
        while (game.getHand().contains(SKARRGAN_PIT_SKULK) && canPay(G)) {
            produce(G);
            game.castPermanent(SKARRGAN_PIT_SKULK, G);
            triggerNettle();
        }
    }

    @Override
    public void endingPhase() {
        if (game.getHand().size() > 7) {
            discard(game.getHand().size() - 7);
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
                game.tap(game.findFirstUntapped(LLANOWAR_ELVES, FYNDHORN_ELVES).get()).add(G);
            } else if (game.getBoard().contains(QUIRION_RANGER) && game.getBoard().contains(FOREST) && !game.isLanded()) {
                // use Quirion ability
                game.log(" - use [Quirion Ranger] ability");
                game.move(FOREST, Game.Area.board, Game.Area.hand);
                game.land(FOREST);
                game.tapLandForMana(FOREST, G);
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
        game.sacrifice(ELDRAZI_SPAWN).add(ONE);
        aCreatureIsDead = true;
    }
}
