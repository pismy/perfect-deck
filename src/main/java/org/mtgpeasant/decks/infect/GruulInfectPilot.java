package org.mtgpeasant.decks.infect;

import org.mtgpeasant.perfectdeck.common.cards.Cards;
import org.mtgpeasant.perfectdeck.common.mana.Mana;
import org.mtgpeasant.perfectdeck.common.matchers.MulliganRules;
import org.mtgpeasant.perfectdeck.goldfish.*;

import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import static java.util.Collections.singleton;
import static org.mtgpeasant.perfectdeck.common.mana.Mana.zero;
import static org.mtgpeasant.perfectdeck.goldfish.ManaSource.Landing.with;
import static org.mtgpeasant.perfectdeck.goldfish.ManaSource.*;
import static org.mtgpeasant.perfectdeck.goldfish.Permanent.*;

public class GruulInfectPilot extends DeckPilot<Game> implements Seer.SpellsPlayer {

    private static final Mana G = Mana.G();
    private static final Mana R = Mana.R();
    private static final Mana G1 = Mana.of("1G");
    private static final Mana R1 = Mana.of("1R");
    private static final Mana R2 = Mana.of("2R");
    private static final Mana GG = Mana.of("GG");
    private static final Mana TWO = Mana.of("2");
    private static final Mana X = Mana.one();

    // LANDS
    private static final String FOREST = "forest";
    private static final String MOUNTAIN = "mountain";
    private static final String PENDELHAVEN = "pendelhaven";
    private static final String CRUMBLING_VESTIGE = "crumbling vestige";

    // CREATURES
    private static final String ICHORCLAW_MYR = "ichorclaw myr";
    private static final String GLISTENER_ELF = "glistener elf";
    private static final String BLIGHT_MAMBA = "blight mamba";

    // BOOSTS
    private static final String RANCOR = "rancor";
    private static final String SEAL_OF_STRENGTH = "seal of strength"; // (enchant) G: sacrifice: +3/+3
    private static final String SCALE_UP = "scale up"; // G: crea become 6/4
    private static final String VINES_OF_VASTWOOD = "vines of vastwood"; // (instant) GG: +4/+4
    private static final String GIANT_GROWTH = "giant growth";
    private static final String LARGER_THAN_LIFE = "larger than life"; // 1G: +4/+4
    private static final String MUTAGENIC_GROWTH = "mutagenic growth"; // (-2 life): +2/+2
    private static final String GROUNDSWELL = "groundswell"; // G: +2/+2; landfall: +4/+4
    private static final String RANGER_S_GUILE = "ranger's guile"; // G: +1/+1
    private static final String MIGHT_OF_OLD_KROSA = "might of old krosa"; // G: +4/+4 on your turn
    private static final String BLOSSOMING_DEFENSE = "blossoming defense"; // G: +2/+2
    private static final String RECKLESS_CHARGE = "reckless charge";
    private static final String RECKLESS_CHARGE_FB = "reckless charge (flashback)";
    private static final String TEMUR_BATTLE_RAGE = "temur battle rage";

    // FREE MANA
    private static final String LOTUS_PETAL = "lotus petal";
    private static final String SIMIAN_SPIRIT_GUIDE = "simian spirit guide";

    // OTHERS
    private static final String GITAXIAN_PROBE = "gitaxian probe";
    private static final String MENTAL_MISSTEP = "mental misstep";
    private static final String APOSTLE_S_BLESSING = "apostle's blessing";

    //    private static String[] MANA_PRODUCERS = new String[]{SIMIAN_SPIRIT_GUIDE, LOTUS_PETAL, CRUMBLING_VESTIGE, MOUNTAIN, FOREST, PENDELHAVEN};
    private static String[] CREATURES = new String[]{GLISTENER_ELF, ICHORCLAW_MYR, BLIGHT_MAMBA};
    private static String[] RUSH = new String[]{GLISTENER_ELF, ICHORCLAW_MYR, BLIGHT_MAMBA, RANCOR, SEAL_OF_STRENGTH, SCALE_UP, VINES_OF_VASTWOOD, GIANT_GROWTH, LARGER_THAN_LIFE, MUTAGENIC_GROWTH, GROUNDSWELL, MIGHT_OF_OLD_KROSA, BLOSSOMING_DEFENSE, RANGER_S_GUILE, RECKLESS_CHARGE, TEMUR_BATTLE_RAGE, RECKLESS_CHARGE_FB};

    private static final String SCALE_UP_TAG = "*scale up";
    private static final String DOUBLE_STRIKE_TAG = "*double strike";
    private static final String TEMP_BOOST = "*+1/+1";

    private static MulliganRules rules;

    static {
        try {
            rules = MulliganRules.parse(new InputStreamReader(GruulInfectPilot.class.getResourceAsStream("/gruul-infect-rules.txt")));
        } catch (IOException e) {
            rules = null;
            System.err.println(e);
        }
    }

    public GruulInfectPilot(Game game) {
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
    public void firstMainPhase() {
        // whichever the situation, if I have a probe in hand: play it
        while (playOneOf(GITAXIAN_PROBE).isPresent()) {
        }

        // play all petals
        while (playOneOf(LOTUS_PETAL).isPresent()) {
        }

        Optional<Seer.VictoryRoute> victoryRoute = Seer.findRouteToVictory(this, RUSH);
        if (victoryRoute.isPresent()) {
            game.log(">> I can rush now with: " + victoryRoute);
            victoryRoute.get().play(this);
            return;
        }

        // else play boosts in best order
        List<Permanent> creatures = game.getBattlefield().find(creaturesThatCanBeTapped());
        if (creatures.isEmpty()) {
            // no creature on board: can I cast a creature + reckless charge this turn ?
            if (game.getHand().contains(RECKLESS_CHARGE)) {
                Mana cost = R;
                Optional<String> cheapestCreature = game.getHand().findFirst(GLISTENER_ELF, ICHORCLAW_MYR, BLIGHT_MAMBA);
                if (cheapestCreature.isPresent()) {
                    cost = cost.plus(
                            cheapestCreature.get().equals(GLISTENER_ELF) ? G : cheapestCreature.get().equals(ICHORCLAW_MYR) ? TWO : G1);
                    if (canPay(cost)) {
                        produce(cost);
                        //
                        game.log(">>> I can cast [" + cheapestCreature.get() + "] + [" + RECKLESS_CHARGE + "]");
                        play(cheapestCreature.get());
                        play(RECKLESS_CHARGE);
                        creatures = game.getBattlefield().find(creaturesThatCanBeTapped());
                    }
                }
            }
        }
        if (creatures.isEmpty()) {
            return;
        }

        Collection<String> boostsToPlay = game.isLanded() ?
                game.getHand().findAll(MUTAGENIC_GROWTH, SCALE_UP, RANCOR, GROUNDSWELL, MIGHT_OF_OLD_KROSA, GIANT_GROWTH, SEAL_OF_STRENGTH, BLOSSOMING_DEFENSE, LARGER_THAN_LIFE, VINES_OF_VASTWOOD, RANGER_S_GUILE, RECKLESS_CHARGE, /*TEMUR_BATTLE_RAGE,*/ RECKLESS_CHARGE_FB)
                : game.getHand().findAll(MUTAGENIC_GROWTH, SCALE_UP, RANCOR, MIGHT_OF_OLD_KROSA, GIANT_GROWTH, SEAL_OF_STRENGTH, BLOSSOMING_DEFENSE, LARGER_THAN_LIFE, VINES_OF_VASTWOOD, GROUNDSWELL, RANGER_S_GUILE, RECKLESS_CHARGE, /*TEMUR_BATTLE_RAGE,*/ RECKLESS_CHARGE_FB);
        boostsToPlay.forEach(card -> {
            if (canPlay(card)) {
                play(card);
            }
        });
    }

    @Override
    public void combatPhase() {
        List<Permanent> creatures = game.getBattlefield().find(creaturesThatCanBeTapped());
        if (creatures.isEmpty()) {
            return;
        }

        // use untapped pendelhavens to boost
        game.getBattlefield().find(withName(PENDELHAVEN).and(untapped())).forEach(card -> {
            game.tap(card);
            creatures.get(0).addCounter(TEMP_BOOST, 1);
        });

        // sacrifice all seals
        game.getBattlefield().find(withName(SEAL_OF_STRENGTH)).forEach(card -> {
            game.sacrifice(card);
            creatures.get(0).addCounter(TEMP_BOOST, 3);
        });

        // attach with all creatures
        creatures.forEach(card -> {
            int strength = strength(card);
            game.tapForAttack(card, strength);
            game.poisonOpponent(strength);
        });
    }

    @Override
    public void secondMainPhase() {
        // cast 1 creature if none on battlefield
        if (game.getBattlefield().count(withType(Game.CardType.creature)) == 0) {
            playOneOf(CREATURES);
        }

        // play all rancors & seals
        while (playOneOf(RANCOR, SEAL_OF_STRENGTH).isPresent()) {

        }

        // cast extra creatures
        while (playOneOf(CREATURES).isPresent()) {

        }

        // finally land if not done
        if (!game.isLanded()) {
            boolean hasRed = game.getBattlefield().findFirst(withName(MOUNTAIN)).isPresent();
            boolean hasGreen = game.getBattlefield().findFirst(withName(PENDELHAVEN, FOREST)).isPresent();

            // pendelhaven in priority
            playOneOf(
                    hasGreen ? "_" : PENDELHAVEN,
                    hasGreen ? "_" : FOREST,
                    hasRed ? "_" : MOUNTAIN,
                    PENDELHAVEN,
                    FOREST,
                    MOUNTAIN
                    //              CRUMBLING_VESTIGE ??
            );
        }
    }

    @Override
    public void endingPhase() {
        if (game.getHand().size() > 7) {
            discard(game.getHand().size() - 7);
        }
    }

    @Override
    public String checkWin() {
        String win = super.checkWin();
        if (win != null && game.getCurrentTurn() == 1) {
            System.out.println("Win T1 !!!");
        }
        return win;
    }

    List<ManaSource> manaSources() {
        List<ManaSource> sources = new ArrayList<>();
        sources.addAll(getTapSources(game, CRUMBLING_VESTIGE, zero(), singleton(X)));
        sources.addAll(getTapSources(game, MOUNTAIN, zero(), singleton(R)));
        sources.addAll(getTapSources(game, FOREST, zero(), singleton(G)));
        sources.add(landing(
                with(FOREST, G),
                with(MOUNTAIN, R),
                with(CRUMBLING_VESTIGE, G, R)
        ));
        sources.addAll(getDiscardSources(game, SIMIAN_SPIRIT_GUIDE, singleton(R)));
        sources.addAll(getSacrificeSources(game, LOTUS_PETAL, Mana.zero(), oneOf(R, G)));
        sources.addAll(getTapSources(game, PENDELHAVEN, zero(), singleton(G)));
        return sources;
    }

    boolean canPay(Mana cost) {
//        // potential mana pool is current pool + untapped lands + petals on battlefield
//        Mana potentialPool = game.getPool()
//                .plus(Mana.of(0, 0, game.getBattlefield().count(withName(MANA_PRODUCERS).and(untapped())), 0, 0, 0));
//        return potentialPool.contains(cost);
        return ManaProductionPlanner.plan(game, manaSources(), cost).isPresent();
    }

    void produce(Mana cost) {
//        while (!game.canPay(cost)) {
//            Optional<Permanent> producer = game.getBattlefield().findFirst(withName(FOREST, PENDELHAVEN, LOTUS_PETAL).and(untapped()));
//            if (producer.isPresent()) {
//                if (producer.get().getCard().equals(LOTUS_PETAL)) {
//                    game.sacrifice(producer.get());
//                    game.add(G);
//                } else {
//                    // a land
//                    game.tapLandForMana(producer.get(), G);
//                }
//            } else {
//                // can't preparePool !!!
//                return;
//            }
//        }
        if (!ManaProductionPlanner.maybeProduce(game, manaSources(), cost)) {
            throw new IllegalActionException("Can't produce " + cost);
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

    @Override
    public boolean canPlay(String card) {
        // first check has card
        switch (card) {
            case RECKLESS_CHARGE_FB:
                if (!game.getGraveyard().contains(RECKLESS_CHARGE)) {
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
        // first check card is in hand
        if (!game.getHand().contains(card)) {
            return false;
        }
        switch (card) {
            case FOREST:
            case MOUNTAIN:
            case CRUMBLING_VESTIGE:
            case PENDELHAVEN:
                return !game.isLanded();
            // FREE MANA
            case LOTUS_PETAL:
            case SIMIAN_SPIRIT_GUIDE:
                return true;

            case GITAXIAN_PROBE:
                return true;

            // creatures
            case GLISTENER_ELF:
                return canPay(G);
            case ICHORCLAW_MYR:
                return canPay(TWO);
            case BLIGHT_MAMBA:
                return canPay(G1);

            // BOOSTS
            case MUTAGENIC_GROWTH: // (-2 life): +2/+2
                // need a target creature ready to attack
                return game.getBattlefield().findFirst(creaturesThatCanBeTapped()).isPresent();
            case SCALE_UP: // G: crea become 6/4
                // need a target creature ready to attack & G
                return game.getBattlefield().findFirst(creaturesThatCanBeTapped().and(notWithTag(SCALE_UP_TAG))).isPresent() && canPay(G);
            case GIANT_GROWTH: // G: +3/+3
            case GROUNDSWELL: // G: +2/+2; landfall: +4/+4
            case RANGER_S_GUILE: // G: +1/+1
            case MIGHT_OF_OLD_KROSA: // G: +4/+4 on your turn
            case BLOSSOMING_DEFENSE: // G: +2/+2
                // need a target creature ready to attack & G
                return game.getBattlefield().findFirst(creaturesThatCanBeTapped()).isPresent() && canPay(G);
            case LARGER_THAN_LIFE: // 1G: +4/+4
                // need a target creature ready to attack & 1G
                return game.getBattlefield().findFirst(creaturesThatCanBeTapped()).isPresent() && canPay(G1);
            case VINES_OF_VASTWOOD: // (instant) GG: +4/+4
                // need a target creature && GG
                return game.getBattlefield().findFirst(creaturesThatCanBeTapped()).isPresent() && canPay(GG);
            case RECKLESS_CHARGE:
                // need a creature & R
                return game.getBattlefield().findFirst(withType(Game.CardType.creature)).isPresent() && canPay(R);
            case RECKLESS_CHARGE_FB:
                // need a creature & R2
                return game.getBattlefield().findFirst(withType(Game.CardType.creature)).isPresent() && canPay(R2);
            case TEMUR_BATTLE_RAGE:
                // need a creature ready to attack & R1
                return game.getBattlefield().findFirst(creaturesThatCanBeTapped().and(notWithTag(DOUBLE_STRIKE_TAG))).isPresent() && canPay(R1);

            // enchantments
            case RANCOR:
                // need a target creature (any)
                return game.getBattlefield().findFirst(withType(Game.CardType.creature)).isPresent() && canPay(G);
            case SEAL_OF_STRENGTH: // (enchant) G: sacrifice: +3/+3
                return canPay(G);
        }
        game.log("oops, unsupported card [" + card + "]");
        return false;
    }

    @Override
    public boolean play(String card) {
        switch (card) {
            case FOREST:
            case MOUNTAIN:
            case CRUMBLING_VESTIGE:
            case PENDELHAVEN:
                game.land(card);
                return true;

            case LOTUS_PETAL:
                game.castArtifact(card, Mana.zero());
                return true;

            case SIMIAN_SPIRIT_GUIDE:
                game.discard(card);
                game.add(R);
                return true;

            case GITAXIAN_PROBE:
                game.castSorcery(card, Mana.zero());
                game.draw(1);
                return true;

            // creatures
            case GLISTENER_ELF:
                produce(G);
                game.castCreature(card, G);
                return true;
            case ICHORCLAW_MYR:
                produce(TWO);
                game.castCreature(card, TWO);
                return true;
            case BLIGHT_MAMBA:
                produce(G1);
                game.castCreature(card, G1);
                return true;

            // BOOSTS
            case MUTAGENIC_GROWTH: {
                // target a creature ready to attack
                Permanent targetCreature = game.getBattlefield().findFirst(creaturesThatCanBeTapped()).get();
                game.castInstant(card, Mana.zero());
                targetCreature.addCounter(TEMP_BOOST, 2);
                return true;
            }
            case SCALE_UP: {
                // target a creature ready to attack
                Permanent targetCreature = game.getBattlefield().findFirst(creaturesThatCanBeTapped().and(notWithTag(SCALE_UP_TAG))).get();
                produce(G);
                game.castSorcery(card, G);
                targetCreature.tag(SCALE_UP_TAG);
                return true;
            }
            case GIANT_GROWTH: {
                // target a creature ready to attack
                Permanent targetCreature = game.getBattlefield().findFirst(creaturesThatCanBeTapped()).get();
                produce(G);
                game.castInstant(card, G);
                targetCreature.addCounter(TEMP_BOOST, 3);
                return true;
            }
            case GROUNDSWELL: {
                // target a creature ready to attack
                Permanent targetCreature = game.getBattlefield().findFirst(creaturesThatCanBeTapped()).get();
                produce(G);
                game.castInstant(card, G);
                targetCreature.addCounter(TEMP_BOOST, game.isLanded() ? 4 : 3);
                return true;
            }
            case RANGER_S_GUILE: {
                // target a creature ready to attack
                Permanent targetCreature = game.getBattlefield().findFirst(creaturesThatCanBeTapped()).get();
                produce(G);
                game.castInstant(card, G);
                targetCreature.addCounter(TEMP_BOOST, 1);
                return true;
            }
            case MIGHT_OF_OLD_KROSA: {
                // target a creature ready to attack
                Permanent targetCreature = game.getBattlefield().findFirst(creaturesThatCanBeTapped()).get();
                produce(G);
                game.castInstant(card, G);
                targetCreature.addCounter(TEMP_BOOST, 4);
                return true;
            }
            case BLOSSOMING_DEFENSE: {
                // target a creature ready to attack
                Permanent targetCreature = game.getBattlefield().findFirst(creaturesThatCanBeTapped()).get();
                produce(G);
                game.castInstant(card, G);
                targetCreature.addCounter(TEMP_BOOST, 2);
                return true;
            }
            case LARGER_THAN_LIFE: {
                // target a creature ready to attack
                Permanent targetCreature = game.getBattlefield().findFirst(creaturesThatCanBeTapped()).get();
                produce(G1);
                game.castSorcery(card, G1);
                targetCreature.addCounter(TEMP_BOOST, 4);
                return true;
            }
            case VINES_OF_VASTWOOD: {
                // target a creature ready to attack
                Permanent targetCreature = game.getBattlefield().findFirst(creaturesThatCanBeTapped()).get();
                produce(GG);
                game.castSorcery(card, GG);
                targetCreature.addCounter(TEMP_BOOST, 4);
                return true;
            }
            case RECKLESS_CHARGE: {
                // target preferably a creature with summon sickness
                Permanent targetCreature = game.getBattlefield().findFirst(withType(Game.CardType.creature).and(withSickness()))
                        .orElseGet(() -> game.getBattlefield().findFirst(creaturesThatCanBeTapped()).get());
                produce(R);
                game.castSorcery(card, R);
                targetCreature.addCounter(TEMP_BOOST, 3);
                targetCreature.setSickness(false);
                return true;
            }
            case RECKLESS_CHARGE_FB: {
                // target preferably a creature with summon sickness
                Permanent targetCreature = game.getBattlefield().findFirst(withType(Game.CardType.creature).and(withSickness()))
                        .orElseGet(() -> game.getBattlefield().findFirst(creaturesThatCanBeTapped()).get());
                produce(R2);
                game.cast(RECKLESS_CHARGE, Game.Area.graveyard, Game.Area.exile, R2, Game.CardType.sorcery);
                targetCreature.addCounter(TEMP_BOOST, 3);
                targetCreature.setSickness(false);
                return true;
            }
            case TEMUR_BATTLE_RAGE: {
                // target a creature ready to attack
                Permanent targetCreature = game.getBattlefield().findFirst(creaturesThatCanBeTapped()).get();
                produce(R1);
                game.castSorcery(card, R1);
                targetCreature.tag(DOUBLE_STRIKE_TAG);
                return true;
            }

            // enchantments
            case RANCOR: {
                // target preferably a creature ready to attack, or else any creature
                Permanent targetCreature = game.getBattlefield().findFirst(creaturesThatCanBeTapped())
                        .orElseGet(() -> game.getBattlefield().findFirst(withType(Game.CardType.creature)).get());
                produce(G);
                game.castEnchantment(card, G).tag("on:" + targetCreature.getCard());
                targetCreature.incrCounter(RANCOR);
                return true;
            }
            case SEAL_OF_STRENGTH: {
                produce(G);
                game.castEnchantment(card, G);
                return true;
            }
        }
        game.log("oops, unsupported card [" + card + "]");
        return false;
    }

    int strength(Permanent creature) {
        // strength is base strength + temporary boosts + 2 * rancors
        int str = baseStrength(creature)
                + creature.getCounter(TEMP_BOOST)
                + 2 * creature.getCounter(RANCOR);
        return creature.hasTag(DOUBLE_STRIKE_TAG) ? str * 2 : str;
    }

    int baseStrength(Permanent creature) {
        switch (creature.getCard()) {
            case GLISTENER_ELF:
            case ICHORCLAW_MYR:
            case BLIGHT_MAMBA:
                return creature.hasTag(SCALE_UP_TAG) ? 6 : 1;

            default:
                return 0;
        }
    }

    void putOnBottomOfLibrary(int number) {
        for (int i = 0; i < number; i++) {
            if (game.putOnBottomOfLibraryOneOf(MENTAL_MISSTEP, APOSTLE_S_BLESSING, GITAXIAN_PROBE).isPresent()) {
                continue;
            }
            // discard extra mana producers
            int redProducers = game.getHand().count(SIMIAN_SPIRIT_GUIDE, CRUMBLING_VESTIGE, LOTUS_PETAL, MOUNTAIN);
            if (redProducers > 2 && game.putOnBottomOfLibraryOneOf(MOUNTAIN).isPresent()) {
                continue;
            }
            int greenProducers = game.getHand().count(CRUMBLING_VESTIGE, LOTUS_PETAL, FOREST, PENDELHAVEN);
            if (greenProducers > 2 && game.putOnBottomOfLibraryOneOf(FOREST).isPresent()) {
                continue;
            }
            // discard extra creatures
            if (game.getHand().count(CREATURES) >= 2 && game.putOnBottomOfLibraryOneOf(BLIGHT_MAMBA, ICHORCLAW_MYR, GLISTENER_ELF).isPresent()) {
                continue;
            }
            // discard a boost
            if (game.putOnBottomOfLibraryOneOf(
                    redProducers == 0 ? RECKLESS_CHARGE : "-",
                    redProducers == 0 ? TEMUR_BATTLE_RAGE : "-",
                    RANGER_S_GUILE,
                    VINES_OF_VASTWOOD,
                    LARGER_THAN_LIFE,
                    BLOSSOMING_DEFENSE,
                    GIANT_GROWTH,
                    GROUNDSWELL,
                    MUTAGENIC_GROWTH,
                    SEAL_OF_STRENGTH,
                    MIGHT_OF_OLD_KROSA,
                    RECKLESS_CHARGE,
                    TEMUR_BATTLE_RAGE,
                    RANCOR,
                    SCALE_UP
            ).isPresent()) {
                continue;
            }
            game.putOnBottomOfLibrary(game.getHand().getFirst());
        }
    }

    void discard(int number) {
        for (int i = 0; i < number; i++) {
            if (game.discardOneOf(MENTAL_MISSTEP, APOSTLE_S_BLESSING).isPresent()) {
                continue;
            }
            // discard extra mana producers
            int redProducers = game.getBattlefield().count(withName(SIMIAN_SPIRIT_GUIDE, CRUMBLING_VESTIGE, LOTUS_PETAL, MOUNTAIN)) + game.getHand().count(SIMIAN_SPIRIT_GUIDE, CRUMBLING_VESTIGE, LOTUS_PETAL, MOUNTAIN);
            if (redProducers > 2 && game.putOnBottomOfLibraryOneOf(MOUNTAIN).isPresent()) {
                continue;
            }
            int greenProducers = game.getBattlefield().count(withName(CRUMBLING_VESTIGE, LOTUS_PETAL, FOREST, PENDELHAVEN)) + game.getHand().count(CRUMBLING_VESTIGE, LOTUS_PETAL, FOREST, PENDELHAVEN);
            if (greenProducers > 2 && game.putOnBottomOfLibraryOneOf(FOREST).isPresent()) {
                continue;
            }
            // discard a boost
            if (game.discardOneOf(
                    redProducers == 0 ? RECKLESS_CHARGE : "-",
                    redProducers == 0 ? TEMUR_BATTLE_RAGE : "-",
                    RANGER_S_GUILE,
                    VINES_OF_VASTWOOD,
                    LARGER_THAN_LIFE,
                    BLOSSOMING_DEFENSE,
                    GIANT_GROWTH,
                    GROUNDSWELL,
                    MUTAGENIC_GROWTH,
                    SEAL_OF_STRENGTH,
                    MIGHT_OF_OLD_KROSA,
                    RECKLESS_CHARGE,
                    TEMUR_BATTLE_RAGE,
                    RANCOR,
                    SCALE_UP
            ).isPresent()) {
                continue;
            }
            game.discard(game.getHand().getFirst());
        }
    }
}
