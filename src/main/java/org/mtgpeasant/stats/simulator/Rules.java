package org.mtgpeasant.stats.simulator;

import lombok.Value;
import org.mtgpeasant.stats.matchers.Matcher;
import org.mtgpeasant.stats.matchers.MatcherContext;
import org.mtgpeasant.stats.matchers.MatcherParser;
import org.mtgpeasant.stats.matchers.Validation;
import org.mtgpeasant.stats.utils.ParseError;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Value
public class Rules implements MatcherContext {
    final Map<String, Matcher> matchers;
    final List<ParseError> errors;
    final List<MatcherParser.DeclaredMatcher> criteria;

    public static Rules parse(Reader input) throws IOException {
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
                // empty or commented line
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

        return new Rules(matchers, errors, criteria);
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
}
