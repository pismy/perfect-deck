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
         * Computes average win turn among game results matching the given predicate
         *
         * @param filter predicate
         * @return average win turn
         */
        public double getAverageWinTurn(Predicate<GameResult> filter) {
            long sum = results.stream()
                    .filter(filter)
                    .mapToLong(result -> result.getEndTurn() * result.getCount())
                    .sum();
            long count = count(filter);
            return (double) sum / (double) count;
        }

        /**
         * <a href="https://en.wikipedia.org/wiki/Average_absolute_deviation">Mean absolute deviation</a> around average win turn
         */
        public double getWinTurnMAD(Predicate<GameResult> filter) {
            double avg = getAverageWinTurn(filter);
            double distanceSum = results.stream()
                    .filter(filter)
                    .mapToDouble(result -> Math.abs(avg - result.getEndTurn()) * result.getCount())
                    .sum();
            long count = count(filter);
            return distanceSum / (double) count;
        }

        /**
         * <a href="https://en.wikipedia.org/wiki/Standard_deviation">Standard deviation</a> around average win turn
         */
        public double getWinTurnSD(Predicate<GameResult> filter) {
            double avg = getAverageWinTurn(filter);
            double distanceSum = results.stream()
                    .filter(filter)
                    .mapToDouble(result -> ((double) result.getEndTurn() - avg) * ((double) result.getEndTurn() - avg) * result.getCount())
                    .sum();
            long count = count(filter);
            return Math.sqrt(distanceSum / (double) count);
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
        StringWriter logsBuffers = null;
        PrintWriter logsWriter = null;
        if (out != null) {
            logsBuffers = new StringWriter();
            logsWriter = new PrintWriter(logsBuffers);
        }

        // instantiate game (from class)
//        Game game = new Game(onThePlay, writer);
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

        if (logsWriter != null) {
            logsWriter.println("=====================");
            logsWriter.println("=== New Game: " + start + " ===");
            logsWriter.println("=====================");
        }

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
                    if (logsWriter != null) {
                        logsWriter.println("===> WIN: " + winReason);
                    }
                    return GameResult.builder()
                            .start(start)
                            .mulligans(game.getMulligans())
                            .outcome(GameResult.Outcome.WON)
                            .endTurn(game.getCurrentTurn())
//                            .reason(winReason)
                            .build();
                }
            }
            if (logsWriter != null) {
                logsWriter.println("===> MAX TURNS REACHED");
            }
            return GameResult.builder()
                    .start(start)
                    .mulligans(game.getMulligans())
                    .outcome(GameResult.Outcome.TIMEOUT)
                    .endTurn(maxTurns + 1)
                    .build();
        } catch (Exception e) {
            throw new GameInternalError("An unexpected error occurred in a game\n\n" + logsWriter.toString(), e);
        } finally {
            // flush buffered logs into (real) output
            if (out != null) {
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
