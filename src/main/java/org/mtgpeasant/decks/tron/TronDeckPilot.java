package org.mtgpeasant.decks.tron;

import org.mtgpeasant.perfectdeck.common.cards.Cards;
import org.mtgpeasant.perfectdeck.common.mana.Mana;
import org.mtgpeasant.perfectdeck.goldfish.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static java.util.Collections.singleton;
import static org.mtgpeasant.perfectdeck.common.mana.Mana.*;
import static org.mtgpeasant.perfectdeck.goldfish.ManaSource.getTapSources;
import static org.mtgpeasant.perfectdeck.goldfish.ManaSource.oneOf;
import static org.mtgpeasant.perfectdeck.goldfish.Permanent.*;

public class TronDeckPilot extends DeckPilot<Game> implements Seer.SpellsPlayer {

    public static final Mana TWO = Mana.of("2");
    public static final Mana THREE = Mana.of("3");
    public static final Mana ONE = Mana.of("1");
    public static final String SNOW_TAG = "*";
    public static final Mana U1 = Mana.of("1U");
    public static final Mana U2 = Mana.of("2U");
    public static final Mana B6 = Mana.of("6B");

    enum Strategy {
        tron,
        u1,
        g1
    }

    final Strategy strategy = Strategy.tron;

    // LANDS
    public static final String CAVE_OF_TEMPTATION = "cave of temptation";
    public static final String UNKNOWN_SHORES = "unknown shores";
    public static final String THORNWOOD_FALLS = "Thornwood Falls";
    public static final String DISMAL_BACKWATER = "dismal backwater";
    public static final String SWIFTWATER_CLIFFS = "swiftwater cliffs";
    public static final String URZA_S_MINE = "urza's mine";
    public static final String URZA_S_POWER_PLANT = "urza's power plant";
    public static final String URZA_S_TOWER = "urza's tower";
    public static final String SNOW_COVERED_ISLAND = "snow-covered island";
    public static final String SNOW_COVERED_MOUNTAIN = "snow-covered mountain";
    public static final String BOJUKA_BOG = "bojuka bog";
    public static final String MORTUARY_MIRE = "mortuary mire";

    // converters
    public static final String NAVIGATOR_S_COMPASS = "navigator's compass";
    public static final String PROPHETIC_PRISM = "prophetic prism";
    public static final String ARCUM_S_ASTROLABE = "arcum's astrolabe";

    // land tutors
    public static final String EXPEDITION_MAP = "expedition map";
    public static final String CROP_ROTATION = "crop rotation";

    // diggers
    //    public static final String MYSTICAL_TEACHINGS = "mystical teachings";
    public static final String IMPULSE = "impulse";
    public static final String FORBIDDEN_ALCHEMY = "forbidden alchemy";
    public static final String FORBIDDEN_ALCHEMY_FB = "forbidden alchemy (flashback)";

    public TronDeckPilot(Game game) {
        super(game);
    }

    @Override
    public boolean keepHand(Cards hand) {
        if (game.getMulligans() >= 3) {
            return true;
        }
//        return rules.firstMatch(hand).isPresent();
        return true;
    }

    @Override
    public void start() {
        putOnBottomOfLibrary(game.getMulligans());
    }

    @Override
    public void firstMainPhase() {
//        while (playOneOf(
//                // start by playing all probes
//                GITAXIAN_PROBE,
//                // then land or land grant
//                FOREST,
//                LAND_GRANT
//        ).isPresent()) {
//        }
//
//        // simulate if I can rush now
//        if (game.getCurrentTurn() > 2) {
//            Optional<Seer.VictoryRoute> victoryRoute = Seer.findRouteToVictory(this, RUSH);
//            if (victoryRoute.isPresent()) {
//                game.log(">> I can rush now with: " + victoryRoute);
//                maybeSacrificeForHunger();
//                victoryRoute.get().play(this);
//            }
//        }
//
//        while (playOneOf(
//                // chain burning trees
//                BURNING_TREE_EMISSARY,
//                // then curse
//                CURSE_OF_PREDATION
//        ).isPresent()) {
//        }
//
//        /*
//         * what to do now ?
//         * - play a green spell to untap Nettle without sickness
//         * - sac a spawn before casting a hunger
//         * - attack before casting any Skarrgan
//         * - consume any mana still in the pool (due to emissary)
//         * // TODO
//         */
//
//        maybeSacrificeForHunger();
//
//        while (playOneOf(
//                // then hunger if a creature is dead
//                aCreatureIsDead ? HUNGER_OF_THE_HOWLPACK : "_",
//                // then rancor
//                RANCOR,
//                // then haste
//                STRANGLEROOT_GEIST
//        ).isPresent()) {
//
//        }
//
//        // TODO: play more (for e.g. to untap nettle before combat)
//        while (playOneOf(
//                // at least one Quirion on battlefield is top priority
//                !game.getBattlefield().findFirst(withName(QUIRION_RANGER)).isPresent() ? QUIRION_RANGER : "_",
//                LLANOWAR_ELVES,
//                FYNDHORN_ELVES,
//                // skarrgan if damage dealt (+1/+1)
//                game.getDamageDealtThisTurn() > 0 ? SKARRGAN_PIT_SKULK : "_",
//                NEST_INVADER,
//                // then hunger if a creature is dead
//                aCreatureIsDead ? HUNGER_OF_THE_HOWLPACK : "_",
//                SYR_FAREN_THE_HENGEHAMMER,
//                SAFEHOLD_ELITE,
//                NETTLE_SENTINEL,
//                RIVER_BOA,
//                SILHANA_LEDGEWALKER,
//                QUIRION_RANGER,
//                VAULT_SKIRGE,
//                SKARRGAN_PIT_SKULK,
//                YOUNG_WOLF,
//                GINGERBRUTE
//        ).isPresent()) {
//
//        }

        if (game.getPool().ccm() > 0) {
            // still have mana in pool: we should consume before combat phase
            // TODO
            game.log(">> unused pool at end of first main phase: " + game.getPool());
        }
    }

    @Override
    public void endingPhase() {
        // discard to 7
        if (game.getHand().size() > 7) {
            discard(game.getHand().size() - 7);
        }
    }

    @Override
    public String checkWin() {
        switch (strategy) {
            case u1:
                return canPay(U1) ? "can pay 1U" : null;
            case g1:
                return canPay(Mana.of("1G")) ? "can pay 1G" : null;
            default:
                return hasTron() ? "tron assembled" : null;
        }
    }

    private void discard(int number) {
    }

    void putOnBottomOfLibrary(int number) {
//        for (int i = 0; i < number; i++) {
//            if (game.putOnBottomOfLibraryOneOf(DISMEMBER).isPresent()) {
//                continue;
//            }
//            // discard extra lands
//            if (game.getHand().count(FOREST) > 2 && game.putOnBottomOfLibraryOneOf(FOREST).isPresent()) {
//                continue;
//            }
//            // discard extra creatures
//            if (game.getHand().count(CREATURES) > 2 && game.putOnBottomOfLibraryOneOf(CREATURES).isPresent()) {
//                continue;
//            }
//            // TODO: choose better
//            game.putOnBottomOfLibrary(game.getHand().getFirst());
//        }
    }

    boolean canPay(Mana cost) {
//        // potential mana pool is current pool + untapped lands + petals on battlefield
//        boolean canUseQuirion = game.getBattlefield().findFirst(withName(QUIRION_RANGER)).isPresent() && game.getBattlefield().findFirst(withName(FOREST)).isPresent() && !game.isLanded();
//        Mana potentialPool = game.getPool()
//                .plus(Mana.of(
//                        0,
//                        0,
//                        game.getBattlefield().count(withName(FOREST).and(untapped())) + (canUseQuirion ? 1 : 0) + game.getBattlefield().count(withName(LLANOWAR_ELVES, FYNDHORN_ELVES).and(untapped())),
//                        0,
//                        0,
//                        game.getBattlefield().count(withName(ELDRAZI_SPAWN)))
//                );
//        return potentialPool.contains(cost);
        return false;
    }

    void produce(Mana cost) {
//        boolean hasTron = hasTron();
//        while (!game.canPay(cost)) {
//            Optional<Permanent> land = game.getBattlefield().findFirst(withName(FOREST).and(untapped()));
//            if (land.isPresent()) {
//                game.tapLandForMana(land.get(), G);
//            } else if (game.getBattlefield().count(withName(LLANOWAR_ELVES, FYNDHORN_ELVES).and(untapped())) > 0) {
//                game.tap(game.getBattlefield().findFirst(withName(LLANOWAR_ELVES, FYNDHORN_ELVES).and(untapped())).get());
//                game.add(G);
//            } else if (game.getBattlefield().findFirst(withName(QUIRION_RANGER)).isPresent() && game.getBattlefield().findFirst(withName(FOREST)).isPresent() && !game.isLanded()) {
//                // use Quirion ability
//                game.log("use [Quirion Ranger] ability");
//                game.move(FOREST, Game.Area.battlefield, Game.Area.hand);
//                Permanent forest = game.land(FOREST);
//                game.tapLandForMana(forest, G);
//                // if possible untap a nettle or a mana producer ? TODO (manage summoning sickness)
//            } else if (game.getBattlefield().count(withName(ELDRAZI_SPAWN)) > 0) {
//                // sacrifice a spawn
//                sacrificeASpawn();
//            } else {
//                // can't preparePool !!!
//                return;
//            }
//        }
    }

    private boolean hasTron() {
        return game.getBattlefield().findFirst(withName(URZA_S_MINE)).isPresent()
                && game.getBattlefield().findFirst(withName(URZA_S_POWER_PLANT)).isPresent()
                && game.getBattlefield().findFirst(withName(URZA_S_TOWER)).isPresent();
    }

    boolean maybeProduce(Mana cost) {
        boolean hasTron = hasTron();
        List<ManaSource> sources = new ArrayList<>();
        sources.addAll(getTapSources(game, URZA_S_MINE, zero(), singleton(hasTron ? TWO : ONE)));
        sources.addAll(getTapSources(game, URZA_S_POWER_PLANT, zero(), singleton(hasTron ? TWO : ONE)));
        sources.addAll(getTapSources(game, URZA_S_TOWER, zero(), singleton(hasTron ? THREE : ONE)));

        sources.addAll(getTapSources(game, BOJUKA_BOG, zero(), singleton(B())));
        sources.addAll(getTapSources(game, MORTUARY_MIRE, zero(), singleton(B())));

        sources.addAll(getTapSources(game, SNOW_COVERED_ISLAND, zero(), singleton(U())));
        sources.addAll(getTapSources(game, SNOW_COVERED_MOUNTAIN, zero(), singleton(R())));

        sources.addAll(getTapSources(game, SWIFTWATER_CLIFFS, zero(), oneOf(U(), Mana.R())));
        sources.addAll(getTapSources(game, THORNWOOD_FALLS, zero(), oneOf(U(), Mana.G())));
        sources.addAll(getTapSources(game, DISMAL_BACKWATER, zero(), oneOf(U(), Mana.B())));

        sources.addAll(getTapSources(game, PROPHETIC_PRISM, one(), oneOf(B(), R(), G(), W(), U())));
        sources.addAll(getTapSources(game, ARCUM_S_ASTROLABE, one(), oneOf(B(), R(), G(), W(), U())));

        sources.addAll(getTapSources(game, UNKNOWN_SHORES, zero(), singleton(ONE)));
        sources.addAll(getTapSources(game, CAVE_OF_TEMPTATION, zero(), singleton(ONE)));

        sources.addAll(getTapSources(game, UNKNOWN_SHORES, one(), oneOf(B(), R(), G(), W(), U())));
        sources.addAll(getTapSources(game, CAVE_OF_TEMPTATION, one(), oneOf(B(), R(), G(), W(), U())));

        // TODO: manage navigator's compass
//        sources.addAll(getTapSources(game, NAVIGATOR_S_COMPASS, one(), oneOf(B(), R(), G(), W(), U())));

        return ManaProductionPlanner.maybeProduce(game, sources, cost);
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

    @Override
    public boolean canPlay(String card) {
        switch (card) {
            case FORBIDDEN_ALCHEMY_FB:
                if (!game.getGraveyard().contains(FORBIDDEN_ALCHEMY)) {
                    return false;
                }
                break;
            default:
                // for all other cards: must be in hand
                if (!game.getHand().contains(card)) {
                    return false;
                }
                break;
        }
        switch (card) {
            case URZA_S_MINE:
            case URZA_S_POWER_PLANT:
            case URZA_S_TOWER:
            case DISMAL_BACKWATER:
            case SWIFTWATER_CLIFFS:
            case THORNWOOD_FALLS:
            case SNOW_COVERED_ISLAND:
            case SNOW_COVERED_MOUNTAIN:
            case BOJUKA_BOG:
            case MORTUARY_MIRE:
                return !game.isLanded();

            // stones
            case NAVIGATOR_S_COMPASS:
                return canPay(ONE);
            case PROPHETIC_PRISM:
                return canPay(TWO);
            case ARCUM_S_ASTROLABE:
                return canPay(ONE) && game.getBattlefield().findFirst(untapped().and(withTag(SNOW_TAG))).isPresent();

            // land tutors
            case EXPEDITION_MAP:
                return canPay(ONE);
            case CROP_ROTATION:
                return canPay(G()) && getLandToSac().isPresent();

            // diggers
            case IMPULSE:
                return canPay(U1);
            case FORBIDDEN_ALCHEMY:
                return canPay(U2);
            case FORBIDDEN_ALCHEMY_FB:
                return canPay(B6);
        }
//        game.log("oops, unsupported card [" + card + "]");
        return false;
    }

    private Optional<Object> getLandToSac() {
        return Optional.empty(); // TODO
    }

    @Override
    public boolean play(String card) {
        switch (card) {
            case URZA_S_MINE:
            case URZA_S_POWER_PLANT:
            case URZA_S_TOWER:
            case DISMAL_BACKWATER:
            case SWIFTWATER_CLIFFS:
            case THORNWOOD_FALLS:
                game.land(card);
                return true;
            case SNOW_COVERED_ISLAND:
            case SNOW_COVERED_MOUNTAIN:
                game.land(card).tag(SNOW_TAG);
                return true;
            case BOJUKA_BOG:
                game.land(card).setTapped(true);
                return true;
            case MORTUARY_MIRE:
                game.land(card).setTapped(true);
                // TODO: CIP effect
                return true;

            // stones
            case NAVIGATOR_S_COMPASS:
                produce(ONE);
                game.castArtifact(card, ONE);
                return true;
            case PROPHETIC_PRISM:
                produce(TWO);
                game.castArtifact(card, TWO);
                game.draw(1);
                return true;
            case ARCUM_S_ASTROLABE:
                Optional<Permanent> snowLand = game.getBattlefield().findFirst(withType(Game.CardType.land).and(untapped()).and(withTag(SNOW_TAG)));
                if (snowLand.isPresent()) {
                    game.tapLandForMana(snowLand.get(), ONE);
                } else {
                    // 1 uncolor + astro
                    produce(ONE);
                    game.tap(game.getBattlefield().findFirst(withName(ARCUM_S_ASTROLABE).and(untapped())).get());
                }
                game.castArtifact(card, ONE).tag(SNOW_TAG);
                game.draw(1);
                return true;

            // land tutors
            case EXPEDITION_MAP:
                produce(ONE);
                game.castArtifact(card, ONE);
                return true;
            case CROP_ROTATION:
                return canPay(G()) && getLandToSac().isPresent();

            // diggers
            case IMPULSE:
                produce(U1);
                game.castInstant(card, U1);
                chooseBestOf(4, Game.Area.library);
                return true;
            case FORBIDDEN_ALCHEMY:
                produce(U2);
                game.castInstant(card, U2);
                chooseBestOf(4, Game.Area.graveyard);
                return true;
            case FORBIDDEN_ALCHEMY_FB:
                produce(B6);
                game.castInstant(card, B6);
                chooseBestOf(4, Game.Area.graveyard);
                return true;
        }
        game.log("oops, unsupported card [" + card + "]");
        return false;
    }

    private String chooseBestOf(int number, Game.Area target) {
        // TODO
        Cards cards = game.draw(number);
        return null;
    }
}
