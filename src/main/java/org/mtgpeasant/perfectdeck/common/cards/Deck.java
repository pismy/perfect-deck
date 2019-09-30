package org.mtgpeasant.perfectdeck.common.cards;

import lombok.Builder;
import lombok.Value;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Builder
@Value
public class Deck {
    static Pattern CARD_LINE = Pattern.compile("(SB[:\\s]\\s*)?(?:(\\d+)x?\\s+)?(?:\\[(.*)\\]\\s*)?(.+)");
    final Cards main;
    final Cards sideboard;

    public static Deck parse(Reader input) throws IOException {
        BufferedReader reader = new BufferedReader(input);
        Cards side = new Cards();
        Cards main = new Cards();
        String line = null;
        boolean[] isReadingSideboard = new boolean[]{false};
        while ((line = reader.readLine()) != null) {
            CardLine dl = parse(line, isReadingSideboard);
            if (dl != null) {
                for (int i = 0; i < dl.getCount(); i++) {
                    (dl.isMain() ? main : side).add(dl.getName());
                }
            }
        }
        input.close();
        return builder()
                .main(main)
                .sideboard(side)
                .build();
    }

    static CardLine parse(String line) {
        return parse(line, new boolean[]{false});
    }

    static CardLine parse(String line, boolean[] isReadingSideboard) {
        line = line.trim();
        if (line.isEmpty() || line.startsWith("#") || line.startsWith("//")) {
            // zero or commented line
            return null;
        } else if (line.toLowerCase().startsWith("sideboard")) {
            isReadingSideboard[0] = true;
            return null;
        } else {
            Matcher m = CARD_LINE.matcher(line);
            if (m.matches()) {
                return CardLine.builder()
                        .main(isReadingSideboard[0] ? false : m.group(1) == null)
                        .count(m.group(2) == null ? 1 : Integer.parseInt(m.group(2)))
                        .extension(m.group(3))
                        .name(m.group(4).toLowerCase())
                        .build();
            } else {
                return null;
            }
        }
    }

    public static Iterable<Deck> decks(Deck... decks) {
        return Arrays.asList(decks);
    }

    @Value
    @Builder
    protected static class CardLine {
        final boolean main;
        final String extension;
        final int count;
        final String name;
    }
}
