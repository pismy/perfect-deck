package org.mtgpeasant.stats.parser;

import org.mtgpeasant.stats.domain.Cards;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DeckParser {
    static Pattern CARD_WITH_NUMBER = Pattern.compile("(\\d+)x?\\s+(.*)");

    public static Cards parse(Reader input) throws IOException {
        BufferedReader reader = new BufferedReader(input);
        Cards.CardsBuilder builder = Cards.builder();
        String line = null;
        while ((line = reader.readLine()) != null) {
            line = line.trim().toLowerCase();
            if(line.isEmpty() || line.startsWith("#")) {
                // empty or commented line
            } else {
                Matcher m = CARD_WITH_NUMBER.matcher(line);
                if (m.matches()) {
                    int nb = Integer.parseInt(m.group(1));
                    for (int i = 0; i < nb; i++) {
                        builder.card(m.group(2));
                    }
                } else {
                    // one card
                    builder.card(line);
                }
            }
        }
        input.close();
        return builder.build();
    }
}
