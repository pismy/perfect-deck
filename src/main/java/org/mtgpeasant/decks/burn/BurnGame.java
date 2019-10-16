package org.mtgpeasant.decks.burn;

import lombok.Getter;
import org.mtgpeasant.perfectdeck.common.Mana;
import org.mtgpeasant.perfectdeck.goldfish.Game;

import java.io.PrintWriter;
import java.util.Arrays;

/**
 * Extends {@link Game} and manages several burn specific stuff:
 * <ul>
 * <li>damage dealt this turn (for spectacle cost and needle drop)</li>
 * <li>non-creature spells triggers (prowess, thermo alchemist, ...)</li>
 * <li>number of instant and sorceries in GY (for ghitu lavarunner)</li>
 * </ul>
 */
@Getter
class BurnGame extends Game implements BurnCards {
    private int damageDealtThisTurn = 0;
    private int prowessBoost = 0;

    public BurnGame(boolean onThePlay, PrintWriter logs) {
        super(onThePlay, logs);
    }

    @Override
    protected Game startNextTurn() {
        damageDealtThisTurn = 0;
        prowessBoost = 0;
        return super.startNextTurn();
    }

    @Override
    protected Game _damageOpponent(int damage) {
        damageDealtThisTurn += damage;
        return super._damageOpponent(damage);
    }

    @Override
    public Game cast(String cardName, Area from, Area to, Mana cost) {
        super.cast(cardName, from, to, cost);

        CardType type = typeof(cardName);

        // trigger instants and sorceries
        if (type == CardType.instant || type == CardType.sorcery) {
            // trigger all untapped thermo
            getUntapped(THERMO_ALCHEMIST).forEach(thermo -> {
                damageOpponent(1, "thermo ability");
            });

            // trigger all electrostatic fields
            getBoard().findAll(ELECTROSTATIC_FIELD).forEach(archer -> damageOpponent(1, "electrostatic field trigger"));

            // untap ghitu lavarunners if at least 2 instant and sorceries ?
            int instantsAndSorceriesInGY = countInGraveyard(CardType.instant, CardType.sorcery);
            if (instantsAndSorceriesInGY >= 2) {
                getTapped().findAll(GHITU_LAVARUNNER).forEach(this::untap);
            }
        }

        // trigger non-creatures
        if (type != CardType.creature && type != CardType.land) {
            // trigger all archer
            getBoard().findAll(FIREBRAND_ARCHER).forEach(archer -> damageOpponent(1, "firebrand trigger"));

            // prowess
            prowessBoost += getBoard().count(MONASTERY_SWIFTSPEAR) * 1;
        }

        // if it was a creature: tap it unless it has haste (to simulate summoning sickness)
        if (type == CardType.creature) {
            if (cardName != MONASTERY_SWIFTSPEAR && (cardName != GHITU_LAVARUNNER || countInGraveyard(CardType.instant, CardType.sorcery) < 2)) {
                // doesn't have haste: tap
                tap(cardName);
            }
        }

        return this;
    }

    public int countInGraveyard(CardType... types) {
        return Arrays.stream(types).mapToInt(type -> getGraveyard().count(this.cardsOfType(type))).sum();
    }

    String[] cardsOfType(CardType type) {
        switch (type) {
            case creature:
                return new String[]{MONASTERY_SWIFTSPEAR, THERMO_ALCHEMIST, KELDON_MARAUDERS, GHITU_LAVARUNNER, ORCISH_HELLRAISER, VIASHINO_PYROMANCER, FIREBRAND_ARCHER};
            case enchantment:
                return new String[]{CURSE_OF_THE_PIERCED_HEART};
            case instant:
                return new String[]{FIREBLAST, LIGHTNING_BOLT, NEEDLE_DROP, SEARING_BLAZE, MAGMA_JET, VOLCANIC_FALLOUT};
            case land:
                return new String[]{MOUNTAIN, FORGOTTEN_CAVE};
            case sorcery:
                return new String[]{RIFT_BOLT, LAVA_SPIKE, SKEWER_THE_CRITICS, CHAIN_LIGHTNING, FORKED_BOLT, FLAME_RIFT, GITAXIAN_PROBE, LIGHT_UP_THE_STAGE};
            case planeswalker:
            case artifact:
            default:
                return new String[0];
        }
    }

    CardType typeof(String cardName) {
        switch (cardName) {
            case MOUNTAIN:
            case FORGOTTEN_CAVE:
                return CardType.land;

            case MONASTERY_SWIFTSPEAR:
            case THERMO_ALCHEMIST:
            case KELDON_MARAUDERS:
            case GHITU_LAVARUNNER:
            case ORCISH_HELLRAISER:
            case VIASHINO_PYROMANCER:
            case FIREBRAND_ARCHER:
                return CardType.creature;

            case FIREBLAST:
            case LIGHTNING_BOLT:
            case NEEDLE_DROP:
            case SEARING_BLAZE:
            case MAGMA_JET:
            case VOLCANIC_FALLOUT:
                return CardType.instant;

            case RIFT_BOLT:
            case LAVA_SPIKE:
            case SKEWER_THE_CRITICS:
            case CHAIN_LIGHTNING:
            case FORKED_BOLT:
            case FLAME_RIFT:
            case GITAXIAN_PROBE:
            case LIGHT_UP_THE_STAGE:
                return CardType.sorcery;

            case CURSE_OF_THE_PIERCED_HEART:
                return CardType.enchantment;
        }
        return null;
    }
}
