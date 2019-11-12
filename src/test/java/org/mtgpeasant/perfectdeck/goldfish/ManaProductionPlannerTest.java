package org.mtgpeasant.perfectdeck.goldfish;

import org.junit.Test;
import org.mtgpeasant.perfectdeck.common.cards.Cards;
import org.mtgpeasant.perfectdeck.common.mana.Mana;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static java.util.Collections.singleton;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mtgpeasant.perfectdeck.common.mana.Mana.zero;
import static org.mtgpeasant.perfectdeck.goldfish.Game.CardType.creature;
import static org.mtgpeasant.perfectdeck.goldfish.Game.CardType.land;
import static org.mtgpeasant.perfectdeck.goldfish.ManaSource.Landing.with;
import static org.mtgpeasant.perfectdeck.goldfish.ManaSource.*;
import static org.mtgpeasant.perfectdeck.goldfish.Permanent.permanent;

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
    public static final String DARK_RITUAL = "dark ritual";
    public static final String PROPHETIC_PRISM = "prophetic prism";

    private List<ManaSource> manaSources(Game game) {
        List<ManaSource> sources = new ArrayList<>();
        sources.addAll(getTapSources(game, CRUMBLING_VESTIGE, zero(), singleton(ONE)));
        sources.addAll(getTapSources(game, LLANOWAR_ELVES, zero(), singleton(G)));
        sources.addAll(getTapSources(game, RAKDOS_CARNARIUM, zero(), singleton(Mana.of("BR"))));
        sources.addAll(getTapSources(game, SWAMP, zero(), singleton(B)));
        sources.addAll(getTapSources(game, MOUNTAIN, zero(), singleton(R)));
        sources.addAll(getTapSources(game, RAKDOS_GUILDGATE, zero(), oneOf(B, R)));
        sources.addAll(getTapSources(game, PROPHETIC_PRISM, Mana.one(), oneOf(B, R)));
        sources.add(landing(
                with(SWAMP, B),
                with(MOUNTAIN, R),
                with(CRUMBLING_VESTIGE, B, R)
        ));
        sources.addAll(getDiscardSources(game, SIMIAN_SPIRIT_GUIDE, singleton(R)));
        sources.addAll(getSacrificeSources(game, LOTUS_PETAL, zero(), oneOf(R, B)));
        sources.addAll(getCastInstantSources(game, DARK_RITUAL, B, oneOf(Mana.of("BBB"))));
        return sources;
    }

    @Test
    public void plan1_should_work() {
        // GIVEN
        Cards library = Cards.of();
        Cards hand = Cards.of(SWAMP, SIMIAN_SPIRIT_GUIDE, RAKDOS_GUILDGATE, MOUNTAIN, CRUMBLING_VESTIGE);
        Game game = GameMock.mock(true, hand, library, Cards.empty(), Collections.emptyList(), Collections.emptyList());
        game.getBattlefield().add(permanent(CRUMBLING_VESTIGE, land));
        game.getBattlefield().add(permanent(SWAMP, land));
        game.getBattlefield().add(permanent(MOUNTAIN, land));
        game.getBattlefield().add(permanent(RAKDOS_GUILDGATE, land));
        game.getBattlefield().add(permanent(RAKDOS_CARNARIUM, land));
        game.getBattlefield().add(permanent(LOTUS_PETAL, land));
        game.getBattlefield().add(permanent(LLANOWAR_ELVES, creature));

        // define sources in order of preference
        List<ManaSource> sources = manaSources(game);

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
        Game game = GameMock.mock(true, hand, library, Cards.empty(), Collections.emptyList(), Collections.emptyList());
        game.getBattlefield().add(permanent(SWAMP, land));
        game.getBattlefield().add(permanent(MOUNTAIN, land));

        // define sources in order of preference
        List<ManaSource> sources = manaSources(game);

        // WHEN
        Optional<ManaProductionPlanner.Plan> plan = ManaProductionPlanner.plan(game, sources, Mana.of("2R"));

        // THEN
        assertThat(plan).isPresent();
        assertThat(plan.get().getSteps()).hasSize(3);
    }

}
