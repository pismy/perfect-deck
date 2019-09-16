package org.mtgpeasant.stats.domain;


import java.util.List;
import java.util.stream.Stream;

public abstract class Matcher {

    public abstract void validate(Validation validation, MatcherContext context);

    public abstract Stream<Match> matches(Stream<Match> stream, MatcherContext context);

    public abstract List<Match> matches(Match upstream, MatcherContext context);
}
