package org.mtgpeasant.perfectdeck.goldfish;

import lombok.Builder;
import lombok.Value;
import org.mtgpeasant.perfectdeck.common.cards.Cards;
import org.mtgpeasant.perfectdeck.common.cards.Deck;
import org.mtgpeasant.perfectdeck.handoptimizer.MulliganRules;


@Builder
@Value
public class GoldfishSimulator {
    int draw = 7;
    final int iterations;
    final MulliganRules rules;

    public void simulate(int maxTurns, DeckPilot pilot, Deck... decks) {
        for (int d = 0; d < decks.length; d++) {
            Deck deck = decks[d];
            for (int it = 0; it < iterations; it++) {
                simulateGame(pilot, deck, maxTurns, true);
            }
        }
    }

    private GameResult simulateGame(DeckPilot pilot, Deck deck, int maxTurns, boolean onThePlay) {
        // 1: select hand
        int mulligans = -1;
        Cards library;
        Cards hand;
        do {
            mulligans++;
            library = deck.getMain().shuffle();
            hand = library.draw(draw);
        } while (!pilot.keep(hand, mulligans));

        Game game = new Game(library, hand);
        if (mulligans > 0) {
            pilot.getRidOfCards(mulligans, game);
        }

        // 2: simulate a game
        try {
            while (game.getCurrentTurn() < maxTurns) {
                // untap
                pilot.untap(game);

                // upkeep
                pilot.upkeep(game);

                // draw (unless first turn on the play)
                if(!onThePlay || game.getCurrentTurn() > 0) {
                    pilot.draw(game);
                }

                // first main phase
                game.emptyPool();
                pilot.firstMainPhase(game);

                // combat phase
                game.emptyPool();
                pilot.combatPhase(game);

                // second main phase
                game.emptyPool();
                pilot.secondMainPhase(game);

                // end phase
                pilot.endingPhase(game);

                // check won
                String winReason = pilot.checkWin(game);
                if (winReason != null) {
                    return GameResult.builder().outcome(Outcome.WON).turns(game.getCurrentTurn() + 1).reason(winReason).build();
                }

                // init next turn
                game.startNextTurn();
            }
            return GameResult.builder().outcome(Outcome.TIMEOUT).turns(maxTurns).build();
        } catch (GameLostException gle) {
            return GameResult.builder().outcome(Outcome.LOST).turns(game.getCurrentTurn() + 1).reason(gle.getMessage()).build();
        }
    }

    private enum Outcome {WON, LOST, TIMEOUT}

    @Builder
    @Value
    private static class GameResult {
        final Outcome outcome;
        final int turns;
        final String reason;
    }
}
