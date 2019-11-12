package org.mtgpeasant.perfectdeck.goldfish;

import org.junit.Test;
import org.mtgpeasant.perfectdeck.common.Mana;
import org.mtgpeasant.perfectdeck.common.cards.Cards;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mtgpeasant.perfectdeck.goldfish.Permanent.permanent;
import static org.mtgpeasant.perfectdeck.goldfish.ManaSource.Landing.with;
import static org.mtgpeasant.perfectdeck.goldfish.ManaSource.*;

public class ManaProductionPlannerTest {

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
    public static final String RAKDOS_CARNARIUM = "rakdos carnarium";

    @Test
    public void plan1_should_work() {
        // GIVEN
        Cards library = Cards.of();
        Cards hand = Cards.of(SWAMP, SIMIAN_SPIRIT_GUIDE, RAKDOS_GUILDGATE, MOUNTAIN, CRUMBLING_VESTIGE);
        Game game = new Game(true, null);
        game.keepHandAndStart(library, hand);
        game.getBattlefield().add(permanent(CRUMBLING_VESTIGE, Game.CardType.land));
        game.getBattlefield().add(permanent(SWAMP, Game.CardType.land));
        game.getBattlefield().add(permanent(MOUNTAIN, Game.CardType.land));
        game.getBattlefield().add(permanent(RAKDOS_GUILDGATE, Game.CardType.land));
        game.getBattlefield().add(permanent(RAKDOS_CARNARIUM, Game.CardType.land));
        game.getBattlefield().add(permanent(LOTUS_PETAL, Game.CardType.land));
        game.getBattlefield().add(permanent(LLANOWAR_ELVES, Game.CardType.creature));

        // define sources in order of preference
        List<ManaSource> sources = new ArrayList<>();
        sources.addAll(getTapSources(game, CRUMBLING_VESTIGE, ONE));
        sources.addAll(getTapSources(game, LLANOWAR_ELVES, G));
        sources.addAll(getTapSources(game, RAKDOS_CARNARIUM, Mana.of("BR")));
        sources.addAll(getTapSources(game, SWAMP, B));
        sources.addAll(getTapSources(game, MOUNTAIN, R));
        sources.addAll(getTapSources(game, RAKDOS_GUILDGATE, B, R));
        sources.add(landing(
                with(SWAMP, B),
                with(MOUNTAIN, R),
                with(CRUMBLING_VESTIGE, B, R)
        ));
        sources.addAll(getDiscardSources(game, SIMIAN_SPIRIT_GUIDE, R));
        sources.addAll(getSacrificeSources(game, LOTUS_PETAL, R, B));

        // WHEN
        Optional<ManaProductionPlanner.Plan> plan = ManaProductionPlanner.plan(game, sources, Mana.of("2BBR"));

        // THEN
        assertThat(plan).isPresent();
        assertThat(plan.get().getSteps()).hasSize(4);
    }

    @Test
    public void plan2_should_work() {
        // GIVEN
        Cards library = Cards.of();
        Cards hand = Cards.of(SWAMP, SIMIAN_SPIRIT_GUIDE);
        Game game = new Game(true, null);
        game.keepHandAndStart(library, hand);
        game.getBattlefield().add(permanent(SWAMP, Game.CardType.land));
        game.getBattlefield().add(permanent(MOUNTAIN, Game.CardType.land));

        // define sources in order of preference
        List<ManaSource> sources = new ArrayList<>();
        sources.addAll(getTapSources(game, CRUMBLING_VESTIGE, ONE));
        sources.addAll(getTapSources(game, SWAMP, B));
        sources.addAll(getTapSources(game, MOUNTAIN, R));
        sources.add(landing(
                with(SWAMP, B),
                with(MOUNTAIN, R),
                with(CRUMBLING_VESTIGE, B, R)
        ));
        sources.addAll(getDiscardSources(game, SIMIAN_SPIRIT_GUIDE, R));
        sources.addAll(getDiscardSources(game, LOTUS_PETAL, R, B));

        // WHEN
        Optional<ManaProductionPlanner.Plan> plan = ManaProductionPlanner.plan(game, sources, Mana.of("2R"));

        // THEN
        assertThat(plan).isPresent();
        assertThat(plan.get().getSteps()).hasSize(3);
    }

}
