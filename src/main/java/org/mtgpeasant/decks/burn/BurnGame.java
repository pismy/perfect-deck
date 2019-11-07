package org.mtgpeasant.decks.burn;

import lombok.Getter;
import org.mtgpeasant.perfectdeck.common.Mana;
import org.mtgpeasant.perfectdeck.goldfish.Game;

import java.io.PrintWriter;
import java.util.Arrays;
import java.util.List;

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

    /**
     * Override to rest turn state
     */
    @Override
    protected void startNextTurn() {
        super.startNextTurn();
        damageDealtThisTurn = 0;
        prowessBoost = 0;
    }

    /**
     * Override to keep track of damage dealt this turn
     */
    @Override
    protected void _damageOpponent(int damage) {
        super._damageOpponent(damage);
        damageDealtThisTurn += damage;
    }

    /**
     * Override to implement triggers
     */
    @Override
    public void cast(String cardName, Area from, Area to, Mana cost) {
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

            // trigger all untapped kiln
            prowessBoost += countUntapped(KILN_FIEND) * 3;
        }

        // trigger non-creatures
        if (type != CardType.creature && type != CardType.land) {
            // trigger all archer
            getBoard().findAll(FIREBRAND_ARCHER).forEach(archer -> damageOpponent(1, "firebrand trigger"));

            // monastery prowess
            prowessBoost += getBoard().count(MONASTERY_SWIFTSPEAR) * 1;
        }

        // if it was a creature: tap it unless it has haste (to simulate summoning sickness)
        if (type == CardType.creature) {
            if(cardName.equals(MONASTERY_SWIFTSPEAR) || (cardName.equals(GHITU_LAVARUNNER) && countInGraveyard(CardType.instant, CardType.sorcery) >= 2)) {
                // create has haste: leave untap
            } else {
                // doesn't have haste: tap
                tap(cardName);
            }
        }
    }

    public int countInGraveyard(CardType... types) {
        List<CardType> typesList = Arrays.asList(types);
        return (int)getGraveyard().stream().filter(card -> typesList.contains(typeof(card))).count();
//        return Arrays.stream(types).mapToInt(type -> getGraveyard().count(this.cardsOfType(type))).sum();
    }

}
