package org.mtgpeasant.perfectdeck.goldfish;

import lombok.Builder;
import lombok.Getter;
import lombok.Value;
import org.mtgpeasant.perfectdeck.common.cards.Cards;
import org.mtgpeasant.perfectdeck.common.cards.Deck;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;


@Builder
@Value
public class GoldfishSimulator {
    public enum OnThePlay {YES, NO, RANDOM}

    @Builder.Default
    final int draw = 7;
    @Builder.Default
    final OnThePlay otp = OnThePlay.RANDOM;
    @Builder.Default
    final int iterations = 50000;
    @Builder.Default
    final int maxTurns = 5;

    final DeckPilot pilot;

    @Builder
    @Getter
    public static class DeckStats {
        final Deck deck;
        final int iterations;
        final Map<Integer, Integer> winCount = new HashMap<>();
        int lostCount = 0;
        int timeoutCount = 0;

        private void add(GameResult result) {
            switch (result.getOutcome()) {
                case WON:
                    winCount.put(result.getTurns(), getWinCount(result.getTurns()) + 1);
                    break;
                case LOST:
                    lostCount++;
                    break;
                case TIMEOUT:
                    timeoutCount++;
                    break;
            }
        }

        /**
         * Returns the number of games won at the given turn
         *
         * @param turn turn number (1-based)
         * @return number of games won at the given turn
         */
        public int getWinCount(int turn) {
            return winCount.getOrDefault(turn, 0);
        }

        // TODO: comment on calcule ?
        public double getAverageWinTurn() {
            long sum = winCount.entrySet().stream().map(e -> e.getKey() * e.getValue()).collect(Collectors.summingLong(Integer::longValue));
            long wonGames = iterations - lostCount - timeoutCount;
            return (double) sum / (double) wonGames;
        }

        /**
         * Ecart type
         */
        public double getWinTurnStdDerivation() {
            double avg = getAverageWinTurn();
            double distanceSum = winCount.entrySet().stream().map(e -> e.getKey() * (e.getValue() - avg) * (e.getValue() - avg)).collect(Collectors.summingDouble(Double::doubleValue));
            long wonGames = iterations - lostCount - timeoutCount;
            return Math.sqrt(distanceSum / (double) wonGames);
        }
    }

    public List<DeckStats> simulate(Iterable<Deck> decksProvider) {
        return StreamSupport.stream(decksProvider.spliterator(), false)
                .map(deck -> simulate(deck))
                .collect(Collectors.toList());
    }

    public DeckStats simulate(Deck deck) {
        DeckStats stats = DeckStats.builder().deck(deck).iterations(iterations).build();
        boolean onThePlay = otp == OnThePlay.NO ? false : true;
        for (int it = 0; it < iterations; it++) {
            stats.add(simulateGame(deck, onThePlay));
            if (otp == OnThePlay.RANDOM) {
                // change every game
                onThePlay = !onThePlay;
            }
        }
        return stats;
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
                pilot.endingPhase();

                // check won
                String winReason = pilot.checkWin();
                if (winReason != null) {
                    return GameResult.builder()
                            .onThePlay(onThePlay)
                            .outcome(GameResult.Outcome.WON)
                            .turns(game.getCurrentTurn())
                            .reason(winReason)
                            .build();
                }

                // init next turn
                game.startNextTurn();
            }
            return GameResult.builder()
                    .onThePlay(onThePlay)
                    .outcome(GameResult.Outcome.TIMEOUT)
                    .turns(maxTurns + 1)
                    .build();
        } catch (GameLostException gle) {
            return GameResult.builder()
                    .onThePlay(onThePlay)
                    .outcome(GameResult.Outcome.LOST)
                    .turns(game.getCurrentTurn())
                    .reason(gle.getMessage())
                    .build();
        }
    }

    @Builder
    @Value
    private static class GameResult {
        private enum Outcome {WON, LOST, TIMEOUT}

        final boolean onThePlay;
        final Outcome outcome;
        final int turns;
        final String reason;
    }
}
