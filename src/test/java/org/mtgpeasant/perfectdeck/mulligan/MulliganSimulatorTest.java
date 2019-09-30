package org.mtgpeasant.perfectdeck.mulligan;

import dnl.utils.text.table.TextTable;
import org.junit.Test;
import org.mtgpeasant.perfectdeck.common.cards.Deck;
import org.mtgpeasant.perfectdeck.common.matchers.Matchers;
import org.mtgpeasant.perfectdeck.common.matchers.MulliganRules;
import org.mtgpeasant.perfectdeck.common.matchers.Validation;
import org.mtgpeasant.perfectdeck.common.utils.ParseError;

import java.io.IOException;
import java.io.InputStreamReader;
import java.text.DecimalFormat;

public class MulliganSimulatorTest {
    private static final DecimalFormat PERCENT = new DecimalFormat("#.##");

    @Test
    public void reanimator_deck1_mulligans() throws IOException {
        Deck deck = Deck.parse(new InputStreamReader(getClass().getResourceAsStream("/reanimator-deck.txt")));
        MulliganRules rules = MulliganRules.parse(new InputStreamReader(getClass().getResourceAsStream("/reanimator-rules.txt")));
        simulate(deck, rules, 50000);
    }

    @Test
    public void reanimator_deck2_mulligans() throws IOException {
        Deck deck = Deck.parse(new InputStreamReader(getClass().getResourceAsStream("/reanimator-deck2.txt")));
        MulliganRules rules = MulliganRules.parse(new InputStreamReader(getClass().getResourceAsStream("/reanimator-rules.txt")));
        simulate(deck, rules, 50000);
    }

    public static void simulate(Deck deck, MulliganRules rules, int iterations) throws IOException {
        System.out.println("Deck loaded: " + deck.getMain().size() + " cards (" + deck.getSideboard().size() + " cards in sideboard)");
        System.out.println();

        if (!rules.getErrors().isEmpty()) {
            System.out.println("=== ERRORS ===");
            for (ParseError error : rules.getErrors()) {
                System.out.println(error.getMessage());
            }
            return;
        }

        // validation
        Validation validation = rules.validate();
        if (!validation.getErrors().isEmpty()) {
            System.out.println("=== ERRORS ===");
            for (String msg : validation.getErrors()) {
                System.out.println("-> " + msg);
            }
            return;
        }

        // simulate draws
        long startTime = System.currentTimeMillis();
        MulliganSimulator simulator = MulliganSimulator.builder().iterations(iterations).rules(rules).build();
        MulliganSimulator.DeckMatches deckMatches = simulator.simulate(deck);

        // dump perfectdeck
        System.out.println("=== STATS (elapsed " + (System.currentTimeMillis() - startTime) + "ms) ===");
        String[] columnNames = new String[]{"Criteria", "Deck #1"};
        Object[][] data = new Object[rules.getCriteria().size() + 1][2];
        for (int i = 0; i < rules.getCriteria().size(); i++) {
            Matchers.NamedMatcher criteria = rules.getCriteria().get(i);
            data[i][0] = criteria.getName();
            int count = deckMatches.getMatchCount(criteria);
            data[i][1] = count + "/" + iterations + " (" + PERCENT.format(100f * (float) count / (float) iterations) + "%)";
        }
        data[rules.getCriteria().size()][0] = "no match";
        int count = deckMatches.getNoMatchCount();
        data[rules.getCriteria().size()][1] = count + "/" + iterations + " (" + PERCENT.format(100f * (float) count / (float) iterations) + "%)";
        new TextTable(columnNames, data).printTable();
    }
}
