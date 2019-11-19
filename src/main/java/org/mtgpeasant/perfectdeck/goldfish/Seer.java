package org.mtgpeasant.perfectdeck.goldfish;

import com.google.common.base.Preconditions;
import lombok.Builder;
import lombok.Singular;
import lombok.Value;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class Seer {
    public interface SpellsPlayer {
        boolean canPlay(String card);

        boolean play(String card);
    }

    @Value
    @Builder
    public static class VictoryRoute {
        @Singular
        final List<String> firstMainPhaseSpells;
        @Singular
        final List<String> secondMainPhaseSpells;

        /**
         * Play the victory route spells
         *
         * @param pilot deck pilot
         */
        public void play(DeckPilot pilot) {
            if (pilot.game.getCurrentPhase() == Game.Phase.first_main) {
                firstMainPhaseSpells.forEach(card -> ((SpellsPlayer) pilot).play(card));
            } else {
                secondMainPhaseSpells.forEach(card -> ((SpellsPlayer) pilot).play(card));
            }
        }
    }

    /**
     * Brute force exploration of all spells sequence that allow winning this turn
     *
     * @param pilot game pilot
     * @param cards cards/spells allowing winning
     * @return optional list of ordered spells to play to win this turn
     */
    public static Optional<VictoryRoute> findRouteToVictory(DeckPilot pilot, String... cards) {
        Preconditions.checkArgument(pilot instanceof SpellsPlayer, "DeckPilot has to implement " + SpellsPlayer.class.getSimpleName());
        return Optional.ofNullable(findRouteToVictory(pilot.fork(), new ArrayList<>(), cards));
    }

    private static VictoryRoute findRouteToVictory(DeckPilot pilot, List<String> playedSpells, String... cards) {
        // filter playable cards
        List<String> playableCards = Arrays.stream(cards)
                .filter(card -> ((SpellsPlayer) pilot).canPlay(card))
                .collect(Collectors.toList());

        if (playableCards.isEmpty()) {
            // cannot play anymore card🕑 in current phase
            if (pilot.game.getCurrentPhase() == Game.Phase.first_main) {
                // end of first main phase

                // combat phase
                pilot.game.startPhase(Game.Phase.combat);
                pilot.combatPhase();

                // check if win conditions are reached after combat phase
                if (pilot.checkWin() != null) {
                    return VictoryRoute.builder().firstMainPhaseSpells(playedSpells).build();
                }

                // else simulate second main phase
                pilot.game.startPhase(Game.Phase.second_main);

                VictoryRoute secondMainPhaseRoute = findRouteToVictory(pilot, new ArrayList<>(), cards);
                if (secondMainPhaseRoute != null) {
                    return VictoryRoute.builder()
                            .firstMainPhaseSpells(playedSpells)
                            .secondMainPhaseSpells(secondMainPhaseRoute.secondMainPhaseSpells)
                            .build();
                } else {
                    return null;
                }
            } else {
                // end of second main phase

                // play ending phase
                pilot.game.startPhase(Game.Phase.ending);
                pilot.endingPhase();

                // then check if win conditions are reached
                return pilot.checkWin() != null ? VictoryRoute.builder().secondMainPhaseSpells(playedSpells).build() : null;
            }
        } else {
            // try each playable card then recurse
            boolean hasToBeForked = playableCards.size() > 1;
            for (String card : playableCards) {
                DeckPilot sub = hasToBeForked ? pilot.fork() : pilot;
                List<String> subPlayed = hasToBeForked ? new ArrayList<>(playedSpells) : playedSpells;
                subPlayed.add(card);
                ((SpellsPlayer) sub).play(card);
                VictoryRoute victoryRoute = findRouteToVictory(sub, subPlayed, cards);
                if (victoryRoute != null) {
                    return victoryRoute;
                }
            }
            return null;
        }
    }
}
