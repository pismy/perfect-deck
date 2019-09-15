package org.mtgpeasant.stats.domain;


import java.util.List;

public abstract class Matcher {

//    final String name;

    public abstract List<Match> matches(Match upstream, MatcherContext context);
}
