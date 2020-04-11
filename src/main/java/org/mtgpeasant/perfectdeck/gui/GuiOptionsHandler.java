package org.mtgpeasant.perfectdeck.gui;

import org.mtgpeasant.perfectdeck.common.cards.Cards;
import org.mtgpeasant.perfectdeck.goldfish.DeckPilot;
import org.mtgpeasant.perfectdeck.goldfish.GoldfishSimulator;

public interface GuiOptionsHandler {
    /**
     * Returns the stats reference (to compare other stats to)
     */
    GoldfishSimulator.DeckStats getReference();

    /**
     * Sets the stats reference (to compare other stats to)
     */
    void setReference(GoldfishSimulator.DeckStats reference);

    /**
     * Returns the current selected deck pilot
     */
    Class<? extends DeckPilot> getPilotClass();

    /**
     * Returns the list of cards managed by the current deck pilot
     */
    Cards managedCards();

    /**
     * Determines whether to compute cumulated stats
     */
    boolean cumulated();
}
