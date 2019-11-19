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
        return complete(game, game.getPool(), new ArrayList<>(sources), cost);
    }

    static Optional<Plan> complete(Game game, Mana pool, List<ManaSource> sources, Mana cost) {
        Plan plan = new Plan();
        while (!pool.contains(cost)) {
            Mana.Extraction result = pool.extract(cost);
            Optional<Plan> next = partial(game, pool, sources, result.getNotExtracted());
            if (!next.isPresent()) {
                return Optional.empty();
            }
            plan.addAll(next.get());
            pool = pool.plus(plan.produce());
        }

        // TODO: remove steps if we have too much mana

        // we did it
        return Optional.of(plan);
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

    /**
     * Returns a plan that produces a part of the given cost
     */
    static Optional<Plan> partial(Game game, Mana pool, List<ManaSource> sources, Mana cost) {
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
                        return Optional.of(new Plan(new Plan.Step(source, sourceCost, selectedProduction)));
                    } else {
                        if(selectedProduction.contains(cost)) {
                            // maybe we can borrow in pool (ex: Dark Ritual)
                            if(pool.contains(cost)) {
                                return Optional.of(new Plan(new Plan.Step(source, sourceCost, selectedProduction)));
                            }
                        }
                        // otherwise look for remaining source(s) to pay the cost
                        List<ManaSource> remainingSources = new ArrayList<>(sources);
                        remainingSources.remove(source);
                        Optional<Plan> sub = complete(game, pool, remainingSources, sourceCost);
                        if (sub.isPresent()) {
                            for (Plan.Step step : sub.get()) {
                                sources.remove(step);
                            }
                            sources.remove(source);
                            sub.get().add(new Plan.Step(source, sourceCost, selectedProduction));
                            return sub;
                        }
                    }
                }
            }
        }
        return Optional.empty();
    }

    @Builder
    public static class Plan extends ArrayList<Plan.Step> {

        private Plan() {
        }

        private Plan(Step step) {
            super(1);
            add(step);
        }

        public void execute(Game game) {
            for (Step step : this) {
                step.execute(game);
            }
        }

        /**
         * Computes the global mana produced by the plan (also removing inner costs)
         */
        public Mana produce() {
            Mana sum = Mana.zero();
            for (Step step : this) {
                sum = sum.minus(step.cost).plus(step.produce);
            }
            return sum;
        }

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
