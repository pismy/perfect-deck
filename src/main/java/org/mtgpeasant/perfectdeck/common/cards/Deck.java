package org.mtgpeasant.perfectdeck.common.cards;

import lombok.Builder;
import lombok.Singular;
import lombok.Value;
import lombok.experimental.NonFinal;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Builder
@Value
public class Deck {
    static Pattern CARD_LINE = Pattern.compile("\\s*(SB[:\\s]\\s*)?(?:(\\d+)x?\\s+)?(?:\\[(.*)\\]\\s*)?(.+)");
    final Cards main;
    final Cards sideboard;

    @Value
    @Builder
    public static class ParsedDeck {
        @Singular
        final List<Line> lines;

        @Value
        @NonFinal
        public static abstract class Line {
            final String line;
        }

        @Value
        public static class SideboardSectionLine extends Line {
            public SideboardSectionLine(String line) {
                super(line);
            }
        }

        @Value
        public static class UnknLine extends Line {
            public UnknLine(String line) {
                super(line);
            }
        }

        @Value
        public static class CommentLine extends Line {
            public CommentLine(String line) {
                super(line);
            }
        }

        @Value
        public static class CardLine extends Line {
            final int count;
            final String extension;
            final String card;
            final int cardIndex;
            final boolean isMain;
            final boolean isKnown;

            public CardLine(String line, int count, String extension, String card, int cardIndex, boolean isMain, boolean isKnown) {
                super(line);
                this.count = count;
                this.card = card;
                this.extension = extension;
                this.cardIndex = cardIndex;
                this.isMain = isMain;
                this.isKnown = isKnown;
            }
        }
    }

    public static List<ParsedDeck.Line> parseLines(Reader input, Cards managedCards) throws IOException {
        List<ParsedDeck.Line> lines = new ArrayList<>();
        BufferedReader reader = new BufferedReader(input);
        String line = null;
        boolean[] isReadingSideboard = new boolean[]{false};
        while ((line = reader.readLine()) != null) {
            lines.add(parseLine(line, isReadingSideboard, managedCards));
        }
        input.close();
        return lines;
    }

    static ParsedDeck.Line parseLine(final String line, boolean[] isReadingSideboard, Cards managedCards) {
        String trimmedLine = line.trim();
        if (trimmedLine.isEmpty() || trimmedLine.startsWith("#") || trimmedLine.startsWith("//")) {
            // zero or commented line
            return new ParsedDeck.CommentLine(line);
        } else if (trimmedLine.toLowerCase().startsWith("sideboard")) {
            isReadingSideboard[0] = true;
            return new ParsedDeck.SideboardSectionLine(line);
        } else {
            Matcher m = CARD_LINE.matcher(trimmedLine);
            if (m.matches()) {
                String card = m.group(4);
                return new ParsedDeck.CardLine(
                        line,
                        m.group(2) == null ? 1 : Integer.parseInt(m.group(2)),
                        m.group(3),
                        card,
                        m.start(4),
                        isReadingSideboard[0] ? false : m.group(1) == null,
                        managedCards == null ? true : managedCards.contains(card.toLowerCase())
                );
            } else {
                return new ParsedDeck.UnknLine(line);
            }
        }
    }

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
