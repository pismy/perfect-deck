package org.mtgpeasant.perfectdeck.mulligan;

import lombok.Value;
import org.mtgpeasant.perfectdeck.common.cards.Cards;
import org.mtgpeasant.perfectdeck.common.utils.ParseError;
import org.mtgpeasant.perfectdeck.goldfish.DeckPilot;

import java.io.*;
import java.util.*;

@Value
public class MulliganRules implements MatcherContext {
    final Map<String, Matcher> matchers;
    final List<ParseError> errors;
    final List<Matchers.NamedMatcher> criteria;

    /**
     * Loads the default mulligans rules of a deck pilot
     */
    public static MulliganRules load(Class<? extends DeckPilot> pilotClass) {
        InputStream stream = pilotClass.getResourceAsStream(pilotClass.getSimpleName() + "-mull.txt");
        if (stream == null) {
            throw new RuntimeException("Mulligan rules file for " + pilotClass + " not found (" + pilotClass.getSimpleName() + "-mull.txt" + ")!");
        }
        try {
            return parse(new InputStreamReader(stream));
        } catch (IOException e) {
            throw new RuntimeException("Error occurred while parsing Mulligan rules file for " + pilotClass, e);
        }
    }

    // TODO: cache
    public static MulliganRules parse(Reader input) throws IOException {
        List<Matchers.NamedMatcher> criteria = new ArrayList<>();
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
                    Matchers.NamedMatcher decl = Matchers.parse(line);
                    matchers.put(decl.getName(), decl.getMatcher());
                    if (decl.isCriterion()) {
                        criteria.add(decl);
                    }
                } catch (ParseError pe) {
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

    /**
     * Tries sequentially all mulligans rules and looks for one that matches
     *
     * @param hand drawn cards
     * @return first matching rule
     */
    public Optional<Matchers.NamedMatcher> firstMatch(boolean onThePlay, int mulligans, Cards hand) {
        return getCriteria().stream()
                .filter(c -> c.getMatcher().matches(onThePlay, mulligans, hand, this).findFirst().isPresent())
                .findFirst();
    }
}
