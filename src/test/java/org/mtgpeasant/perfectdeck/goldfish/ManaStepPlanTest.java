package org.mtgpeasant.perfectdeck.goldfish;

import org.junit.Test;
import org.mtgpeasant.perfectdeck.common.Mana;
import org.mtgpeasant.perfectdeck.common.cards.Cards;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mtgpeasant.perfectdeck.goldfish.Card.card;
import static org.mtgpeasant.perfectdeck.goldfish.ManaSource.Landing.with;
import static org.mtgpeasant.perfectdeck.goldfish.ManaSource.*;

public class ManaStepPlanTest {

    public static final String SIMIAN_SPIRIT_GUIDE = "simian spirit guide";
    public static final Mana B = Mana.of("B");
    public static final Mana R = Mana.of("R");
    public static final Mana G = Mana.of("G");
    public static final Mana ONE = Mana.of("1");
    public static final String LOTUS_PETAL = "lotus petal";
    public static final String SWAMP = "swamp";
    public static final String MOUNTAIN = "mountain";
    public static final String CRUMBLING_VESTIGE = "crumbling vestige";
    public static final String RAKDOS_GUILDGATE = "rakdos guildgate";
    public static final String LLANOWAR_ELVES = "llanowar elves";

    @Test
    public void plan1_should_work() {
        // GIVEN
        Cards library = Cards.of();
        Cards hand = Cards.of(SWAMP, SIMIAN_SPIRIT_GUIDE, RAKDOS_GUILDGATE, MOUNTAIN, CRUMBLING_VESTIGE);
        Game game = new Game(true, null);
        game.keepHandAndStart(library, hand);
        game.getBoard().add(card(CRUMBLING_VESTIGE, Game.CardType.land));
        game.getBoard().add(card(SWAMP, Game.CardType.land));
        game.getBoard().add(card(MOUNTAIN, Game.CardType.land));
        game.getBoard().add(card(RAKDOS_GUILDGATE, Game.CardType.land));
        game.getBoard().add(card(LOTUS_PETAL, Game.CardType.land));
        game.getBoard().add(card(LLANOWAR_ELVES, Game.CardType.creature));

        // define sources in order of preference
        List<ManaSource> sources = new ArrayList<>();
        sources.addAll(getTapSources(game, CRUMBLING_VESTIGE, ONE));
        sources.addAll(getTapSources(game, LLANOWAR_ELVES, G));
        sources.addAll(getTapSources(game, SWAMP, B));
        sources.addAll(getTapSources(game, MOUNTAIN, R));
        sources.addAll(getTapSources(game, RAKDOS_GUILDGATE, B, R));
        sources.add(landing(
                with(SWAMP, B),
                with(MOUNTAIN, R),
                with(CRUMBLING_VESTIGE, B, R)
        ));
        sources.addAll(getDiscardSources(game, SIMIAN_SPIRIT_GUIDE, R));
        sources.addAll(ManaSource.getSacrificeSources(game, LOTUS_PETAL, R, B));

        // WHEN
        Optional<ManaProductionPlanner.Plan> plan = ManaProductionPlanner.plan(game, sources, Mana.of("2BBB"));

        // THEN
        assertThat(plan).isPresent();
//        assertThat(plan.get().getProductions())
    }
}