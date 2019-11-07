package org.mtgpeasant.decks.reanimator;

import org.mtgpeasant.perfectdeck.common.Mana;
import org.mtgpeasant.perfectdeck.common.cards.Cards;
import org.mtgpeasant.perfectdeck.common.matchers.MulliganRules;
import org.mtgpeasant.perfectdeck.goldfish.DeckPilot;
import org.mtgpeasant.perfectdeck.goldfish.Game;

import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Optional;

public class ReanimatorDeckPilot extends DeckPilot<Game> {
//    boolean firstCreaKilled = false;

    private static final Mana B = Mana.of("B");
    private static final Mana B1 = Mana.of("1B");
    private static final Mana R = Mana.of("R");
    private static final Mana R2 = Mana.of("2R");
    private static final Mana X = Mana.of("1");

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

    private static String[] REANIMATORS_1B = new String[]{EXHUME, ANIMATE_DEAD};
    // ordered by power / interest to discard
    private static String[] CREATURES = new String[]{PATHRAZER_OF_ULAMOG, ULAMOG_S_CRUSHER, HAND_OF_EMRAKUL, GREATER_SANDWURM};

    private static MulliganRules rules;

    static {
        try {
            rules = MulliganRules.parse(new InputStreamReader(ReanimatorDeckPilot.class.getResourceAsStream("/reanimator-rules.txt")));
        } catch (IOException e) {
            rules = null;
            System.err.println(e);
        }
    }

    public ReanimatorDeckPilot(Game game) {
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
            game.castNonPermanent(GITAXIAN_PROBE, Mana.zero());
            game.draw(1);
            return true;
        }

        Cards monstersInGy = game.getGraveyard().findAll(CREATURES);
        Cards monstersInHand = game.getHand().findAll(CREATURES);
        if (!monstersInGy.isEmpty()) {
            // I have a monster in the graveyard: I must now reanimate
            Cards reanimators = game.getHand().findAll(REANIMATORS_1B);
            if (game.getHand().contains(REANIMATE) && canPay(B)) {
                produce(B);
                game.castNonPermanent(REANIMATE, B);
                String monster = monstersInGy.getFirst();
                game.move(monster, Game.Area.graveyard, Game.Area.board);
                return true;
            } else if (!reanimators.isEmpty() && canPay(B1)) {
                produce(B1);
                game.castNonPermanent(reanimators.getFirst(), B1);
                String monster = monstersInGy.getFirst();
                game.move(monster, Game.Area.graveyard, Game.Area.board);
                return true;
            } else if (game.getHand().contains(FAITHLESS_LOOTING) && canPay(R)) {
                produce(R);
                game.castNonPermanent(FAITHLESS_LOOTING, R);
                game.draw(2);
                discard(2);
                return true;
            } else if (game.getHand().contains(GREATER_SANDWURM) && canPay(Mana.of("2"))) {
                // cycle
                produce(Mana.of("2"));
                game.discard(GREATER_SANDWURM);
                game.draw(1);
                return true;
            } else if (game.getGraveyard().contains(FAITHLESS_LOOTING) && canPay(R2)) {
                produce(R2);
                game.cast(FAITHLESS_LOOTING, Game.Area.graveyard, Game.Area.exile, R2);
                game.draw(2);
                // then another card...
                discard(2);
                return true;
            }
        } else if (monstersInHand.isEmpty()) {
            // no monster in hand: can I look for one ?
            if (game.getHand().contains(FAITHLESS_LOOTING) && canPay(R)) {
                produce(R);
                game.castNonPermanent(FAITHLESS_LOOTING, R);
                game.draw(2);
                // now discard 2 cards
                discard(2);
                return true;
            } else if (game.getGraveyard().contains(FAITHLESS_LOOTING) && canPay(R2)) {
                produce(R2);
                game.cast(FAITHLESS_LOOTING, Game.Area.graveyard, Game.Area.exile, R2);
                game.draw(2);
                // then another card...
                discard(2);
                return true;
            }
        } else {
            // I have a creature in hand
            if (game.getBoard().contains(PUTRID_IMP)) {
                // I can discard a monster (any)
                game.discard(monstersInHand.getFirst());
                return true;
            } else if (game.getHand().contains(PUTRID_IMP) && canPay(B)) {
                produce(B);
                game.castPermanent(PUTRID_IMP, B);
                // discard a monster (any)
                game.discard(monstersInHand.getFirst());
                return true;
            } else if (game.getHand().contains(FAITHLESS_LOOTING) && canPay(R)) {
                produce(R);
                game.castNonPermanent(FAITHLESS_LOOTING, R);
                game.draw(2);
                discard(2);
                return true;
            } else if (game.getHand().contains(GREATER_SANDWURM) && canPay(Mana.of("2"))) {
                // cycle
                produce(Mana.of("2"));
                game.discard(GREATER_SANDWURM);
                game.draw(1);
                return true;
            } else if (game.getGraveyard().contains(FAITHLESS_LOOTING) && canPay(R2)) {
                produce(R2);
                game.cast(FAITHLESS_LOOTING, Game.Area.graveyard, Game.Area.exile, R2);
                game.draw(2);
                // then another card...
                discard(2);
                return true;
            } else if (game.getHand().size() >= 7) {
                // I have a monster in hand: I can discard it at the end of this turn or next one
                return false;
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
        if (!game.getBoard().contains(PUTRID_IMP) && game.getHand().contains(PUTRID_IMP) && canPay(B)) {
            produce(B);
            game
                    .castPermanent(PUTRID_IMP, B);
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
            Cards blackProducersInHand = game.getHand().findAll(CRUMBLING_VESTIGE, SWAMP);
            if (blackProducersInHand.size() > 2) {
                game.putOnBottomOfLibrary(blackProducersInHand.getFirst());
                continue;
            }
            // gitaxian
            if (game.putOnBottomOfLibraryOneOf(GITAXIAN_PROBE).isPresent()) {
                continue;
            }
            Cards discarders = game.getHand().findAll(PUTRID_IMP, FAITHLESS_LOOTING);
            if (discarders.size() > 2) {
                game.putOnBottomOfLibrary(discarders.getFirst());
                continue;
            }
            // guide
            if (game.putOnBottomOfLibraryOneOf(SIMIAN_SPIRIT_GUIDE, LOTUS_PETAL, MOUNTAIN, SWAMP, CRUMBLING_VESTIGE).isPresent()) {
                continue;
            }
            System.out.println("Didn't find any suitable card to get rid of");
            System.out.println(game);
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
            int blackProducersInHand = game.getHand().count(CRUMBLING_VESTIGE, SWAMP);
            if (blackProducersInHand > 0 && landsProduction.getB() + blackProducersInHand > 1) {
                // I can discard a black source
                game.discardOneOf(CRUMBLING_VESTIGE, SWAMP).isPresent();
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
            if (landsProduction.getB() == 0 && game.discardOneOf(PUTRID_IMP).isPresent()) {
                continue;
            }
            if (game.discardOneOf(FAITHLESS_LOOTING).isPresent()) {
                continue;
            }
            if (game.discardOneOf(PUTRID_IMP).isPresent()) {
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
            return Mana.of(game.getBoard().count(SWAMP) - game.getTapped().count(SWAMP), 0, 0, game.getBoard().count(MOUNTAIN) - game.getTapped().count(MOUNTAIN), 0, game.getBoard().count(CRUMBLING_VESTIGE) - game.getTapped().count(CRUMBLING_VESTIGE));
        } else {
            // total lands production
            return Mana.of(game.getBoard().count(SWAMP), 0, 0, game.getBoard().count(MOUNTAIN), 0, game.getBoard().count(CRUMBLING_VESTIGE));
        }
    }

    boolean canPay(Mana toPay) {
        // draw required X from Vestiges
        int untappedVestiges = game.getBoard().count(CRUMBLING_VESTIGE) - game.getTapped().count(CRUMBLING_VESTIGE);
        if (toPay.getX() > 0 && untappedVestiges > 0) {
            int nb = Math.min(toPay.getX(), untappedVestiges);
            untappedVestiges -= nb;
            toPay = toPay.minus(Mana.of(0, 0, 0, 0, 0, nb));
        }
        // draw required B from Swamps
        int untappedSwamps = game.getBoard().count(SWAMP) - game.getTapped().count(SWAMP);
        if (toPay.getB() > 0 && untappedSwamps > 0) {
            int nb = Math.min(toPay.getB(), untappedSwamps);
            untappedSwamps -= nb;
            toPay = toPay.minus(Mana.of(nb, 0, 0, 0, 0, 0));
        }
        // draw required R from Mountains
        int untappedMountains = game.getBoard().count(MOUNTAIN) - game.getTapped().count(MOUNTAIN);
        if (toPay.getR() > 0 && untappedMountains > 0) {
            int nb = Math.min(toPay.getR(), untappedMountains);
            untappedMountains -= nb;
            toPay = toPay.minus(Mana.of(0, 0, 0, nb, 0, 0));
        }

        // pay remaining cost with: land drop + simian + petal
        boolean landed = game.isLanded();
        int simiansInHand = game.getHand().count(SIMIAN_SPIRIT_GUIDE);
        int petalsInHand = game.getHand().count(LOTUS_PETAL);

        while (!toPay.isEmpty()) {
            if (toPay.getB() > 0) {
                if (!landed && game.getHand().findFirst(SWAMP, CRUMBLING_VESTIGE).isPresent()) {
                    toPay = toPay.minus(B);
                    landed = true;
                } else if (petalsInHand > 0) {
                    int nb = Math.min(toPay.getB(), petalsInHand);
                    petalsInHand -= nb;
                    toPay = toPay.minus(Mana.of(nb, 0, 0, 0, 0, 0));
                } else {
                    // can't pay B :(
                    return false;
                }
            } else if (toPay.getR() > 0) {
                if (!landed && game.getHand().findFirst(MOUNTAIN, CRUMBLING_VESTIGE).isPresent()) {
                    toPay = toPay.minus(R);
                    landed = true;
                } else if (simiansInHand > 0) {
                    int nb = Math.min(toPay.getR(), simiansInHand);
                    simiansInHand -= nb;
                    toPay = toPay.minus(Mana.of(0, 0, 0, nb, 0, 0));
                } else if (petalsInHand > 0) {
                    int nb = Math.min(toPay.getR(), petalsInHand);
                    petalsInHand -= nb;
                    toPay = toPay.minus(Mana.of(0, 0, 0, nb, 0, 0));
                } else {
                    // can't pay R :(
                    return false;
                }
            } else if (toPay.getX() > 0) {
                if (untappedMountains > 0) {
                    int nb = Math.min(toPay.getX(), untappedMountains);
                    untappedMountains -= nb;
                    toPay = toPay.minus(Mana.of(0, 0, 0, 0, 0, nb));
                } else if (untappedSwamps > 0) {
                    int nb = Math.min(toPay.getX(), untappedSwamps);
                    untappedSwamps -= nb;
                    toPay = toPay.minus(Mana.of(0, 0, 0, 0, 0, nb));
                } else if (!landed && game.getHand().findFirst(MOUNTAIN, SWAMP, CRUMBLING_VESTIGE).isPresent()) {
                    toPay = toPay.minus(X);
                    landed = true;
                } else if (simiansInHand > 0) {
                    int nb = Math.min(toPay.getX(), simiansInHand);
                    simiansInHand -= nb;
                    toPay = toPay.minus(Mana.of(0, 0, 0, 0, 0, nb));
                } else if (petalsInHand > 0) {
                    int nb = Math.min(toPay.getX(), petalsInHand);
                    petalsInHand -= nb;
                    toPay = toPay.minus(Mana.of(0, 0, 0, 0, 0, nb));
                } else {
                    // can't pay X :(
                    return false;
                }
            }
        }
        return true;
    }

    void produce(Mana toPay) {
        // draw required X from Vestiges
        int untappedVestiges = game.getBoard().count(CRUMBLING_VESTIGE) - game.getTapped().count(CRUMBLING_VESTIGE);
        if (toPay.getX() > 0 && untappedVestiges > 0) {
            int nb = Math.min(toPay.getX(), untappedVestiges);
            for (int i = 0; i < nb; i++) {
                game.tapLandForMana(CRUMBLING_VESTIGE, X);
            }
            untappedVestiges -= nb;
            toPay = toPay.minus(Mana.of(0, 0, 0, 0, 0, nb));
        }
        // draw required B from Swamps
        int untappedSwamps = game.getBoard().count(SWAMP) - game.getTapped().count(SWAMP);
        if (toPay.getB() > 0 && untappedSwamps > 0) {
            int nb = Math.min(toPay.getB(), untappedSwamps);
            for (int i = 0; i < nb; i++) {
                game.tapLandForMana(SWAMP, B);
            }
            untappedSwamps -= nb;
            toPay = toPay.minus(Mana.of(nb, 0, 0, 0, 0, 0));
        }
        // draw required R from Mountains
        int untappedMountains = game.getBoard().count(MOUNTAIN) - game.getTapped().count(MOUNTAIN);
        if (toPay.getR() > 0 && untappedMountains > 0) {
            int nb = Math.min(toPay.getR(), untappedMountains);
            for (int i = 0; i < nb; i++) {
                game.tapLandForMana(MOUNTAIN, R);
            }
            untappedMountains -= nb;
            toPay = toPay.minus(Mana.of(0, 0, 0, nb, 0, 0));
        }

        // pay remaining cost with: land drop + simian + petal
        boolean landed = game.isLanded();
        int simiansInHand = game.getHand().count(SIMIAN_SPIRIT_GUIDE);
        int petalsInHand = game.getHand().count(LOTUS_PETAL);

        while (!toPay.isEmpty()) {
            if (toPay.getB() > 0) {
                Optional<String> blackProducer = game.getHand().findFirst(SWAMP, CRUMBLING_VESTIGE);
                if (!landed && blackProducer.isPresent()) {
                    game.land(blackProducer.get());
                    game.tapLandForMana(blackProducer.get(), B);
                    toPay = toPay.minus(B);
                    landed = true;
                } else if (petalsInHand > 0) {
                    int nb = Math.min(toPay.getB(), petalsInHand);
                    for (int i = 0; i < nb; i++) {
                        game.discard(LOTUS_PETAL);
                        game.add(B);
                    }
                    petalsInHand -= nb;
                    toPay = toPay.minus(Mana.of(nb, 0, 0, 0, 0, 0));
                } else {
                    // can't pay B :(
                    throw new RuntimeException("Couldn't pay " + toPay);
                }
            } else if (toPay.getR() > 0) {
                Optional<String> redProducer = game.getHand().findFirst(MOUNTAIN, CRUMBLING_VESTIGE);
                if (!landed && redProducer.isPresent()) {
                    game.land(redProducer.get());
                    game.tapLandForMana(redProducer.get(), R);
                    toPay = toPay.minus(R);
                    landed = true;
                } else if (simiansInHand > 0) {
                    int nb = Math.min(toPay.getR(), simiansInHand);
                    for (int i = 0; i < nb; i++) {
                        game.discard(SIMIAN_SPIRIT_GUIDE);
                        game.add(R);
                    }
                    simiansInHand -= nb;
                    toPay = toPay.minus(Mana.of(0, 0, 0, nb, 0, 0));
                } else if (petalsInHand > 0) {
                    int nb = Math.min(toPay.getR(), petalsInHand);
                    for (int i = 0; i < nb; i++) {
                        game.discard(LOTUS_PETAL);
                        game.add(R);
                    }
                    petalsInHand -= nb;
                    toPay = toPay.minus(Mana.of(0, 0, 0, nb, 0, 0));
                } else {
                    // can't pay R :(
                    throw new RuntimeException("Couldn't pay " + toPay);
                }
            } else if (toPay.getX() > 0) {
                Optional<String> xProducer = game.getHand().findFirst(MOUNTAIN, SWAMP, CRUMBLING_VESTIGE);
                if (untappedMountains > 0) {
                    int nb = Math.min(toPay.getX(), untappedMountains);
                    for (int i = 0; i < nb; i++) {
                        game.tapLandForMana(MOUNTAIN, R);
                    }
                    untappedMountains -= nb;
                    toPay = toPay.minus(Mana.of(0, 0, 0, 0, 0, nb));
                } else if (untappedSwamps > 0) {
                    int nb = Math.min(toPay.getX(), untappedSwamps);
                    for (int i = 0; i < nb; i++) {
                        game.tapLandForMana(SWAMP, B);
                    }
                    untappedSwamps -= nb;
                    toPay = toPay.minus(Mana.of(0, 0, 0, 0, 0, nb));
                } else if (!landed && xProducer.isPresent()) {
                    game.land(xProducer.get());
                    game.tapLandForMana(xProducer.get(), X);
                    toPay = toPay.minus(X);
                    landed = true;
                } else if (simiansInHand > 0) {
                    int nb = Math.min(toPay.getX(), simiansInHand);
                    for (int i = 0; i < nb; i++) {
                        game.discard(SIMIAN_SPIRIT_GUIDE);
                        game.add(R);
                    }
                    simiansInHand -= nb;
                    toPay = toPay.minus(Mana.of(0, 0, 0, 0, 0, nb));
                } else if (petalsInHand > 0) {
                    int nb = Math.min(toPay.getX(), petalsInHand);
                    for (int i = 0; i < nb; i++) {
                        game.discard(LOTUS_PETAL);
                        game.add(B);
                    }
                    petalsInHand -= nb;
                    toPay = toPay.minus(Mana.of(0, 0, 0, 0, 0, nb));
                } else {
                    // can't pay X :(
                    throw new RuntimeException("Couldn't pay " + toPay);
                }
            }
        }
    }

    @Override
    public String checkWin() {
        super.checkWin();
        // consider I won as soon as I have a monster on the board
        Cards monstersOnBoard = game.getBoard().findAll(CREATURES);
//        if (!monstersOnBoard.isEmpty() && !firstCreaKilled) {
//            // kill first creature
//            game.destroy(monstersOnBoard.draw());
//            firstCreaKilled = true;
//        }
        if (!monstersOnBoard.isEmpty()) {
//            return "I reanimated a second monster";
            return "I reanimated a monster";
        }
        return null;
    }
}
