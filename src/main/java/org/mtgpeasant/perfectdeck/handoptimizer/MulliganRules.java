package org.mtgpeasant.perfectdeck.handoptimizer;

import lombok.Value;
import org.mtgpeasant.perfectdeck.common.cards.Cards;
import org.mtgpeasant.perfectdeck.common.matchers.*;
import org.mtgpeasant.perfectdeck.common.utils.ParseError;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

@Value
public class MulliganRules implements MatcherContext {
    final Map<String, Matcher> matchers;
    final List<ParseError> errors;
    final List<MatcherParser.DeclaredMatcher> criteria;

    public static MulliganRules parse(Reader input) throws IOException {
        List<MatcherParser.DeclaredMatcher> criteria = new ArrayList<>();
        Map<String, Matcher> matchers = new HashMap<>();
        List<ParseError> errors = new ArrayList<>();

        BufferedReader reader = new BufferedReader(input);
        String line = null;
        int lineNb = 0;
        while ((line = reader.readLine()) != null) {
            lineNb++;
            line = line.trim();
            if (line.isEmpty() || line.startsWith("#") || line.startsWith("//")) {
                // zero or commented line
            } else {
                try {
                    MatcherParser.DeclaredMatcher decl = MatcherParser.parse(line);
                    matchers.put(decl.getName(), decl.getMatcher());
                    if (decl.isCriterion()) {
                        criteria.add(decl);
                    }
                } catch(ParseError pe) {
                    pe.setLine(lineNb);
                    errors.add(pe);
                }
            }
        }
        reader.close();

        return new MulliganRules(matchers, errors, criteria);
    }

    @Override
    public Matcher findByName(String name) {
        return matchers.get(name);
    }

    public Validation validate() {
        Validation validation = new Validation();
        for (Matcher matcher : matchers.values()) {
            matcher.validate(validation, this);
        }
        return validation;
    }

    public MatcherParser.DeclaredMatcher findMatch(Cards hand) {
        Match match = Match.from(hand);
        for (MatcherParser.DeclaredMatcher decl : getCriteria()) {
            if (decl.getMatcher().matches(Stream.of(match), this).findFirst().isPresent()) {
                return decl;
            }
        }
        return null;
    }
}
