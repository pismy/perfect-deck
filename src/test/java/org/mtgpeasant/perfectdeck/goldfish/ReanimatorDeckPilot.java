package org.mtgpeasant.perfectdeck.goldfish;

import org.mtgpeasant.perfectdeck.common.cards.Cards;
import org.mtgpeasant.perfectdeck.common.matchers.MulliganRules;

public class ReanimatorDeckPilot extends DeckPilot {
    public static final Mana B = Mana.of("B");
    public static final Mana B1 = Mana.of("1B");
    public static final Mana R = Mana.of("R");
    public static final Mana R2 = Mana.of("2R");
    public static final Mana X = Mana.of("1");

    public static final String SWAMP = "swamp";
    public static final String MOUNTAIN = "mountain";
    public static final String CRUMBLING_VESTIGE = "crumbling vestige";
    public static final String LOTUS_PETAL = "lotus petal";
    public static final String DARK_RITUAL = "dark ritual";
    public static final String SIMIAN_SPIRIT_GUIDE = "simian spirit guide";
    public static final String EXHUME = "exhume";
    public static final String ANIMATE_DEAD = "animate dead";
    public static final String HAND_OF_EMRAKUL = "hand of emrakul";
    public static final String GREATER_SANDWURM = "greater sandwurm";
    public static final String PATHRAZER_OF_ULAMOG = "pathrazer of ulamog";
    public static final String ULAMOG_S_CRUSHER = "ulamog's crusher";
    public static final String PUTRID_IMP = "putrid imp";
    public static final String FAITHLESS_LOOTING = "faithless looting";
    public static final String REANIMATE = "reanimate";
    public static final String GITAXIAN_PROBE = "gitaxian probe";
    public static final String DRAGON_BREATH = "dragon breath";

    private static String[] REANIMATORS_1B = new String[]{EXHUME, ANIMATE_DEAD};
    private static String[] CREATURES = new String[]{HAND_OF_EMRAKUL, GREATER_SANDWURM, PATHRAZER_OF_ULAMOG, ULAMOG_S_CRUSHER};

    private final MulliganRules rules;

    public ReanimatorDeckPilot(MulliganRules rules) {
        this.rules = rules;
    }

    @Override
    public boolean keepHand(boolean onThePlay, int mulligans, Cards hand) {
        if (mulligans >= 3) {
            return true;
        }
        return rules.firstMatch(hand).isPresent();
    }

    @Override
    public void startGame(int mulligans, Game game) {
        super.startGame(mulligans, game);
        getRid(mulligans);
    }

    @Override
    public void firstMainPhase() {
        while (play()) {
        }
    }

    private boolean play() {
        Cards monstersInGy = game.getGraveyard().select(CREATURES);
        Cards monstersInHand = game.getHand().select(CREATURES);
        if (!monstersInGy.isEmpty()) {
            // I have a monster in the graveyard: I must now reanimate
            Cards reanimators = game.getHand().select(REANIMATORS_1B);
            if (game.getHand().contains(REANIMATE) && canPay(B)) {
                pay(B);
                game.castNonPermanent(REANIMATE, B);
                game.move(monstersInGy.getFirst(), Game.Area.graveyard, Game.Area.board);
                // done
                return false;
            } else if (!reanimators.isEmpty() && canPay(B1)) {
                pay(B1);
                game.castNonPermanent(reanimators.getFirst(), B1);
                game.move(monstersInGy.getFirst(), Game.Area.graveyard, Game.Area.board);
                // done
                return false;
            } else if (game.getHand().contains(GITAXIAN_PROBE)) {
                // play it: we'll maybe find what we miss
                game.castNonPermanent(GITAXIAN_PROBE, Mana.zero()).draw(1);
                return true;
            } else if (game.getHand().contains(FAITHLESS_LOOTING) && canPay(R)) {
                pay(R);
                game
                        .castNonPermanent(FAITHLESS_LOOTING, R)
                        .draw(2);
                discard(2);
                return true;
            } else if (game.getHand().contains(GREATER_SANDWURM) && canPay(Mana.of("2"))) {
                // cycle
                pay(Mana.of("2"));
                game
                        .discard(GREATER_SANDWURM)
                        .draw(1);
                return true;
            } else if (game.getGraveyard().contains(FAITHLESS_LOOTING) && canPay(R2)) {
                pay(R2);
                game
                        .cast(FAITHLESS_LOOTING, Game.Area.graveyard, Game.Area.exile, R2)
                        .draw(2);
                // then another card...
                discard(2);
                return true;
            }
        } else if (monstersInHand.isEmpty()) {
            // no monster in hand: can I look for one ?
            if (game.getHand().contains(GITAXIAN_PROBE)) {
                game.castNonPermanent(GITAXIAN_PROBE, Mana.zero()).draw(1);
                return true;
            } else if (game.getHand().contains(FAITHLESS_LOOTING) && canPay(R)) {
                pay(R);
                game
                        .castNonPermanent(FAITHLESS_LOOTING, R)
                        .draw(2);
                // now discard 2 cards
                discard(2);
                return true;
            } else if (game.getGraveyard().contains(FAITHLESS_LOOTING) && canPay(R2)) {
                pay(R2);
                game
                        .cast(FAITHLESS_LOOTING, Game.Area.graveyard, Game.Area.exile, R2)
                        .draw(2);
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
                pay(B);
                game
                        .castPermanent(PUTRID_IMP, B)
                        // discard a monster (any)
                        .discard(monstersInHand.getFirst());
                return true;
            } else if (game.getHand().contains(FAITHLESS_LOOTING) && canPay(R)) {
                pay(R);
                game
                        .castNonPermanent(FAITHLESS_LOOTING, R)
                        .draw(2);
                discard(2);
                return true;
            } else if (game.getHand().contains(GREATER_SANDWURM) && canPay(Mana.of("2"))) {
                // cycle
                pay(Mana.of("2"));
                game
                        .discard(GREATER_SANDWURM)
                        .draw(1);
                return true;
            } else if (game.getGraveyard().contains(FAITHLESS_LOOTING) && canPay(R2)) {
                pay(R2);
                game
                        .cast(FAITHLESS_LOOTING, Game.Area.graveyard, Game.Area.exile, R2)
                        .draw(2);
                // then another card...
                discard(2);
                return true;
            } else if (game.getHand().size() >= 8) {
                // I have a monster in hand: I can discard it at the end of the turn
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
        // cast imp if none
        if (!game.getBoard().contains(PUTRID_IMP) && game.getHand().contains(PUTRID_IMP) && canPay(B)) {
            pay(B);
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

    private void getRid(int number) {
        for (int i = 0; i < number; i++) {
            // extra creatures
            Cards creatures = game.getHand().select(CREATURES);
            if (creatures.size() > 1) {
                game.putOnBottomOfLibrary(creatures.getFirst());
                continue;
            }
            // dragon breath
            if (game.putOnBottomOfLibraryOneOf(DRAGON_BREATH) != null) {
                continue;
            }
            // extra reanimator spells
            Cards reanimators = game.getHand().select(ANIMATE_DEAD, EXHUME, REANIMATE);
            if (reanimators.size() > 1) {
                game.putOnBottomOfLibrary(reanimators.getFirst());
                continue;
            }
            // extra lands and/or mana
            Cards redProducersInHand = game.getHand().select(CRUMBLING_VESTIGE, SIMIAN_SPIRIT_GUIDE, MOUNTAIN);
            if (redProducersInHand.size() > 2) {
                game.putOnBottomOfLibrary(redProducersInHand.getFirst());
                continue;
            }
            Cards blackProducersInHand = game.getHand().select(CRUMBLING_VESTIGE, SWAMP);
            if (blackProducersInHand.size() > 2) {
                game.putOnBottomOfLibrary(blackProducersInHand.getFirst());
                continue;
            }
            // gitaxian
            if (game.putOnBottomOfLibraryOneOf(GITAXIAN_PROBE) != null) {
                continue;
            }
            Cards discarders = game.getHand().select(PUTRID_IMP, FAITHLESS_LOOTING);
            if (discarders.size() > 2) {
                game.putOnBottomOfLibrary(discarders.getFirst());
                continue;
            }
            // guide
            if (game.putOnBottomOfLibraryOneOf(SIMIAN_SPIRIT_GUIDE, LOTUS_PETAL, MOUNTAIN, SWAMP, CRUMBLING_VESTIGE) != null) {
                continue;
            }
            System.out.println("Didn't find any suitable card to get rid of");
            System.out.println(game);
            game.putOnBottomOfLibrary(game.getHand().getFirst());
        }
    }

    private void discard(int number) {
        for (int i = 0; i < number; i++) {
            Cards monstersInGy = game.getGraveyard().select(CREATURES);
            // 1st: discard a creature
            if (monstersInGy.isEmpty() && maybeDiscard(CREATURES)) {
                continue;
            }
            // 2nd: discard a dragon breath
            if (maybeDiscard(DRAGON_BREATH)) {
                continue;
            }
            // 3rd: discard any additional creature in hand
            if (maybeDiscard(CREATURES)) {
                continue;
            }
            // 4th: discard probe
            if (maybeDiscard(GITAXIAN_PROBE)) {
                continue;
            }
            // now: lands, free mana, discard or reanimator spells
            // I only need 3 mana producers, with one B and one R
            Mana landsProduction = landsProduction(false);
            int redProducersInHand = game.getHand().count(CRUMBLING_VESTIGE, SIMIAN_SPIRIT_GUIDE, MOUNTAIN);
            if (redProducersInHand > 0 && landsProduction.getR() + redProducersInHand > 1) {
                // I can discard a red source
                maybeDiscard(CRUMBLING_VESTIGE, SIMIAN_SPIRIT_GUIDE, MOUNTAIN);
                continue;
            }
            int blackProducersInHand = game.getHand().count(CRUMBLING_VESTIGE, SWAMP);
            if (blackProducersInHand > 0 && landsProduction.getB() + blackProducersInHand > 1) {
                // I can discard a black source
                maybeDiscard(CRUMBLING_VESTIGE, SWAMP);
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
            Cards reanimators = game.getHand().select(ANIMATE_DEAD, EXHUME, REANIMATE);
            if (reanimators.size() > 1) {
                game.discard(reanimators.getFirst());
                continue;
            }
            if (landsProduction.getR() == 0 && maybeDiscard(FAITHLESS_LOOTING)) {
                continue;
            }
            if (landsProduction.getB() == 0 && maybeDiscard(PUTRID_IMP)) {
                continue;
            }
            if (maybeDiscard(FAITHLESS_LOOTING)) {
                continue;
            }
            if (maybeDiscard(PUTRID_IMP)) {
                continue;
            }
            System.out.println("Didn't find any suitable card to discard");
            System.out.println(game);
            if (maybeDiscard(GITAXIAN_PROBE, SIMIAN_SPIRIT_GUIDE, MOUNTAIN, SWAMP, CRUMBLING_VESTIGE, LOTUS_PETAL)) {
                continue;
            }
            game.discard(game.getHand().getFirst());
        }
    }

    private boolean maybeDiscard(String... cards) {
        return game.discardOneOf(cards) != null;
    }

    private Mana landsProduction(boolean untapped) {
        if (untapped) {
            // untapped lands only
            return Mana.of(game.getBoard().count(SWAMP) - game.getTapped().count(SWAMP), 0, 0, game.getBoard().count(MOUNTAIN) - game.getTapped().count(MOUNTAIN), 0, game.getBoard().count(CRUMBLING_VESTIGE) - game.getTapped().count(CRUMBLING_VESTIGE));
        } else {
            // total lands production
            return Mana.of(game.getBoard().count(SWAMP), 0, 0, game.getBoard().count(MOUNTAIN), 0, game.getBoard().count(CRUMBLING_VESTIGE));
        }
    }

    private boolean canPay(Mana toPay) {
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
                if (!landed && game.getHand().hasOne(CRUMBLING_VESTIGE, SWAMP) != null) {
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
                if (!landed && game.getHand().hasOne(CRUMBLING_VESTIGE, MOUNTAIN) != null) {
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
                if (!landed && game.getHand().hasOne(MOUNTAIN, SWAMP, CRUMBLING_VESTIGE) != null) {
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

    private void pay(Mana toPay) {
        // draw required X from Vestiges
        int untappedVestiges = game.getBoard().count(CRUMBLING_VESTIGE) - game.getTapped().count(CRUMBLING_VESTIGE);
        if (toPay.getX() > 0 && untappedVestiges > 0) {
            int nb = Math.min(toPay.getX(), untappedVestiges);
            for (int i = 0; i < nb; i++) {
                game.tap(CRUMBLING_VESTIGE).add(X);
            }
            untappedVestiges -= nb;
            toPay = toPay.minus(Mana.of(0, 0, 0, 0, 0, nb));
        }
        // draw required B from Swamps
        int untappedSwamps = game.getBoard().count(SWAMP) - game.getTapped().count(SWAMP);
        if (toPay.getB() > 0 && untappedSwamps > 0) {
            int nb = Math.min(toPay.getB(), untappedSwamps);
            for (int i = 0; i < nb; i++) {
                game.tap(SWAMP).add(B);
            }
            untappedSwamps -= nb;
            toPay = toPay.minus(Mana.of(nb, 0, 0, 0, 0, 0));
        }
        // draw required R from Mountains
        int untappedMountains = game.getBoard().count(MOUNTAIN) - game.getTapped().count(MOUNTAIN);
        if (toPay.getR() > 0 && untappedMountains > 0) {
            int nb = Math.min(toPay.getR(), untappedMountains);
            for (int i = 0; i < nb; i++) {
                game.tap(MOUNTAIN).add(R);
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
                String blackProducer = game.getHand().hasOne(CRUMBLING_VESTIGE, SWAMP);
                if (!landed && blackProducer != null) {
                    game.land(blackProducer).tap(blackProducer).add(B);
                    toPay = toPay.minus(B);
                    landed = true;
                } else if (petalsInHand > 0) {
                    int nb = Math.min(toPay.getB(), petalsInHand);
                    for (int i = 0; i < nb; i++) {
                        game.discard(LOTUS_PETAL).add(B);
                    }
                    petalsInHand -= nb;
                    toPay = toPay.minus(Mana.of(nb, 0, 0, 0, 0, 0));
                } else {
                    // can't pay B :(
                    throw new RuntimeException("Couldn't pay " + toPay);
                }
            } else if (toPay.getR() > 0) {
                String redProducer = game.getHand().hasOne(CRUMBLING_VESTIGE, MOUNTAIN);
                if (!landed && redProducer != null) {
                    game.land(redProducer).tap(redProducer).add(R);
                    toPay = toPay.minus(R);
                    landed = true;
                } else if (simiansInHand > 0) {
                    int nb = Math.min(toPay.getR(), simiansInHand);
                    for (int i = 0; i < nb; i++) {
                        game.discard(SIMIAN_SPIRIT_GUIDE).add(R);
                    }
                    simiansInHand -= nb;
                    toPay = toPay.minus(Mana.of(0, 0, 0, nb, 0, 0));
                } else if (petalsInHand > 0) {
                    int nb = Math.min(toPay.getR(), petalsInHand);
                    for (int i = 0; i < nb; i++) {
                        game.discard(LOTUS_PETAL).add(R);
                    }
                    petalsInHand -= nb;
                    toPay = toPay.minus(Mana.of(0, 0, 0, nb, 0, 0));
                } else {
                    // can't pay R :(
                    throw new RuntimeException("Couldn't pay " + toPay);
                }
            } else if (toPay.getX() > 0) {
                String xProducer = game.getHand().hasOne(MOUNTAIN, SWAMP, CRUMBLING_VESTIGE);
                if (!landed && xProducer != null) {
                    game.land(xProducer).tap(xProducer).add(X);
                    toPay = toPay.minus(X);
                    landed = true;
                } else if (simiansInHand > 0) {
                    int nb = Math.min(toPay.getX(), simiansInHand);
                    for (int i = 0; i < nb; i++) {
                        game.discard(SIMIAN_SPIRIT_GUIDE).add(R);
                    }
                    simiansInHand -= nb;
                    toPay = toPay.minus(Mana.of(0, 0, 0, 0, 0, nb));
                } else if (petalsInHand > 0) {
                    int nb = Math.min(toPay.getX(), petalsInHand);
                    for (int i = 0; i < nb; i++) {
                        game.discard(LOTUS_PETAL).add(B);
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
        if (game.getBoard().count(CREATURES) > 0) {
            return "I reanimated a monster";
        }
        return null;
    }
}
