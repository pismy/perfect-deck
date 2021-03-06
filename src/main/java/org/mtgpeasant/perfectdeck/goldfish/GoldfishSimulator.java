package org.mtgpeasant.perfectdeck.goldfish;

import com.google.common.base.Predicates;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Value;
import org.mtgpeasant.perfectdeck.common.cards.Cards;
import org.mtgpeasant.perfectdeck.common.cards.Deck;
import org.mtgpeasant.perfectdeck.goldfish.event.GameListener;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Constructor;
import java.lang.reflect.ParameterizedType;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.StreamSupport;


@Builder
@Value
public class GoldfishSimulator {
    public enum Start {OTP, OTD, BOTH}

    @Builder.Default
    final int draw = 7;
    @Builder.Default
    final Start start = Start.BOTH;
    @Builder.Default
    final int iterations = 50000;
    @Builder.Default
    final int maxTurns = 20;
    @Builder.Default
    final PrintWriter out = null;

    final Class<? extends DeckPilot> pilotClass;

    /**
     * TODO:
     * stats on mulligans & OTP + kill turn breakdown
     */
    @Builder
    @Getter
    public static class DeckStats {
        final Deck deck;
        final List<GameResult> results;
        final int iterations;

        public static Predicate<GameResult> withStart(Start start) {
            return result -> result.getStart() == start;
        }

        public static Predicate<GameResult> withMulligans(int mulligans, boolean orLower) {
            return mulligans < 0 ? Predicates.alwaysTrue() : orLower ? result -> result.getMulligans() <= mulligans : result -> result.getMulligans() == mulligans;
        }

        public static Predicate<GameResult> withEndTurn(int turn, boolean orLower) {
            return turn <= 0 ? Predicates.alwaysTrue() : orLower ? result -> result.getEndTurn() <= turn : result -> result.getEndTurn() == turn;
        }

        /**
         * Lists all win turns matching the given predicate
         *
         * @param filter predicate
         * @return ordered list of win turns
         */
        public List<Integer> getWinTurns(Predicate<GameResult> filter) {
            return results.stream()
                    .filter(filter)
                    .map(GameResult::getEndTurn)
                    .distinct()
                    .sorted()
                    .collect(Collectors.toList());
        }

        /**
         * Counts the number of game results matching the given predicate
         *
         * @param filter predicate
         * @return total count of games matching the predicate
         */
        public long count(Predicate<GameResult> filter) {
            return results.stream()
                    .filter(filter)
                    .mapToInt(GameResult::getCount)
                    .sum();
        }

        /**
         * Computes a percentage
         *
         * @param total   filter that count the total games
         * @param partial sub-filter that counts selected games
         * @return a percentage
         */
        public Percentage getPercentage(Predicate<GameResult> total, Predicate<GameResult> partial) {
            return new Percentage(count(total.and(partial)), count(total));
        }

        /**
         * Computes average win turn among game results matching the given predicate
         *
         * @param filter predicate
         * @return average win turn
         */
        public Average getAverageWinTurn(Predicate<GameResult> filter) {
            long sum = results.stream()
                    .filter(filter)
                    .mapToLong(result -> result.getEndTurn() * result.getCount())
                    .sum();
            long count = count(filter);
            double avg = (double) sum / (double) count;

            double distanceSum = results.stream()
                    .filter(filter)
                    .mapToDouble(result -> Math.abs(avg - result.getEndTurn()) * result.getCount())
                    .sum();
            double mad = distanceSum / (double) count;

//            double distanceSqSum = results.stream()
//                    .filter(filter)
//                    .mapToDouble(result -> ((double) result.getEndTurn() - avg) * ((double) result.getEndTurn() - avg) * result.getCount())
//                    .sum();
//            double standardDeviation = Math.sqrt(distanceSum / (double) count);

            return new Average(avg, mad);
        }

        /**
         * Lists all mulligans taken matching the given predicate
         *
         * @param filter predicate
         * @return ordered list of mulligans taken
         */
        public List<Integer> getMulligans(Predicate<GameResult> filter) {
            return results.stream()
                    .filter(filter)
                    .map(GameResult::getMulligans)
                    .distinct()
                    .sorted()
                    .collect(Collectors.toList());
        }

        public List<Integer> getMulligans() {
            return getMulligans(Predicates.alwaysTrue());
        }

        @Value
        public static class Percentage implements Comparable<Percentage> {
            final long count;
            final long total;

            public String toString() {
                //        return String.format("%.1f%% (%d/%d)", (100f * (float) count / (float) total), count, total);
                return String.format("%.1f%%", getPercentage());
            }

            public double getPercentage() {
                return 100d * (double) count / (double) total;
            }

            @Override
            public int compareTo(Percentage other) {
                return Double.compare(getPercentage(), other.getPercentage());
            }
        }

        @Value
        public static class Average implements Comparable<Average> {
            final double average;
            /**
             * <a href="https://en.wikipedia.org/wiki/Average_absolute_deviation">Mean absolute deviation</a> around average win turn
             */
            final double mad; // mean absolute deviation

            public String toString() {
                return String.format("%.2f ±%.2f", average, mad);
            }

            @Override
            public int compareTo(Average other) {
                return Double.compare(average, other.average);
            }
        }
    }


    public List<DeckStats> simulate(Iterable<Deck> decksProvider) {
        return StreamSupport.stream(decksProvider.spliterator(), false)
                .map(deck -> simulate(deck))
                .collect(Collectors.toList());
    }

    public DeckStats simulate(Deck deck) {
        List<GameResult> results = IntStream.range(0, iterations)
                .parallel()
                // simulate a game
                .mapToObj(idx -> simulateGame(deck, toss(start, idx)))
                // aggregate results
                .collect(Collectors.groupingBy(Function.identity()))
                .entrySet().stream()
                .map(entry -> GameResult.builder()
                        .mulligans(entry.getKey().mulligans)
                        .start(entry.getKey().start)
                        .outcome(entry.getKey().outcome)
                        .endTurn(entry.getKey().endTurn)
                        .count(entry.getValue().size())
                        .build()
                )
                .collect(Collectors.toList());
        return DeckStats.builder().deck(deck).iterations(iterations).results(results).build();
    }

    private Start toss(Start policy, int idx) {
        if (policy == Start.BOTH) {
            return idx % 2 == 0 ? Start.OTP : Start.OTD;
        } else {
            return policy;
        }
    }

    GameResult simulateGame(Deck deck, Start start) {
        // instantiate new game
        StringWriter logsBuffers = new StringWriter();
        PrintWriter logsWriter = new PrintWriter(logsBuffers, true);

        // instantiate game (from class)
        Class<? extends Game> gameClass = (Class) ((ParameterizedType) pilotClass.getGenericSuperclass()).getActualTypeArguments()[0];
        Game game = null;
        try {
            Constructor<? extends Game> constructor = gameClass.getDeclaredConstructor(Boolean.TYPE, PrintWriter.class);
            constructor.setAccessible(true);
            game = constructor.newInstance(start == Start.OTP, logsWriter);
        } catch (Exception e) {
            throw new RuntimeException("Couldn't instantiate game of type " + gameClass.getSimpleName(), e);
        }

        // instantiate deck pilot (from class)
        DeckPilot pilot = null;
        try {
            Constructor<? extends DeckPilot> constructor = pilotClass.getDeclaredConstructor(gameClass);
            constructor.setAccessible(true);
            pilot = constructor.newInstance(game);
        } catch (Exception e) {
            throw new RuntimeException("Couldn't instantiate pilot of type " + pilotClass.getSimpleName(), e);
        }
        if (pilot instanceof GameListener) {
            game.addListener((GameListener) pilot);
        }

        logsWriter.println("=====================");
        logsWriter.println("=== New Game: " + start + " ===");
        logsWriter.println("=====================");

        // 1: select opening hand
        while (true) {
            Cards library = deck.getMain().shuffle();
            Cards hand = library.draw(draw);
            if (pilot.keepHand(hand)) {
                game.keepHandAndStart(library, hand);
                break;
            }
            game.rejectHand(hand);
        }
        // 2: start and check mulligans have been taken
        pilot.start();

        if (game.getHand().size() > draw - game.getMulligans()) {
            throw new IllegalStateException("You shouldn't have " + game.getHand().size() + " cards in hand after " + game.getMulligans() + " mulligans.");
        }

        try {
            while (game.getCurrentTurn() <= maxTurns) {
                // start next turn
                game.startNextTurn();
                game.startPhase(Game.Phase.beginning);

                // untap
                pilot.untapStep();

                // upkeep
                pilot.upkeepStep();

                // draw (unless first turn on the play)
                if (!game.isOnThePlay() || game.getCurrentTurn() > 1) {
                    pilot.drawStep();
                }

                // first main phase
                game.startPhase(Game.Phase.first_main);
                pilot.firstMainPhase();

                // combat phase
                game.startPhase(Game.Phase.combat);
                pilot.combatPhase();

                // second main phase
                game.startPhase(Game.Phase.second_main);
                pilot.secondMainPhase();

                // end phase
                game.startPhase(Game.Phase.ending);
                pilot.endingPhase();

                // check no more than 7 cards in hand
                if (game.getHand().size() > draw) {
                    throw new IllegalStateException("You shouldn't have " + game.getHand().size() + " cards in hand after ending phase.");
                }

                // check won
                String winReason = pilot.checkWin();
                if (winReason != null) {
                    logsWriter.println("===> WIN: " + winReason);
                    return GameResult.builder()
                            .start(start)
                            .mulligans(game.getMulligans())
                            .outcome(GameResult.Outcome.WON)
                            .endTurn(game.getCurrentTurn())
//                            .reason(winReason)
                            .build();
                }
            }
            logsWriter.println("===> MAX TURNS REACHED");
            return GameResult.builder()
                    .start(start)
                    .mulligans(game.getMulligans())
                    .outcome(GameResult.Outcome.TIMEOUT)
                    .endTurn(maxTurns + 1)
                    .build();
        } catch (Exception e) {
            logsWriter.flush();
            logsWriter.close();
            throw new GameInternalError("An unexpected error occurred in a game", logsBuffers.toString(), e);
        } finally {
            // flush buffered logs into (real) output
            if (out != null) {
                logsWriter.flush();
                logsWriter.close();
                out.println(logsBuffers.toString());
                out.println();
            }
        }
    }

    @EqualsAndHashCode(exclude = "count")
    @Builder
    @Value
    public static class GameResult {
        public enum Outcome {WON, TIMEOUT}

        final Start start;
        final int mulligans;
        final Outcome outcome;
        final int endTurn;
        @Builder.Default
        final int count = 1;

        public boolean isOnThePlay() {
            return start == Start.OTP;
        }
    }
}
