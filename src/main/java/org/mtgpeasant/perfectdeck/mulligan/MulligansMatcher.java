package org.mtgpeasant.perfectdeck.mulligan;

import lombok.Value;

import java.util.stream.Stream;

@Value
class MulligansMatcher extends Matcher {
    CmpOp operator;
    int number;

    @Override
    public String toString() {
        return "@mulligans(" + operator + ")(" + number + ")";
    }

    @Override
    protected void validate(Validation validation, MatcherContext context) {
    }

    @Override
    protected Stream<Match> matches(boolean onThePlay, int mulligans, Stream<Match> upStreamMatches, MatcherContext context) {
        if (operator.compare(mulligans, number)) {
            return upStreamMatches;
        } else {
            return Stream.empty();
        }
    }
}
