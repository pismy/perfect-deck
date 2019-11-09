package org.mtgpeasant.perfectdeck.goldfish;

import lombok.Builder;
import lombok.Value;
import org.mtgpeasant.perfectdeck.common.Mana;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public class ManaProductionPlanner {

    public static final Mana B = Mana.of("B");
    public static final Mana R = Mana.of("R");
    public static final Mana G = Mana.of("G");
    public static final Mana U = Mana.of("U");
    public static final Mana W = Mana.of("W");
    public static final Mana ONE = Mana.of("1");

    public static boolean maybeProduce(Game game, List<ManaSource> sources, Mana cost) {
        Optional<Plan> plan = plan(game, sources, cost);
        if (plan.isPresent()) {
            plan.get().execute(game);
            return true;
        } else {
            return false;
        }
    }

    public static Optional<Plan> plan(Game game, List<ManaSource> sources, Mana cost) {
        List<ManaSource> usableSources = new ArrayList<>(sources);
        List<Plan.Step> steps = new ArrayList<>();

        Mana pool = game.getPool();
        while (!pool.contains(cost)) {
            Mana.RemoveResult result = pool.remove(cost);
            Optional<Plan.Step> next = findFirst(game, usableSources, one(result.getNotRemoved()));
            if (!next.isPresent()) {
                return Optional.empty();
            }
            steps.add(next.get());
            pool = pool.plus(next.get().mana);
        }

        // TODO: remove a step if we have too much mana

        // we did it
        return Optional.of(new Plan(steps));
    }

    static Mana one(Mana cost) {
        if(cost.getB() > 0) {
            return B;
        } else if(cost.getR() > 0) {
            return R;
        } else if(cost.getG() > 0) {
            return G;
        } else if(cost.getU() > 0) {
            return U;
        } else if(cost.getW() > 0) {
            return W;
        } else {
            return ONE;
        }
    }

    static Optional<Plan.Step> findFirst(Game game, List<ManaSource> sources, Mana one) {
        for (ManaSource source : sources) {
            Set<Mana> produceable = source.canProduce(game);
            for (Mana mana : produceable) {
                if (mana.contains(one)) {
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
