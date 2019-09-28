package org.mtgpeasant.perfectdeck.goldfish;

import org.mtgpeasant.perfectdeck.common.cards.Cards;
import org.mtgpeasant.perfectdeck.handoptimizer.MulliganRules;

public class ReanimatorDeckPilot extends DeckPilot {
    private static String[] LANDS = new String[]{"swamp", "mountain", "crumbling vestige"};
    private static String[] FREEMANA = new String[]{"lotus petal", "simian spirit guide"};
    private static String[] REANIMATORS = new String[]{"exhume", "animate dead", "reanimate"};
    private static String[] MONSTERS = new String[]{"hand of emrakul", "greater sandwurm", "pathrazer of ulamog", "ulamog's crusher"};
    private static String[] DISCARD = new String[]{"faithless looting", "putrid imp"};

    private final MulliganRules rules;

    public ReanimatorDeckPilot(MulliganRules rules) {
        this.rules = rules;
    }

    @Override
    public boolean keep(Cards hand, int mulligans) {
        if (mulligans >= 2) {
            return true;
        }
        return rules.firstMatch(hand) != null;
    }

    @Override
    public void firstMainPhase(Game game) {
        while (play(game)) {

        }
    }
    
    private boolean play(Game game) {
        if(game.getGraveyard().count(MONSTERS) > 0) {
            // I have a monster in the graveyard: I must now reanimate
            Cards reanimators = game.getHand().select(REANIMATORS);
            if(!reanimators.isEmpty()) {

            }
        } else {
            // my first objective is now to put a monster in the GY
            Cards reanimators = game.getHand().select(REANIMATORS);
            Cards monsters = game.getHand().select(MONSTERS);
            Cards lands = game.getHand().select(LANDS);
            Cards freemana = game.getHand().select(FREEMANA);

            if(monsters.isEmpty()) {
                // no monster in hand: can I look for one ?
            } else if(game.getHand().has("putrid imp") && canPay(Mana.of("B"))) {

            } else if(game.getHand().has("faithless looting") && canPay(Mana.of("R"))) {

            } else

            // if I have in hand: a monster + a discard + the required mana, let's do it!


        }

        // count available mana
        if (rules.findByName("turn 1 imp").matches(game.getHand(), rules) != null) {

        }
        // first land
        Cards hand = game.getHand();
        if (LANDS.stream().filter(hand::has).findFirst().isPresent()) {

        }
    }

    @Override
    public String checkWin(Game game) {
        super.checkWin(game);
        // consider I won as soon as I have a monster on the board
        if(game.getBoard().count(MONSTERS) > 0) {
            return "I reanimated a monster";
        }
        return null;
    }
}
