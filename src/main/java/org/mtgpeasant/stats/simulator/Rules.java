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
    final List<MatcherParser.MatcherDeclaration> criterias;

    public static Rules parse(Reader input) throws IOException, ParseError {
        List<MatcherParser.MatcherDeclaration> criterias = new ArrayList<>();
        Map<String, Matcher> matchers = new HashMap<>();

        BufferedReader reader = new BufferedReader(input);
        String line = null;
        while ((line = reader.readLine()) != null) {
            line = line.trim();
            if (line.isEmpty() || line.startsWith("#")) {
                // empty or commented line
            } else {
                MatcherParser.MatcherDeclaration decl = MatcherParser.parse(line);
                matchers.put(decl.getName(), decl.getMatcher());
                if (decl.isCriteria()) {
                    criterias.add(decl);
                }
            }
        }
        reader.close();

        return new Rules(matchers, criterias);
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
