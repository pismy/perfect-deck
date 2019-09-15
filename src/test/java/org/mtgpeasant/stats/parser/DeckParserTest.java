package org.mtgpeasant.stats.parser;

import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.mtgpeasant.stats.domain.Cards;

import java.io.IOException;
import java.io.StringReader;


public class DeckParserTest {
    @Test
    public void should_parse() throws IOException {
        Cards cards = DeckParser.parse(new StringReader("6x swamp\n3 mountains  \n2   Ulamog Crusher\ndark ritual"));
        Assertions.assertThat(cards.size()).isEqualTo(12);
        Assertions.assertThat(cards.getCards()).containsExactly(
                "swamp",
                "swamp",
                "swamp",
                "swamp",
                "swamp",
                "swamp",
                "mountains",
                "mountains",
                "mountains",
                "ulamog crusher",
                "ulamog crusher",
                "dark ritual"
        );
    }
}