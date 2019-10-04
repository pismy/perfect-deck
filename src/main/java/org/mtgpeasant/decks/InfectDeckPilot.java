package org.mtgpeasant.decks;

import org.mtgpeasant.perfectdeck.common.Mana;
import org.mtgpeasant.perfectdeck.common.cards.Cards;
import org.mtgpeasant.perfectdeck.common.matchers.MulliganRules;
import org.mtgpeasant.perfectdeck.goldfish.DeckPilot;
import org.mtgpeasant.perfectdeck.goldfish.Game;

import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Optional;

public class InfectDeckPilot extends DeckPilot {

    public static final Mana G = Mana.of("G");
    public static final Mana G1 = Mana.of("G1");
    public static final Mana TWO = Mana.of("2");

    // LANDS
    public static final String FOREST = "forest";
    public static final String PENDELHAVEN = "pendelhaven";

    // CREATURES
    public static final String ICHORCLAW_MYR = "ichorclaw myr";
    public static final String GLISTENER_ELF = "glistener elf";
    public static final String BLIGHT_MAMBA = "blight mamba";

    // BOOSTS
    public static final String SCALE_UP = "scale up";
    public static final String VINES_OF_VASTWOOD = "vines of vastwood";
    public static final String GIANT_GROWTH = "giant growth";
    public static final String SEAL_OF_STRENGTH = "seal of strength";
    public static final String RANCOR = "rancor";
    public static final String LARGER_THAN_LIFE = "larger than life";
    public static final String INVIGORATE = "invigorate";
    public static final String MUTAGENIC_GROWTH = "mutagenic growth";
    public static final String GROUNDSWELL = "groundswell";

    // FREE MANA
    public static final String LOTUS_PETAL = "lotus petal";

    // OTHERS
    public static final String GITAXIAN_PROBE = "gitaxian probe";
    public static final String MENTAL_MISSTEP = "mental misstep";
    public static final String APOSTLE_S_BLESSING = "apostle's blessing";

    private static String[] MANA_PRODUCERS = new String[]{PENDELHAVEN, FOREST, LOTUS_PETAL};
    private static String[] CREATURES = new String[]{GLISTENER_ELF, ICHORCLAW_MYR, BLIGHT_MAMBA};

    private static MulliganRules rules;

    static {
        try {
            rules = MulliganRules.parse(new InputStreamReader(InfectDeckPilot.class.getResourceAsStream("/infect-rules.txt")));
        } catch (IOException e) {
            rules = null;
            System.err.println(e);
        }
    }

    public InfectDeckPilot(Game game) {
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
        getRid(game.getMulligans());
    }

    @Override
    public void firstMainPhase() {
        // whichever the situation, if I have a probe in hand: play it
        while (game.getHand().contains(GITAXIAN_PROBE)) {
            game.castNonPermanent(GITAXIAN_PROBE, Mana.zero()).draw(1);
        }

        // land
        // pendelhaven if no vines in hand and no forest on board
        if (game.getHand().contains(PENDELHAVEN) && (!game.getHand().contains(VINES_OF_VASTWOOD) || game.getBoard().contains(FOREST))) {
            game.land(PENDELHAVEN);
        } else if (game.getHand().contains(FOREST)) {
            game.land(FOREST);
        }

        // play all petals
        while (game.getHand().contains(LOTUS_PETAL)) {
            game.castPermanent(LOTUS_PETAL, Mana.zero());
        }
    }

    @Override
    public void combatPhase() {
        // boost all creatures and attack
        Cards creatures = game.getBoard().findAll(CREATURES);
        if (creatures.isEmpty()) {
            return;
        }
        // play all mutagenic
        while (game.getHand().contains(MUTAGENIC_GROWTH)) {
            game.castNonPermanent(MUTAGENIC_GROWTH, Mana.zero()).poisonOpponent(2);
        }
        // play all scale up
        int castableScaleUp = Math.min(creatures.size(), game.getHand().count(SCALE_UP));
        while (castableScaleUp > 0 && canPay(G)) {
            pay(G);
            game.castNonPermanent(SCALE_UP, G).poisonOpponent(5);
            castableScaleUp--;
        }
        // play all rancors
        while (game.getHand().contains(RANCOR) && canPay(G)) {
            pay(G);
            game.castPermanent(RANCOR, G);
        }
        // play all invigorates (if forest)
        if (game.getBoard().contains(FOREST)) {
            while (game.getHand().contains(INVIGORATE)) {
                game.castNonPermanent(INVIGORATE, Mana.zero()).poisonOpponent(4).damageOpponent(-3);
            }
        }
        // play all groundswell (if landed)
        if (game.isLanded()) {
            while (game.getHand().contains(GROUNDSWELL) && canPay(G)) {
                pay(G);
                game.castNonPermanent(GROUNDSWELL, G).poisonOpponent(4);
            }
        }
        // play all growths
        while (game.getHand().contains(GIANT_GROWTH) && canPay(G)) {
            pay(G);
            game.castNonPermanent(GIANT_GROWTH, G).poisonOpponent(3);
        }
        // play all seals
        while (game.getHand().contains(SEAL_OF_STRENGTH) && canPay(G)) {
            pay(G);
            game.castPermanent(SEAL_OF_STRENGTH, G);
        }
        // play all larger
        while (game.getHand().contains(LARGER_THAN_LIFE) && canPay(G1)) {
            pay(G1);
            game.castNonPermanent(LARGER_THAN_LIFE, G1).poisonOpponent(4);
        }
        // play all vines
        while (game.getHand().contains(VINES_OF_VASTWOOD) && canPay(G1)) {
            pay(G1);
            game.castNonPermanent(VINES_OF_VASTWOOD, G1).poisonOpponent(4);
        }
        // play all groundswell (if not landed)
        if (!game.isLanded()) {
            while (game.getHand().contains(GROUNDSWELL) && canPay(G)) {
                pay(G);
                game.castNonPermanent(GROUNDSWELL, G).poisonOpponent(2);
            }
        }

        // sacrifice all seals
        game.getBoard().findAll(SEAL_OF_STRENGTH).forEach(card -> game.sacrifice(card).poisonOpponent(3));

        // attach with all creatures
        creatures.forEach(card -> game.tapForAttack(card, 1).poisonOpponent(1));

        // add rancors
        game.getBoard().findAll(RANCOR).forEach(card -> game.tap(card).poisonOpponent(2));

        // TODO: use pendlhaven to boost
    }

    @Override
    public void secondMainPhase() {
        // cast 1 creature if none on board
        if (game.getBoard().count(CREATURES) == 0) {
            if (game.getHand().contains(GLISTENER_ELF) && canPay(G)) {
                pay(G);
                game.castPermanent(GLISTENER_ELF, G);
            } else if (game.getHand().contains(BLIGHT_MAMBA) && canPay(G1)) {
                pay(G1);
                game.castPermanent(BLIGHT_MAMBA, G1);
            } else if (game.getHand().contains(ICHORCLAW_MYR) && canPay(TWO)) {
                pay(G1);
                game.castPermanent(ICHORCLAW_MYR, TWO);
            }
        }

        // play all seals
        while (game.getHand().contains(SEAL_OF_STRENGTH) && canPay(G)) {
            pay(G);
            game.castPermanent(SEAL_OF_STRENGTH, G);
        }

        // cast extra creatures
        for (String crea : game.getHand().findAll(CREATURES)) {
            Mana cost = crea.equals(GLISTENER_ELF) ? G : crea.equals(BLIGHT_MAMBA) ? G1 : TWO;
            if (canPay(cost)) {
                pay(cost);
                game.castPermanent(crea, cost);
            }
        }
    }

    @Override
    public void endingPhase() {
        if (game.getHand().size() > 7) {
            discard(game.getHand().size() - 7);
        }
    }

    void getRid(int number) {
        for (int i = 0; i < number; i++) {
            if (game.putOnBottomOfLibraryOneOf(MENTAL_MISSTEP, APOSTLE_S_BLESSING).isPresent()) {
                continue;
            }
            // discard extra lands
            if (game.getHand().count(MANA_PRODUCERS) > 2 && game.putOnBottomOfLibraryOneOf(MANA_PRODUCERS).isPresent()) {
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
            if (game.discardOneOf(MENTAL_MISSTEP, APOSTLE_S_BLESSING).isPresent()) {
                continue;
            }
            // discard extra lands
            if (game.getBoard().count(MANA_PRODUCERS) + game.getHand().count(MANA_PRODUCERS) > 3 && game.discardOneOf(MANA_PRODUCERS).isPresent()) {
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
        Mana potentialPool = game.getPool()
                .plus(Mana.of(0, 0, game.getBoard().count(MANA_PRODUCERS) - game.getTapped().count(MANA_PRODUCERS), 0, 0, 0));
        return potentialPool.contains(cost);
    }

    void pay(Mana cost) {
        while (!game.canPay(cost)) {
            Optional<String> producer = game.findFirstUntapped(FOREST, PENDELHAVEN, LOTUS_PETAL);
            if (producer.isPresent()) {
                if (producer.get().equals(LOTUS_PETAL)) {
                    game.sacrifice(LOTUS_PETAL).add(G);
                } else {
                    // a land
                    game.tapLandForMana(producer.get(), G);
                }
            } else {
                // can't pay !!!
                return;
            }
        }
    }
}
