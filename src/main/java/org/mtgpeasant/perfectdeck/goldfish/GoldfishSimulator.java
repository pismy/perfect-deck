package org.mtgpeasant.perfectdeck.goldfish;

import com.google.common.base.Predicates;
import lombok.Builder;
import lombok.Getter;
import lombok.Value;
import org.mtgpeasant.perfectdeck.common.cards.Cards;
import org.mtgpeasant.perfectdeck.common.cards.Deck;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;
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
    final boolean verbose = false;

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

        public List<Integer> getWinTurns(Predicate<GameResult> filter) {
            return results.stream()
                    .filter(filter)
                    .map(GameResult::getEndTurn)
                    .distinct()
                    .sorted()
                    .collect(Collectors.toList());
        }

        public long count(Predicate<GameResult> filter) {
            return results.stream()
                    .filter(filter)
                    .count();
        }

        public double getAverageWinTurn(Predicate<GameResult> filter) {
            long sum = results.stream()
                    .filter(filter)
                    .map(GameResult::getEndTurn)
                    .collect(Collectors.summingLong(Integer::longValue));
            long count = count(filter);
            return (double) sum / (double) count;
        }

        public double getAverageWinTurn() {
            return getAverageWinTurn(r -> r.getOutcome() == GameResult.Outcome.WON);
        }

        /**
         * Ecart type
         */
        public double getWinTurnStdDerivation(Predicate<GameResult> filter) {
            double avg = getAverageWinTurn(filter);
            double distanceSum = results.stream()
                    .filter(filter)
                    .map(r -> Math.abs(avg - r.getEndTurn()))
//                    .map(r -> ((double) r.getEndTurn() - avg) * ((double) r.getEndTurn() - avg))
                    .collect(Collectors.summingDouble(Double::doubleValue));
            long count = count(filter);
//            return Math.sqrt(distanceSum / (double) count);
            return distanceSum / (double) count;
        }

        /**
         * Ecart type
         */
        public double getWinTurnStdDerivation() {
            return getWinTurnStdDerivation(r -> r.getOutcome() == GameResult.Outcome.WON);
        }

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
        List<GameResult> results = new ArrayList<>(iterations);
        boolean onThePlay = start == Start.OTD ? false : true;
        for (int it = 0; it < iterations; it++) {
            results.add(simulateGame(deck, onThePlay));
            if (verbose) {
                System.out.println();
            }
            if (start == Start.BOTH) {
                // change every game
                onThePlay = !onThePlay;
            }
        }
        return DeckStats.builder().deck(deck).iterations(iterations).results(results).build();
    }

    GameResult simulateGame(Deck deck, boolean onThePlay) {
        StringWriter output = new StringWriter();
        Game game = new Game(new PrintWriter(output));
        DeckPilot pilot = null;
        try {
            pilot = pilotClass.getConstructor(Game.class).newInstance(game);
        } catch (Exception e) {
            throw new RuntimeException("Couldn't instantiate pilot", e);
        }

        // 1: findAll hand
        game.start(onThePlay);
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

                // untap
                pilot.untapPhase();

                // upkeep
                pilot.upkeepPhase();

                // draw (unless first turn on the play)
                if (!game.isOnThePlay() || game.getCurrentTurn() > 1) {
                    pilot.drawPhase();
                }

                // first main phase
                game.emptyPool();
                pilot.firstMainPhase();

                // combat phase
                game.emptyPool();
                pilot.combatPhase();

                // second main phase
                game.emptyPool();
                pilot.secondMainPhase();

                // end phase
                game.emptyPool();
                pilot.endingPhase();

                // check no more than 7 cards in hand
                if (game.getHand().size() > draw) {
                    throw new IllegalStateException("You shouldn't have " + game.getHand().size() + " cards in hand after ending phase.");
                }

                // check won
                String winReason = pilot.checkWin();
                if (winReason != null) {
                    output.write("===> WIN: " + winReason + "\n");
                    return GameResult.builder()
                            .onThePlay(game.isOnThePlay())
                            .mulligans(game.getMulligans())
                            .outcome(GameResult.Outcome.WON)
                            .endTurn(game.getCurrentTurn())
                            .reason(winReason)
                            .build();
                }
            }
            output.write("===> MAX TURNS REACHED\n");
            return GameResult.builder()
                    .onThePlay(game.isOnThePlay())
                    .mulligans(game.getMulligans())
                    .outcome(GameResult.Outcome.TIMEOUT)
                    .endTurn(maxTurns + 1)
                    .build();
        } catch (GameLostException gle) {
            output.write("===> LOST: " + gle.getMessage() + "\n");
            return GameResult.builder()
                    .onThePlay(game.isOnThePlay())
                    .mulligans(game.getMulligans())
                    .outcome(GameResult.Outcome.LOST)
                    .endTurn(game.getCurrentTurn())
                    .reason(gle.getMessage())
                    .build();
        } catch (Exception e) {
            throw new GameInternalError("An unexpected error occurred in a game\n\n" + output.toString(), e);
        } finally {
            if (verbose) {
                System.out.println(output.toString());
            }
        }
    }

    @Builder
    @Value
    public static class GameResult {
        public enum Outcome {WON, LOST, TIMEOUT}

        final boolean onThePlay;
        final int mulligans;
        final Outcome outcome;
        final int endTurn;
        final String reason;
    }
}
