package org.mtgpeasant.stats.domain;


import java.util.List;

public abstract class Matcher {

    public abstract void validate(Validation validation, MatcherContext context);

    public abstract List<Match> matches(Match upstream, MatcherContext context);
}
