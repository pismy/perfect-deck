package org.mtgpeasant.stats.parser;

import lombok.Builder;
import lombok.Value;
import org.mtgpeasant.stats.domain.*;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

public class MatcherParser {
    static final String SEPARATORS = "[]<>()";
    static final String WHITE = " \t";
    static final String DIGITS = "0123456789";

    @Builder
    @Value
    public static class MatcherDeclaration {
        final String name;
        final boolean criteria;
        final Matcher matcher;

        @Override
        public String toString() {
            return (criteria ? "<<" : "<") + name + (criteria ? ">>" : ">") + ": " + matcher.toString();
        }
    }

    public static MatcherDeclaration parse(String line) throws ParseError {
        ParseHelper parser = new ParseHelper(new StringReader(line));
        parser.curChar();

        // 1: read name
        if(!parser.consumeChar('<', WHITE)) {
            parser.error(ParseError.RC_SYNTAX_ERROR, "'<' expected to open a matcher name declaration");
        }
        boolean isCriteria = parser.consumeChar('<', "");
        String name = parser.readUntil(SEPARATORS).trim();
        if(name.isEmpty()) {
            parser.error(ParseError.RC_SYNTAX_ERROR, "matcher name should not be empty");
        }
        if(!parser.consumeChar('>', WHITE)) {
            parser.error(ParseError.RC_SYNTAX_ERROR, "'>' expected to close a matcher name declaration");
        }
        if(isCriteria && !parser.consumeChar('>', WHITE)) {
            parser.error(ParseError.RC_SYNTAX_ERROR, "'>>' expected to close a criteria name declaration");
        }
        // 2: read ':'
        if(!parser.consumeChar(':', WHITE)) {
            parser.error(ParseError.RC_SYNTAX_ERROR, "':' expected after matcher name declaration");
        }
        // 3: read matchers
        parser.skipChars(WHITE);
        Matcher matcher = parseCompound(parser);

        return MatcherDeclaration.builder()
                .name(name)
                .criteria(isCriteria)
                .matcher(matcher)
                .build();
    }

    private static Matcher parseCompound(ParseHelper parser) throws ParseError {
        List<Matcher> matchers = new ArrayList<>();
        Character compound = null;
        while(parser.curChar() > 0 && parser.curChar() != ')') {
            if(matchers.size() == 1) {
                // read "&&" or "||"
                if(parser.consumeChar('&', WHITE)) {
                    // optional doubled
                    parser.consumeChar('&', "");
                    compound = '&';
                } else if(parser.consumeChar('|', WHITE)) {
                    // optional doubled
                    parser.consumeChar('|', "");
                    compound = '|';
                } else {
                    parser.error(ParseError.RC_SYNTAX_ERROR, "either '&' or '|' expected to assemble several matchers");
                }
            } else if(matchers.size() > 1) {
                if(!parser.consumeChar(compound, WHITE)) {
                    parser.error(ParseError.RC_SYNTAX_ERROR, "'" + compound + "' expected");
                }
                // optional doubled
                parser.consumeChar(compound, "");
            }

            matchers.add(parse(parser));
        }

        if(matchers.isEmpty()) {
            parser.error(ParseError.RC_SYNTAX_ERROR, "you should declare at least one matcher");
        }

        if(matchers.size() == 1) {
            return matchers.get(0);
        } else if(compound == '&') {
            return AndMatcher.builder().matchers(matchers).build();
        } else {
            return OrMatcher.builder().matchers(matchers).build();
        }
    }

    private static Matcher parse(ParseHelper parser) throws ParseError {
        // 1: read integer (times)
        String timesStr = "";
        while(DIGITS.indexOf(parser.curChar()) > 0) {
            timesStr+=(char)parser.curChar();
            parser.nextChar();
        }
        int times = 1;
        if(!timesStr.isEmpty()) {
            times = Integer.parseInt(timesStr);
        }
        // 2: read card matcher or ref matcher
        Matcher matcher = null;
        if(parser.consumeChar('[', WHITE)) {
            // card matcher
            matcher = CardMatcher.builder().card(parser.readUntil(SEPARATORS).trim().toLowerCase()).build();
            if(!parser.consumeChar(']', WHITE)) {
                parser.error(ParseError.RC_SYNTAX_ERROR, "']' expected to close a card matcher");
            }
        } else if(parser.consumeChar('<', WHITE)) {
            // ref matcher
            matcher = RefMatcher.builder().name(parser.readUntil(SEPARATORS).trim()).build();
            if(!parser.consumeChar('>', WHITE)) {
                parser.error(ParseError.RC_SYNTAX_ERROR, "'>' expected to close a matcher reference");
            }
        } else if(parser.consumeChar('(', WHITE)) {
            // compound matcher
            matcher = parseCompound(parser);
            if(!parser.consumeChar(')', WHITE)) {
                parser.error(ParseError.RC_SYNTAX_ERROR, "')' expected to close a compound matcher");
            }
        } else {
            parser.error(ParseError.RC_SYNTAX_ERROR, "either '[' or '<' expected to declare a matcher");
        }
        parser.skipChars(WHITE);

        if(times == 1) {
            return matcher;
        } else {
            return TimesMatcher.builder().times(times).matcher(matcher).build();
        }
    }
}
