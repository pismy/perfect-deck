package org.mtgpeasant.stats.parser;

import org.mtgpeasant.stats.domain.Cards;
import org.mtgpeasant.stats.domain.Deck;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DeckParser {
    static Pattern CARD_WITH_NUMBER = Pattern.compile("(\\d+)x?\\s+(.*)");

    public static Deck parse(Reader input) throws IOException {
        BufferedReader reader = new BufferedReader(input);
        Cards.CardsBuilder side = Cards.builder();
        Cards.CardsBuilder main = Cards.builder();
        String line = null;
        while ((line = reader.readLine()) != null) {
            line = line.trim().toLowerCase();
            if (line.isEmpty() || line.startsWith("#")) {
                // empty or commented line
            } else {
                Cards.CardsBuilder cards = null;
                if (line.startsWith("sb ")) {
                    cards = side;
                    line = line.substring(3).trim();
                } else {
                    cards = main;
                }
                Matcher m = CARD_WITH_NUMBER.matcher(line);
                if (m.matches()) {
                    int nb = Integer.parseInt(m.group(1));
                    for (int i = 0; i < nb; i++) {
                        cards.card(m.group(2));
                    }
                } else {
                    // one card
                    cards.card(line);
                }
            }
        }
        input.close();
        return Deck.builder()
                .main(main.build())
                .sideboard(side.build())
                .build();
    }
}
