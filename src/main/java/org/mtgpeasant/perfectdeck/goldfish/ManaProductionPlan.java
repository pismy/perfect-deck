package org.mtgpeasant.perfectdeck.goldfish;

import lombok.Builder;
import lombok.Value;
import org.mtgpeasant.perfectdeck.common.Mana;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public class ManaProductionPlan {

    public static boolean maybePay(Game game, List<ManaSource> sources, Mana cost) {
        Optional<Plan> plan = plan(game, sources, cost);
        if (plan.isPresent()) {
            plan.get().execute(game);
            game.pay(cost);
            return true;
        } else {
            return false;
        }
    }

    public static Optional<Plan> plan(Game game, List<ManaSource> sources, Mana cost) {
        List<ManaSource> usableSources = new ArrayList<>(sources);
        List<Plan.Step> steps = new ArrayList<>();

        // first remove all I can from actual pool
        // TODO

        // TODO: concentrate on uncolored mana producers first ? or managed by sources order ?

        while (!cost.isEmpty()) {
            Optional<Plan.Step> next = findFirst(game, usableSources, cost);
            if (!next.isPresent()) {
                return Optional.empty();
            }
            steps.add(next.get());
            cost = cost.minus(next.get().mana);
        }

        // we did it
        return Optional.of(new Plan(steps));
    }

    static Optional<Plan.Step> findFirst(Game game, List<ManaSource> sources, Mana cost) {
        for (ManaSource source : sources) {
            Set<Mana> produceable = source.canProduce(game);
            for (Mana mana : produceable) {
                if (cost.contains(mana)) { // TODO: not the right test !!!
                    // use this source to produce this mana
                    sources.remove(source);
                    return Optional.of(new Plan.Step(mana, source));
                }
            }
        }
        return Optional.empty();
    }

    @Builder
    @Value
    public static class Plan {
        final List<Step> steps;

        public void execute(Game game) {
            for (Step prod : steps) {
                prod.source.doProduce(game, prod.mana);
            }
        }

        @Override
        public String toString() {
            return steps.toString();
        }

        @Builder
        @Value
        public static class Step {
            final Mana mana;
            final ManaSource source;

            @Override
            public String toString() {
                return source + " to produce " + mana;
            }
        }
    }
}
