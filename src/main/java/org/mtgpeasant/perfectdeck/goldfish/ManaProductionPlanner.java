package org.mtgpeasant.perfectdeck.goldfish;

import lombok.Builder;
import lombok.Value;
import org.mtgpeasant.perfectdeck.common.mana.Mana;

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
            Mana.Extraction result = pool.extract(cost);
            Optional<Plan.Step> next = findFirst(game, usableSources, result.getNotExtracted());
            if (!next.isPresent()) {
                return Optional.empty();
            }
            steps.add(next.get());
            pool = pool.plus(next.get().produce);
        }

        // TODO: remove steps if we have too much mana

        // we did it
        return Optional.of(new Plan(steps));
    }

//    static Mana pickOne(Mana cost) {
//        if (cost.getB() > 0) {
//            return B;
//        } else if (cost.getR() > 0) {
//            return R;
//        } else if (cost.getG() > 0) {
//            return G;
//        } else if (cost.getU() > 0) {
//            return U;
//        } else if (cost.getW() > 0) {
//            return W;
//        } else {
//            return ONE;
//        }
//    }

    static Optional<Plan.Step> findFirst(Game game, List<ManaSource> sources, Mana cost) {
        // TODO: start with coloured mana
        for (ManaSource source : sources) {
            Set<Mana> produceable = source.produces(game);
            for (Mana selectedProduction : produceable) {
                Mana.Extraction extraction = selectedProduction.extract(cost);
                if (!extraction.getExtracted().isEmpty()) {
                    // yes, this source produces required mana
                    Mana sourceCost = source.cost(game);
//                    if (sourceCost.isEmpty()) {
//                        selectedCost = Mana.zero();
//                    } else {
////                        // can we pay one of the cost ?
////                        for (Mana aCost : cost) {
////                            List<ManaSource> remainingSources = new ArrayList<>(sources);
////                            remainingSources.remove(source);
////                            findFirst(game, remainingSources, aCost); // TODO: not unitary
////                            // TODO: pay from pool (ex: dark ritual)
////                        }
////                        TODO: need to try all alternative cost and sources until a solution is found
//                    }

                    if (sourceCost.isEmpty()) {
                        sources.remove(source);
                        return Optional.of(new Plan.Step(source, sourceCost, selectedProduction));
                    }
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
            for (Step step : steps) {
                step.execute(game);
            }
        }

        @Override
        public String toString() {
            return steps.toString();
        }

        @Builder
        @Value
        public static class Step {
            final ManaSource source;
            final Mana cost;
            final Mana produce;

            void execute(Game game) {
                source.doProduce(game, cost, produce);
            }

            @Override
            public String toString() {
                return source + (cost.isEmpty() ? "" : ", pay " + cost) + ": produce " + produce;
            }
        }
    }
}
