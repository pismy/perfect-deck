package org.mtgpeasant.decks.reanimator;

import org.mtgpeasant.perfectdeck.common.cards.Cards;
import org.mtgpeasant.perfectdeck.common.mana.Mana;
import org.mtgpeasant.perfectdeck.goldfish.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static java.util.Collections.singleton;
import static org.mtgpeasant.perfectdeck.common.mana.Mana.zero;
import static org.mtgpeasant.perfectdeck.goldfish.ManaSource.Landing.with;
import static org.mtgpeasant.perfectdeck.goldfish.ManaSource.*;
import static org.mtgpeasant.perfectdeck.goldfish.Permanent.untapped;
import static org.mtgpeasant.perfectdeck.goldfish.Permanent.withName;

/**
 * TODO: manage "Ransack the Lab" ?
 * TODO: manage "Demonic Consultation" ?
 * TODO: manage "Cathartic Reunion" / "Tormenting Voice" ?
 * TODO: change mulligans criteria (much more complex)
 */
public class ReanimatorDeckPilot extends DeckPilot<Game> {
    //    boolean firstCreaKilled = false;

    private static final Mana B = Mana.of("B");
    private static final Mana B1 = Mana.of("1B");
    private static final Mana R = Mana.of("R");
    private static final Mana R2 = Mana.of("2R");
    private static final Mana X = Mana.of("1");
    private static final Mana TWO = Mana.of("2");

    private static final String SWAMP = "swamp";
    private static final String MOUNTAIN = "mountain";
    private static final String CRUMBLING_VESTIGE = "crumbling vestige";
    private static final String LOTUS_PETAL = "lotus petal";
    private static final String DARK_RITUAL = "dark ritual";
    private static final String SIMIAN_SPIRIT_GUIDE = "simian spirit guide";
    private static final String EXHUME = "exhume";
    private static final String ANIMATE_DEAD = "animate dead";
    private static final String HAND_OF_EMRAKUL = "hand of emrakul";
    private static final String GREATER_SANDWURM = "greater sandwurm";
    private static final String PATHRAZER_OF_ULAMOG = "pathrazer of ulamog";
    private static final String ULAMOG_S_CRUSHER = "ulamog's crusher";
    private static final String PUTRID_IMP = "putrid imp";
    private static final String FAITHLESS_LOOTING = "faithless looting";
    private static final String REANIMATE = "reanimate";
    private static final String GITAXIAN_PROBE = "gitaxian probe";
    private static final String DRAGON_BREATH = "dragon breath";
    private static final String FUNERAL_CHARM = "funeral charm"; // B: discard 1
    private static final String RAVEN_S_CRIME = "raven's crime"; // B: discard 1; wit retrace
    private static final String MIND_RAKE = "mind rake"; // 1B: discard 2
    public static final Mana BBB = Mana.of("BBB");

    // ordered by power / interest to discard
//    private static String[] LANDS = new String[]{MOUNTAIN, SWAMP, CRUMBLING_VESTIGE};
//    private static String[] MANA = new String[]{MOUNTAIN, SWAMP, CRUMBLING_VESTIGE, LOTUS_PETAL, SIMIAN_SPIRIT_GUIDE, DARK_RITUAL};
    private static String[] CREATURES = new String[]{PATHRAZER_OF_ULAMOG, ULAMOG_S_CRUSHER, HAND_OF_EMRAKUL, GREATER_SANDWURM};

    private static Cards managedCards = DeckPilot.loadManagedCards(ReanimatorDeckPilot.class);

    public ReanimatorDeckPilot(Game game) {
        super(game);
    }

    @Override
    public boolean keepHand(Cards hand) {
        int reanimation = hand.count(ANIMATE_DEAD, REANIMATE, EXHUME);
        int creatures = hand.count(CREATURES);
        int blackMana = hand.count(SWAMP, LOTUS_PETAL, CRUMBLING_VESTIGE);
        int redMana = hand.count(MOUNTAIN, LOTUS_PETAL, CRUMBLING_VESTIGE, SIMIAN_SPIRIT_GUIDE);
        int totalMana = hand.count(MOUNTAIN, SWAMP, LOTUS_PETAL, CRUMBLING_VESTIGE, SIMIAN_SPIRIT_GUIDE);
        if (blackMana > 0) {
            // each dark ritual adds 2 mana
            blackMana += hand.count(DARK_RITUAL) * 2;
            totalMana += hand.count(DARK_RITUAL) * 2;
        }
        int discard = hand.count(PUTRID_IMP, FAITHLESS_LOOTING, FUNERAL_CHARM, MIND_RAKE, RAVEN_S_CRIME, GREATER_SANDWURM /* count sandwurm as a discard */);

        if (game.getMulligans() >= 3) {
            // always keep (no more than 3 mulligans)
            return true;
        }

        if (game.getMulligans() == 0 && !game.isOnThePlay()) {
            // specific case: I don't necessarily need a discard outlet (T1: draw / discard)
            discard++;
        }

        // general case: I need at least B1
        if (blackMana < 1 || totalMana < 2) {
            return false;
        }
        // I need all 3 pieces of the combo or only 2 + something to draw
        int piecesOfTheCombo = (creatures > 0 ? 1 : 0)
                + (reanimation > 0 ? 1 : 0)
                + (discard > 0 ? 1 : 0);
        switch (piecesOfTheCombo) {
            case 3:
                return true;
            case 2:
                return hand.findFirst(GREATER_SANDWURM, GITAXIAN_PROBE).isPresent() || hand.findFirst(FAITHLESS_LOOTING).isPresent() && redMana >= 1;
        }
        game.log("rea " + reanimation + "; crea " + creatures + "; discard" + discard);
        return false;
    }

    @Override
    public void start() {
        putOnBottomOfLibrary(game.getMulligans());
//        firstCreaKilled = false;
    }

    @Override
    public void firstMainPhase() {
        while (play()) {
        }
    }

    boolean play() {
        // whichever the situation, if I have a probe in hand: play it
        if (game.getHand().contains(GITAXIAN_PROBE)) {
            // play it: we'll maybe find what we miss
            game.castSorcery(GITAXIAN_PROBE, Mana.zero());
            game.draw(1);
            return true;
        }

        Optional<String> monsterInGy = game.getGraveyard().findFirst(CREATURES);
        Optional<String> monsterInHand = game.getHand().findFirst(CREATURES);
        if (monsterInGy.isPresent()) {
            // I have a monster in the graveyard: I must now reanimate
            if (game.getHand().contains(REANIMATE) && maybeProduce(B)) {
                game.castSorcery(REANIMATE, B);
                String monster = monsterInGy.get();
                game.move(monster, Game.Area.graveyard, Game.Area.battlefield);
                return true;
            } else if (game.getHand().findFirst(EXHUME, ANIMATE_DEAD).isPresent() && maybeProduce(B1)) {
                game.castSorcery(game.getHand().findFirst(EXHUME, ANIMATE_DEAD).get(), B1);
                String monster = monsterInGy.get();
                game.move(monster, Game.Area.graveyard, Game.Area.battlefield);
                return true;
            } else if (game.getHand().contains(FAITHLESS_LOOTING) && maybeProduce(R)) {
                game.castSorcery(FAITHLESS_LOOTING, R);
                game.draw(2);
                discard(2);
                return true;
            } else if (game.getHand().contains(GREATER_SANDWURM) && maybeProduce(TWO)) {
                // cycle
                game.log("cycle [" + GREATER_SANDWURM + "]");
                game.pay(TWO);
                game.discard(GREATER_SANDWURM);
                game.draw(1);
                return true;
            } else if (game.getGraveyard().contains(FAITHLESS_LOOTING) && maybeProduce(R2)) {
                game.cast(FAITHLESS_LOOTING, Game.Area.graveyard, Game.Area.exile, R2);
                game.draw(2);
                // then another card...
                discard(2);
                return true;
            }
        } else if (monsterInHand.isPresent()) {
            // I have a creature in hand
            if (game.getBattlefield().findFirst(withName(PUTRID_IMP)).isPresent()) {
                // I can discard a monster (any)
                game.discard(monsterInHand.get());
                return true;
            } else if (game.getHand().contains(PUTRID_IMP) && maybeProduce(B)) {
                game.castCreature(PUTRID_IMP, B);
                // discard a monster (any)
                game.discard(monsterInHand.get());
                return true;
            } else if (game.getHand().contains(FAITHLESS_LOOTING) && maybeProduce(R)) {
                game.castSorcery(FAITHLESS_LOOTING, R);
                game.draw(2);
                discard(2);
                return true;
            } else if (game.getHand().contains(RAVEN_S_CRIME) && maybeProduce(B)) { // no reason why I should play retrace in goldfish
                game.castSorcery(RAVEN_S_CRIME, B);
                // discard a monster (any)
                game.discard(monsterInHand.get());
                return true;
            } else if (game.getHand().contains(FUNERAL_CHARM) && maybeProduce(B)) {
                game.castInstant(FUNERAL_CHARM, B);
                // discard a monster (any)
                game.discard(monsterInHand.get());
                return true;
            } else if (game.getHand().contains(MIND_RAKE) && maybeProduce(B1)) {
                game.castSorcery(MIND_RAKE, B1);
                // discard a monster (any)
                game.discard(monsterInHand.get());
                // then discard something else
                discard(1);
                return true;
            } else if (game.getHand().contains(GREATER_SANDWURM) && maybeProduce(TWO)) {
                // cycle
                game.log("cycle [" + GREATER_SANDWURM + "]");
                game.pay(TWO);
                game.discard(GREATER_SANDWURM);
                game.draw(1);
                return true;
            } else if (game.getGraveyard().contains(FAITHLESS_LOOTING) && maybeProduce(R2)) {
                game.cast(FAITHLESS_LOOTING, Game.Area.graveyard, Game.Area.exile, R2);
                game.draw(2);
                // then another card...
                discard(2);
                return true;
            } else if (game.getHand().size() >= 7) {
                // I have a monster in hand: I can discard it at the end of this turn or next one
                return false;
            }
        } else {
            // no monster in hand: can I look for one ?
            if (game.getHand().contains(FAITHLESS_LOOTING) && maybeProduce(R)) {
                game.castSorcery(FAITHLESS_LOOTING, R);
                game.draw(2);
                // now discard 2 cards
                discard(2);
                return true;
            } else if (game.getGraveyard().contains(FAITHLESS_LOOTING) && maybeProduce(R2)) {
                game.cast(FAITHLESS_LOOTING, Game.Area.graveyard, Game.Area.exile, R2);
                game.draw(2);
                // then another card...
                discard(2);
                return true;
            }
        }

        // land if necessary
        Mana landsProduction = landsProduction(false);
        if (!game.isLanded() && landsProduction.getB() == 0 && game.getHand().contains(SWAMP)) {
            game.land(SWAMP);
        }
        if (!game.isLanded() && landsProduction.getR() == 0 && game.getHand().contains(MOUNTAIN)) {
            game.land(MOUNTAIN);
        }
        if (!game.isLanded() && landsProduction.ccm() < 3 && game.getHand().count(CRUMBLING_VESTIGE) >= 2) {
            game.land(CRUMBLING_VESTIGE);
        }
        // cast imp if none
        if (!game.getBattlefield().findFirst(withName(PUTRID_IMP)).isPresent() && game.getHand().contains(PUTRID_IMP) && maybeProduce(B)) {
            game.castCreature(PUTRID_IMP, B);
        }
        // EOT
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
            // look for an unmanaged card
            Optional<String> unmanagedCard = game.getHand().findFirstNotIn(managedCards);
            if (unmanagedCard.isPresent()) {
                game.putOnBottomOfLibrary(unmanagedCard.get());
                continue;
            }
            // extra creatures
            Cards creatures = game.getHand().findAll(CREATURES);
            if (creatures.size() > 1) {
                game.putOnBottomOfLibrary(creatures.getFirst());
                continue;
            }
            // dragon breath
            if (game.putOnBottomOfLibraryOneOf(DRAGON_BREATH).isPresent()) {
                continue;
            }
            // extra reanimator spells
            Cards reanimators = game.getHand().findAll(ANIMATE_DEAD, EXHUME, REANIMATE);
            if (reanimators.size() > 1) {
                game.putOnBottomOfLibrary(reanimators.getFirst());
                continue;
            }
            // extra lands and/or mana
            Cards redProducersInHand = game.getHand().findAll(CRUMBLING_VESTIGE, SIMIAN_SPIRIT_GUIDE, MOUNTAIN);
            if (redProducersInHand.size() > 2) {
                game.putOnBottomOfLibrary(redProducersInHand.getFirst());
                continue;
            }
            Cards blackProducersInHand = game.getHand().findAll(CRUMBLING_VESTIGE, DARK_RITUAL, SWAMP);
            if (blackProducersInHand.size() > 2) {
                game.putOnBottomOfLibrary(blackProducersInHand.getFirst());
                continue;
            }
            // gitaxian
            if (game.putOnBottomOfLibraryOneOf(GITAXIAN_PROBE).isPresent()) {
                continue;
            }
            Cards discarders = game.getHand().findAll(RAVEN_S_CRIME, FUNERAL_CHARM, PUTRID_IMP, FAITHLESS_LOOTING);
            if (discarders.size() > 2) {
                game.putOnBottomOfLibrary(discarders.getFirst());
                continue;
            }
            // guide
            if (game.putOnBottomOfLibraryOneOf(SIMIAN_SPIRIT_GUIDE, LOTUS_PETAL, MOUNTAIN, SWAMP, CRUMBLING_VESTIGE).isPresent()) {
                continue;
            }
//            System.out.println("Didn't find any suitable card to get rid of");
//            System.out.println(game);
            game.putOnBottomOfLibrary(game.getHand().getFirst());
        }
    }

    void discard(int number) {
        for (int i = 0; i < number; i++) {
            Cards monstersInGy = game.getGraveyard().findAll(CREATURES);
            // 1st: discard a creature
            if (monstersInGy.isEmpty() && game.discardOneOf(CREATURES).isPresent()) {
                continue;
            }
            // 2nd: discard a dragon breath
            if (game.discardOneOf(DRAGON_BREATH).isPresent()) {
                continue;
            }
            // discard an unmanaged card
            Optional<String> unmanagedCard = game.getHand().findFirstNotIn(managedCards);
            if (unmanagedCard.isPresent()) {
                game.discard(unmanagedCard.get());
                continue;
            }
            // 3rd: discard any additional creature in hand
            if (game.discardOneOf(CREATURES).isPresent()) {
                continue;
            }
            // 4th: discard probe
            if (game.discardOneOf(GITAXIAN_PROBE).isPresent()) {
                continue;
            }
            // now: lands, free mana, discard or reanimator spells
            // I only need 3 mana producers, with one B and one R
            Mana landsProduction = landsProduction(false);
            int redProducersInHand = game.getHand().count(CRUMBLING_VESTIGE, SIMIAN_SPIRIT_GUIDE, MOUNTAIN);
            if (redProducersInHand > 0 && landsProduction.getR() + redProducersInHand > 1) {
                // I can discard a red source
                game.discardOneOf(CRUMBLING_VESTIGE, SIMIAN_SPIRIT_GUIDE, MOUNTAIN).isPresent();
                continue;
            }
            int blackProducersInHand = game.getHand().count(CRUMBLING_VESTIGE, DARK_RITUAL, SWAMP);
            if (blackProducersInHand > 0 && landsProduction.getB() + blackProducersInHand > 1) {
                // I can discard a black source
                game.discardOneOf(CRUMBLING_VESTIGE, DARK_RITUAL, SWAMP).isPresent();
                continue;
            }
//            if (landsProduction.ccm() >= 3) {
//                if (landsProduction.getX() >= 1 && (maybeDiscard(CRUMBLING_VESTIGE))) {
//                    continue;
//                }
//                if (landsProduction.getR() >= 1 && (maybeDiscard(CRUMBLING_VESTIGE, MOUNTAIN))) {
//                    continue;
//                }
//                if (landsProduction.getB() >= 1 && (maybeDiscard(CRUMBLING_VESTIGE, SWAMP))) {
//                    continue;
//                }
//            }
            // discard extra reanimator spells
            Cards reanimators = game.getHand().findAll(ANIMATE_DEAD, EXHUME, REANIMATE);
            if (reanimators.size() > 1) {
                game.discard(reanimators.getFirst());
                continue;
            }
            if (landsProduction.getR() == 0 && game.discardOneOf(FAITHLESS_LOOTING).isPresent()) {
                continue;
            }
            if (landsProduction.getB() == 0 && game.discardOneOf(RAVEN_S_CRIME, FUNERAL_CHARM, PUTRID_IMP).isPresent()) {
                continue;
            }
            if (game.discardOneOf(FAITHLESS_LOOTING).isPresent()) {
                continue;
            }
            if (game.discardOneOf(RAVEN_S_CRIME, FUNERAL_CHARM, PUTRID_IMP).isPresent()) {
                continue;
            }
//            System.out.println("Didn't find any suitable card to discard");
//            System.out.println(game.getHand());
            if (game.discardOneOf(GITAXIAN_PROBE, SIMIAN_SPIRIT_GUIDE, MOUNTAIN, SWAMP, CRUMBLING_VESTIGE, LOTUS_PETAL).isPresent()) {
                continue;
            }
            game.discard(game.getHand().getFirst());
        }
    }

    Mana landsProduction(boolean untapped) {
        if (untapped) {
            // untapped lands only
            return Mana.of(game.getBattlefield().count(withName(SWAMP).and(untapped())), 0, 0, game.getBattlefield().count(withName(MOUNTAIN).and(untapped())), 0, game.getBattlefield().count(withName(CRUMBLING_VESTIGE).and(untapped())));
        } else {
            // total lands production
            return Mana.of(game.getBattlefield().count(withName(SWAMP)), 0, 0, game.getBattlefield().count(withName(MOUNTAIN)), 0, game.getBattlefield().count(withName(CRUMBLING_VESTIGE)));
        }
    }

    boolean maybeProduce(Mana cost) {
        List<ManaSource> sources = new ArrayList<>();
        sources.addAll(getTapSources(game, CRUMBLING_VESTIGE, zero(), singleton(X)));
        sources.addAll(getTapSources(game, SWAMP, zero(), singleton(B)));
        sources.addAll(getTapSources(game, MOUNTAIN, zero(), singleton(R)));
        sources.add(landing(
                with(SWAMP, B),
                with(MOUNTAIN, R),
                with(CRUMBLING_VESTIGE, B, R)
        ));
        sources.addAll(getDiscardSources(game, SIMIAN_SPIRIT_GUIDE, singleton(R)));
        sources.addAll(getDiscardSources(game, LOTUS_PETAL, oneOf(R, B)));
        // dark ritual not yet managed by planner
        if (ManaProductionPlanner.maybeProduce(game, sources, cost)) {
            return true;
        }
        if (game.getHand().contains(DARK_RITUAL) && BBB.contains(cost) && ManaProductionPlanner.maybeProduce(game, sources, B)) {
            game.castInstant(DARK_RITUAL, B);
            game.add(BBB);
            return true;
        }
        return false;
    }

//    boolean canPay(Mana toPay) {
//        // draw required X from Vestiges
//        int untappedVestiges = game.count(withName(CRUMBLING_VESTIGE).and(untapped()));
//        while (toPay.getX() > 0 && untappedVestiges > 0) {
//            untappedVestiges--;
//            toPay = toPay.minus(X);
//        }
//        // draw required B from Swamps
//        int untappedSwamps = game.count(withName(SWAMP).and(untapped()));
//        while (toPay.getB() > 0 && untappedSwamps > 0) {
//            untappedSwamps--;
//            toPay = toPay.minus(B);
//        }
//        // draw required R from Mountains
//        int untappedMountains = game.count(withName(MOUNTAIN).and(untapped()));
//        while (toPay.getR() > 0 && untappedMountains > 0) {
//            untappedMountains--;
//            toPay = toPay.minus(R);
//        }
//
//        // pay remaining cost with: land drop + simian + petal
//        boolean landed = game.isLanded();
//        int simiansInHand = game.getHand().count(SIMIAN_SPIRIT_GUIDE);
//        int petalsInHand = game.getHand().count(LOTUS_PETAL);
//
//        while (!toPay.isEmpty()) {
//            if (toPay.getB() > 0) {
//                if (!landed && game.getHand().findFirst(SWAMP, CRUMBLING_VESTIGE).isPresent()) {
//                    toPay = toPay.minus(B);
//                    landed = true;
//                } else if (petalsInHand > 0) {
//                    petalsInHand--;
//                    toPay = toPay.minus(B);
//                } else {
//                    // can't pay B :(
//                    return false;
//                }
//            } else if (toPay.getR() > 0) {
//                if (!landed && game.getHand().findFirst(MOUNTAIN, CRUMBLING_VESTIGE).isPresent()) {
//                    toPay = toPay.minus(R);
//                    landed = true;
//                } else if (simiansInHand > 0) {
//                    simiansInHand--;
//                    toPay = toPay.minus(R);
//                } else if (petalsInHand > 0) {
//                    petalsInHand--;
//                    toPay = toPay.minus(R);
//                } else {
//                    // can't pay R :(
//                    return false;
//                }
//            } else if (toPay.getX() > 0) {
//                if (untappedMountains > 0) {
//                    untappedMountains--;
//                    toPay = toPay.minus(X);
//                } else if (untappedSwamps > 0) {
//                    untappedSwamps--;
//                    toPay = toPay.minus(X);
//                } else if (!landed && game.getHand().findFirst(MOUNTAIN, SWAMP, CRUMBLING_VESTIGE).isPresent()) {
//                    toPay = toPay.minus(X);
//                    landed = true;
//                } else if (simiansInHand > 0) {
//                    simiansInHand--;
//                    toPay = toPay.minus(X);
//                } else if (petalsInHand > 0) {
//                    petalsInHand--;
//                    toPay = toPay.minus(X);
//                } else {
//                    // can't pay X :(
//                    return false;
//                }
//            }
//        }
//        return true;
//    }
//
//    boolean maybeProduce(String land, Mana mana) {
//        Optional<Card> untappedLand = game.findFirst(withName(land).and(untapped()));
//        if (untappedLand.isPresent()) {
//            game.tapLandForMana(untappedLand.get(), mana);
//            return true;
//        } else {
//            return false;
//        }
//    }
//
//    void produce(Mana cost) {
//        // draw required X from Vestiges
//        while (cost.getX() > 0 && maybeProduce(CRUMBLING_VESTIGE, X)) {
//            cost = cost.minus(X);
//        }
//        // draw required B from Swamps
//        while (cost.getB() > 0 && maybeProduce(SWAMP, B)) {
//            cost = cost.minus(B);
//        }
//        // draw required R from Mountains
//        while (cost.getR() > 0 && maybeProduce(MOUNTAIN, R)) {
//            cost = cost.minus(R);
//        }
//
//        // pay remaining cost with: land drop + simian + petal
//        boolean landed = game.isLanded();
//        int simiansInHand = game.getHand().count(SIMIAN_SPIRIT_GUIDE);
//        int petalsInHand = game.getHand().count(LOTUS_PETAL);
//
//        while (!cost.isEmpty()) {
//            if (cost.getB() > 0) {
//                Optional<String> blackProducer = game.getHand().findFirst(SWAMP, CRUMBLING_VESTIGE);
//                if (!landed && blackProducer.isPresent()) {
//                    Card land = game.land(blackProducer.get());
//                    game.tapLandForMana(land, B);
//                    cost = cost.minus(B);
//                    landed = true;
//                } else if (petalsInHand > 0) {
//                    game.discard(LOTUS_PETAL);
//                    game.add(B);
//                    cost = cost.minus(B);
//                    petalsInHand--;
//                } else {
//                    // can't pay B :(
//                    throw new RuntimeException("Couldn't pay " + cost);
//                }
//            } else if (cost.getR() > 0) {
//                Optional<String> redProducer = game.getHand().findFirst(MOUNTAIN, CRUMBLING_VESTIGE);
//                if (!landed && redProducer.isPresent()) {
//                    Card land = game.land(redProducer.get());
//                    game.tapLandForMana(land, R);
//                    cost = cost.minus(R);
//                    landed = true;
//                } else if (simiansInHand > 0) {
//                    game.discard(SIMIAN_SPIRIT_GUIDE);
//                    game.add(R);
//                    cost = cost.minus(R);
//                    simiansInHand--;
//                } else if (petalsInHand > 0) {
//                    game.discard(LOTUS_PETAL);
//                    game.add(R);
//                    cost = cost.minus(R);
//                    petalsInHand--;
//                } else {
//                    // can't pay R :(
//                    throw new RuntimeException("Couldn't pay " + cost);
//                }
//            } else if (cost.getX() > 0) {
//                Optional<String> xProducer = game.getHand().findFirst(MOUNTAIN, SWAMP, CRUMBLING_VESTIGE);
//                if (maybeProduce(MOUNTAIN, R)) {
//                    cost = cost.minus(X);
//                } else if (maybeProduce(SWAMP, B)) {
//                    cost = cost.minus(X);
//                } else if (!landed && xProducer.isPresent()) {
//                    Card land = game.land(xProducer.get());
//                    game.tapLandForMana(land, X);
//                    cost = cost.minus(X);
//                    landed = true;
//                } else if (simiansInHand > 0) {
//                    int nb = Math.min(cost.getX(), simiansInHand);
//                    for (int i = 0; i < nb; i++) {
//                        game.discard(SIMIAN_SPIRIT_GUIDE);
//                        game.add(R);
//                    }
//                    simiansInHand -= nb;
//                    cost = cost.minus(Mana.of(0, 0, 0, 0, 0, nb));
//                } else if (petalsInHand > 0) {
//                    int nb = Math.min(cost.getX(), petalsInHand);
//                    for (int i = 0; i < nb; i++) {
//                        game.discard(LOTUS_PETAL);
//                        game.add(B);
//                    }
//                    petalsInHand -= nb;
//                    cost = cost.minus(Mana.of(0, 0, 0, 0, 0, nb));
//                } else {
//                    // can't pay X :(
//                    throw new RuntimeException("Couldn't pay " + cost);
//                }
//            }
//        }
//    }

    @Override
    public String checkWin() {
        super.checkWin();
        // consider I won as soon as I have a monster on the battlefield
        Optional<Permanent> monstersOnBoard = game.getBattlefield().findFirst(withName(CREATURES));
//        if (!monstersOnBoard.isEmpty() && !firstCreaKilled) {
//            // kill first creature
//            game.destroy(monstersOnBoard.draw());
//            firstCreaKilled = true;
//        }
        if (monstersOnBoard.isPresent()) {
//            return "I reanimated a second monster";
            return "I reanimated a monster";
        }
        return null;
    }
}
