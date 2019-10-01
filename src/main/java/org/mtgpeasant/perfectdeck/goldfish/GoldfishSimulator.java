package org.mtgpeasant.perfectdeck.goldfish;

import lombok.Builder;
import lombok.Getter;
import lombok.Value;
import org.mtgpeasant.perfectdeck.common.cards.Cards;
import org.mtgpeasant.perfectdeck.common.cards.Deck;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;


@Builder
@Value
public class GoldfishSimulator {
    public enum Start {OTP, OTD, RANDOM}

    @Builder.Default
    final int draw = 7;
    @Builder.Default
    final Start start = Start.RANDOM;
    @Builder.Default
    final int iterations = 50000;
    @Builder.Default
    final int maxTurns = 20;

    final DeckPilot pilot;

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
                    .map(r -> ((double) r.getEndTurn() - avg) * ((double) r.getEndTurn() - avg))
                    .collect(Collectors.summingDouble(Double::doubleValue));
            long count = count(filter);
            return Math.sqrt(distanceSum / (double) count);
        }

        /**
         * Ecart type
         */
        public double getWinTurnStdDerivation() {
            return getWinTurnStdDerivation(r -> r.getOutcome() == GameResult.Outcome.WON);
        }

//        final Map<Integer, Integer> winCount = new HashMap<>();
//        int lostCount = 0;
//        int timeoutCount = 0;
//
//        private void add(GameResult result) {
//            switch (result.getOutcome()) {
//                case WON:
//                    winCount.put(result.getEndTurn(), getWinCount(result.getEndTurn()) + 1);
//                    break;
//                case LOST:
//                    lostCount++;
//                    break;
//                case TIMEOUT:
//                    timeoutCount++;
//                    break;
//            }
//        }
//
//        /**
//         * Returns the number of games won at the given turn
//         *
//         * @param turn turn number (1-based)
//         * @return number of games won at the given turn
//         */
//        public int getWinCount(int turn) {
//            return winCount.getOrDefault(turn, 0);
//        }
//
//        // TODO: comment on calcule ?
//        public double getAverageWinTurn() {
//            long sum = winCount.entrySet().stream().map(e -> e.getKey() * e.getValue()).collect(Collectors.summingLong(Integer::longValue));
//            long wonGames = iterations - lostCount - timeoutCount;
//            return (double) sum / (double) wonGames;
//        }
//
//        /**
//         * Ecart type
//         */
//        public double getWinTurnStdDerivation() {
//            double avg = getAverageWinTurn();
//            double distanceSum = winCount.entrySet().stream().map(e -> e.getValue() * ((double) e.getKey() - avg) * ((double) e.getKey() - avg)).collect(Collectors.summingDouble(Double::doubleValue));
//            long wonGames = iterations - lostCount - timeoutCount;
//            return Math.sqrt(distanceSum / (double) wonGames);
//        }
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
            if (start == Start.RANDOM) {
                // change every game
                onThePlay = !onThePlay;
            }
        }
        return DeckStats.builder().deck(deck).iterations(iterations).results(results).build();
    }

    private GameResult simulateGame(Deck deck, boolean onThePlay) {
        // 1: select hand
        int mulligans = -1;
        Cards library;
        Cards hand;
        do {
            mulligans++;
            library = deck.getMain().shuffle();
            hand = library.draw(draw);
        } while (!pilot.keepHand(onThePlay, mulligans, hand));

        Game game = new Game(library, hand);
        pilot.startGame(mulligans, game);

        if (game.getHand().size() > draw - mulligans) {
            throw new IllegalStateException("You shouldn't have " + game.getHand().size() + " cards in hand after " + mulligans + " mulligans.");
        }

        // 2: simulate a game
        try {
            while (game.getCurrentTurn() <= maxTurns) {
                // untap
                pilot.untapPhase();

                // upkeep
                pilot.upkeepPhase();

                // draw (unless first turn on the play)
                if (!onThePlay || game.getCurrentTurn() > 1) {
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
                    return GameResult.builder()
                            .onThePlay(onThePlay)
                            .outcome(GameResult.Outcome.WON)
                            .endTurn(game.getCurrentTurn())
                            .reason(winReason)
                            .build();
                }

                // init next turn
                game.startNextTurn();
            }
            return GameResult.builder()
                    .onThePlay(onThePlay)
                    .outcome(GameResult.Outcome.TIMEOUT)
                    .endTurn(maxTurns + 1)
                    .build();
        } catch (GameLostException gle) {
            return GameResult.builder()
                    .onThePlay(onThePlay)
                    .outcome(GameResult.Outcome.LOST)
                    .endTurn(game.getCurrentTurn())
                    .reason(gle.getMessage())
                    .build();
        }
    }

    @Builder
    @Value
    public static class GameResult {
        public enum Outcome {WON, LOST, TIMEOUT}

        final boolean onThePlay;
        final Outcome outcome;
        final int endTurn;
        final String reason;
    }
}
