package org.mtgpeasant.stats.parser;

import lombok.Builder;
import lombok.Value;
import org.mtgpeasant.stats.domain.Cards;
import org.mtgpeasant.stats.domain.Deck;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DeckParser {
    static Pattern CARD_LINE = Pattern.compile("(SB[:\\s]\\s*)?(?:(\\d+)x?\\s+)?(?:\\[(.*)\\]\\s*)?(.+)");

    public static Deck parse(Reader input) throws IOException {
        BufferedReader reader = new BufferedReader(input);
        Cards.CardsBuilder side = Cards.builder();
        Cards.CardsBuilder main = Cards.builder();
        String line = null;
        while ((line = reader.readLine()) != null) {
            CardLine dl = parse(line);
            if(dl != null) {
                for (int i = 0; i < dl.getCount(); i++) {
                    (dl.isMain() ? main : side).card(dl.getName());
                }
            }
        }
        input.close();
        return Deck.builder()
                .main(main.build())
                .sideboard(side.build())
                .build();
    }

    public static CardLine parse(String line) {
        line = line.trim();
        if (line.isEmpty() || line.startsWith("#")) {
            // empty or commented line
            return null;
        } else {
            Matcher m = CARD_LINE.matcher(line);
            if (m.matches()) {
                return CardLine.builder()
                        .main(m.group(1) == null)
                        .count(m.group(2) == null ? 1 : Integer.parseInt(m.group(2)))
                        .extension(m.group(3))
                        .name(m.group(4).toLowerCase())
                        .build();
            } else {
                return null;
            }
        }
    }

    @Value
    @Builder
    public static class CardLine {
        final boolean main;
        final String extension;
        final int count;
        final String name;
    }
}
