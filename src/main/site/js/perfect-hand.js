class Util {
    static getCombinations(array, size, start, initialStuff, output) {
        if (initialStuff.length >= size) {
            output.push(initialStuff);
        } else {
            let i;
            for (i = start; i < array.length; ++i) {
                Util.getCombinations(array, size, i + 1, initialStuff.concat(array[i]), output);
            }
        }
        return output;
    }

    static getAllPossibleCombinations(array, size) {
        return Util.getCombinations(array, size, 0, [], []);
    }
}

class ParseError {
    /**
     *
     * @param {String} message
     * @param {Number} line
     * @param {Number} column
     * @param {String} curLine
     */
    constructor(message, line, column, curLine) {
        this.message = message;
        this.line = line;
        this.column = column;
        this.curLine = curLine;
    }

    toString() {
        return this.message + " at line " + this.line + ":\n"
            + this.curLine + "\n"
            + "-".repeat(this.column - 1) + '^';
    }
}

class Parser {
    /**
     * Creates a multi-purpose parser
     * @param {string} input text to parse (array of chars)
     * @constructor
     */
    constructor(input) {
        this.input = input;
        // --- read variables
        this.curIdx = 0;
        this.curLine = [];
        this.lineNb = 0;
        this.colNb = -1;
    }

    /**
     * @return {string}
     */
    get curChar() {
        if (this.curIdx >= this.input.length) {
            return null;
        } else {
            return this.input[this.curIdx];
        }
    }

    /**
     * @return {boolean}
     */
    get eof() {
        return this.curIdx >= this.input.length;
    }

    /**
     * @return {string}
     */
    nextChar() {
        // --- change line if current char is LF
        if (this.curChar === '\n') {
            this.lineNb++;
            this.colNb = -1;
            // reset current line buffer
            this.curLine.length = 0;
        }

        // --- advance
        this.curIdx++;

        // ---
        if (!this.eof && this.curChar !== '\n' && this.curChar !== '\r') {
            this.curLine.push(this.curChar);
            this.colNb++;
        }
        return this.curChar;
    }

    /**
     * Reads and consumes the given char.
     *
     * @param {string} c
     *            the character to read
     * @param {String} skip
     *            chars allowed to skip
     * @return boolean true if the given char was read
     * @throws ParseError
     */
    consumeChar(c, skip) {
        if (skip != null)
            this.skipChars(skip);
        if (this.curChar !== c)
            return false;

        // --- expected char has been read
        this.nextChar();
        return true;
    }

    /**
     * Skips chars belonging to the given string.
     *
     * @param {String} chars
     *            characters to skip.
     * @throws ParseError
     */
    skipChars(chars) {
        while (!this.eof && chars.indexOf(this.curChar) >= 0)
            this.nextChar();
    }

    /**
     * Reads until EOF or a character from the given string is encountered.
     *
     * @param {String} chars
     *            stop characters.
     * @return {String} characters read
     * @throws ParseError
     */
    readUntil(chars) {
        let buffer = [];
        while (!this.eof && chars.indexOf(this.curChar) < 0) {
            buffer.push(this.curChar);
            this.nextChar();
        }
        return buffer.join("");
    }

    /**
     * Reads until EOF or a character different from the given string is
     * encountered.
     *
     * @param {String} chars
     *            accepted characters.
     * @return {String} characters read
     * @throws ParseError
     */
    readWhile(chars) {
        let buffer = [];
        while (!this.eof && chars.indexOf(this.curChar) >= 0) {
            buffer.push(this.curChar);
            this.nextChar();
        }
        return buffer.join("");
    }

    /**
     * Throws an error
     * @param {String} message
     */
    error(message) {
        let l = this.lineNb;
        let c = this.colNb;
        this.readUntil("\n\r");
        throw new ParseError(message, l, c, this.curLine.join(""));
    }
}

class Cards {
    /**
     * @param {Array} cards card names
     */
    constructor(cards) {
        this.cards = cards;
    }

    size() {
        return this.cards.length;
    }

    isEmpty() {
        return this.cards.length === 0;
    }

    shuffle() {
        let shuffled = Array.from(this.cards);
        let j, x, i;
        for (i = shuffled.length - 1; i > 0; i--) {
            j = Math.floor(Math.random() * (i + 1));
            x = shuffled[i];
            shuffled[i] = shuffled[j];
            shuffled[j] = x;
        }
        return new Cards(shuffled);
    }

    /**
     * Draws cards.
     * @param {Number} number number of cards to draw.
     */
    draw(number) {
        let drawn = [];
        for (let i = 0; i < number && !this.isEmpty(); i++) {
            drawn.push(this.cards.pop());
        }
        return new Cards(drawn);
    }

    /**
     * Checks whether has given card.
     * @param {String} card card name.
     * @returns {boolean}
     */
    has(card) {
        return this.cards.indexOf(card) >= 0;
    }

    /**
     * Removes the given card.
     * @param {String} card card name.
     * @return {Cards}
     */
    remove(card) {
        let copy = Array.from(this.cards);
        copy.splice(copy.indexOf(card), 1);
        return new Cards(copy);
    }

    /**
     * Adds the given card.
     * @param {String} card card name.
     * @return {Cards}
     */
    add(card) {
        let copy = Array.from(this.cards);
        copy.push(card);
        return new Cards(copy);
    }
}

class Deck {
    /**
     * Constructor
     * @param {Cards} main
     * @param {Cards} side
     */
    constructor(main, side) {
        this.main = main;
        this.side = side;
    }
}

const CARD_LINE = /(SB[:\s]\s*)?(?:(\d+)x?\s+)?(?:\[(.*)\]\s*)?(.+)/;

class DeckParser {
    /**
     * Parses a deck
     * @param {String} text deck text
     * @return {Deck}
     */
    static parse(text) {
        let main = [];
        let side = [];
        text.split("\n").forEach((line) => {
            let card = DeckParser.parseCard(line);
            if (card) {
                for (let i = 0; i < card.count; i++) {
                    if (card.main) {
                        main.push(card.name)
                    } else {
                        side.push(card.name)
                    }
                }
            }
        });
        return new Deck(new Cards(main), new Cards(side));
    }

    /**
     * Parses a card line
     * @param {String} line line
     * @returns {*}
     */
    static parseCard(line) {
        line = line.trim();
        if (line.length === 0 || line.startsWith("#")) {
            // empty or commented line
            return null;
        } else {
            let m = CARD_LINE.exec(line);
            return {
                main: m[1] == null,
                count: m[2] == null ? 0 : parseInt(m[2], 10),
                extension: m[3],
                name: m[4].toLowerCase()
            }
        }
    }
}

class Match {
    /**
     * Constructor
     * @param {Cards} remaining
     * @param {Cards} selected
     */
    constructor(remaining, selected) {
        this.remaining = remaining;
        this.selected = selected;
    }

    /**
     * Determines whether has given card
     * @param {string} card name
     * @returns {boolean}
     */
    has(card) {
        return this.remaining.has(card);
    }

    /**
     * Selectes the given card
     * @param {string} card name
     * @returns {Match}
     */
    select(card) {
        return new Match(this.remaining.remove(card), this.selected.add(card));
    }
}

class Matcher {
    /**
     * Constructor
     * @param {Validation} validation
     * @param context
     */
    validate(validation, context) {
    }

    /**
     * Computes all sub matches
     * @param {Array.<Match>} stream
     * @param {MatcherContext} context
     * @return {Array.<Match>} sub stream
     */
    matches(stream, context) {
        throw new Error('Not implemented');
    }
}

class Validation {
    constructor() {
        this.errors = [];
        this.warnings = [];
    }
}

class MatcherContext {

    /**
     * Returns the declared matcher with given name
     * @param {string} name
     * @return {Matcher}
     */
    matcher(name) {
    }

    /**
     * Returns the card with given name
     * @param {string} name
     * @return {string} card
     */
    card(name) {
    }
}

class NoopMatcher extends Matcher {

    toString() {
        return "$noop";
    }

    matches(stream, context) {
        return stream;
    }
}

class CardMatcher extends Matcher {
    /**
     * Constructor
     * @param {string} card
     */
    constructor(card) {
        super();
        this.card = card;
    }

    toString() {
        return "[" + this.card + "]";
    }

    validate(validation, context) {
        if (!context.card(this.card)) {
            validation.warnings.push("Card [" + this.card + "] not found in deck");
        }
    }

    matches(stream, context) {
        return stream.filter(match => match.has(this.card)).map(match => match.select(this.card));
    }
}

class AndMatcher extends Matcher {
    /**
     * Constructor
     * @param {Array.<Matcher>} matchers
     */
    constructor(matchers) {
        super();
        this.matchers = matchers;
    }

    toString() {
        let sb = [];
        sb.push("(");
        for (let i = 0; i < this.matchers.length; i++) {
            if (i > 0) {
                sb.push(" && ");
            }
            sb.push(this.matchers[i]);
        }
        sb.push(")");
        return sb.join("");
    }

    validate(validation, context) {
        this.matchers.forEach((matcher) => {
            matcher.validate(validation, context);
        });
    }

    matches(stream, context) {
        for (let i = 0; i < this.matchers.length; i++) {
            let matcher = this.matchers[i];
            stream = matcher.matches(stream, context);
        }
        return stream;
    }
}

class OrMatcher extends Matcher {
    /**
     * Constructor
     * @param {Array.<Matcher>} matchers
     */
    constructor(matchers) {
        super();
        this.matchers = matchers;
    }

    toString() {
        let sb = [];
        sb.push("(");
        for (let i = 0; i < this.matchers.length; i++) {
            if (i > 0) {
                sb.push(" || ");
            }
            sb.push(this.matchers[i]);
        }
        sb.push(")");
        return sb.join("");
    }

    validate(validation, context) {
        this.matchers.forEach((matcher) => {
            matcher.validate(validation, context);
        });
    }

    matches(stream, context) {
        return this.matchers.flatMap(matcher => matcher.matches(stream, context));
    }
}

class TimesMatcher extends Matcher {
    /**
     *
     * @param {Number} times
     * @param {Matcher} matcher
     */
    constructor(times, matcher) {
        super();
        this.times = times;
        this.matcher = matcher;
    }

    toString() {
        return this.times + " " + this.matcher;
    }

    validate(validation, context) {
        this.matcher.validate(validation, context);
    }

    matches(stream, context) {
        for (let i = 0; i < this.times; i++) {
            stream = this.matcher.matches(stream, context);
        }
        return stream;
    }
}

class RefMatcher extends Matcher {
    /**
     * Constructor
     * @param {string} name
     */
    constructor(name) {
        super();
        this.name = name;
    }

    toString() {
        return "<" + this.name + ">";
    }

    validate(validation, context) {
        if (context.matcher(this.name) == null) {
            validation.errors.push("Matcher <" + this.name + "> not found");
        }
    }

    matches(stream, context) {
        return context.matcher(this.name).matches(stream, context);
    }
}

class Rules extends MatcherContext {
    constructor() {
        super();
        this.criterias = [];
        this.matchers = {};
        this.validation = new Validation();
    }

    /**
     *
     * @param {DeclaredMatcher} declaredMatcher
     */
    add(declaredMatcher) {
        this.matchers[declaredMatcher.name] = declaredMatcher.matcher;
        if (declaredMatcher.criteria) {
            this.criterias.push(declaredMatcher);
        }
    }

    matcher(name) {
        return this.matchers[name];
    }

    card(name) {
        // TODO
        return name;
    }

    validate() {
        for (let matchersKey in this.matchers) {
            this.matchers[matchersKey].validate(this.validation, this);
        }
    }

    /**
     * Looks for the first declared matcher that matches the given cards
     * @param {Cards} hand
     * @return {DeclaredMatcher}
     */
    matches(hand) {
        let match = new Match(hand, new Cards([]));
        let matching = this.criterias.find(declMatcher => declMatcher.matcher.matches([match], this).length > 0);
        if (matching) {
            console.log("hand", hand, " matches ", matching)
        } else {
            console.log("hand", hand, " rejected")
        }
        return matching;
    }

}

class DeclaredMatcher {
    /**
     * Constructor
     * @param {string} name
     * @param {boolean} criteria
     * @param {Matcher} matcher
     */
    constructor(name, criteria, matcher) {
        this.name = name;
        this.criteria = criteria;
        this.matcher = matcher;
    }

    toString() {
        return (this.criteria ? "<<" : "<") + this.name + (this.criteria ? ">>" : ">") + ": " + this.matcher;
    }
}

const SEPARATORS = "[]<>()";
const WHITE = " \t";
const DIGITS = "0123456789";

class RulesParser {
    /**
     * Parses a set of rules
     * @param {string} text
     * @return {Rules}
     */
    static parse(text) {
        let rules = new Rules();
        text.split("\n").forEach((line, lineNb) => {
            if (line.length === 0 || line.startsWith("#")) {
                // empty or commented line
            } else {
                try {
                    rules.add(RulesParser.parseRuleDeclaration(line));
                } catch (pe) {
                    pe.line = lineNb;
                    rules.validation.errors.push(pe);
                }
            }
        });
        // validate
        rules.validate();
        return rules;
    }

    /**
     *
     * @param {String} line
     * @return {DeclaredMatcher}
     */
    static parseRuleDeclaration(line) {
        let parser = new Parser(line);

        // 1: read name
        if (!parser.consumeChar('<', WHITE)) {
            parser.error("'<' expected to open a matcher name declaration");
        }
        let isCriteria = parser.consumeChar('<', "");
        let name = parser.readUntil(SEPARATORS).trim();
        if (name.length === 0) {
            parser.error("matcher name should not be empty");
        }
        if (!parser.consumeChar('>', WHITE)) {
            parser.error("'>' expected to close a matcher name declaration");
        }
        if (isCriteria && !parser.consumeChar('>', WHITE)) {
            parser.error("'>>' expected to close a criteria name declaration");
        }
        // 2: read ':'
        if (!parser.consumeChar(':', WHITE)) {
            parser.error("':' expected after matcher name declaration");
        }
        // 3: read either function or matchers
        parser.skipChars(WHITE);
        let matcher = null;
        if (parser.consumeChar('@', WHITE)) {
            matcher = RulesParser.parseFn(parser);
        } else {
            matcher = RulesParser.parseCompound(parser);
        }

        return new DeclaredMatcher(name, isCriteria, matcher);
    }

    /**
     *
     * @param {Parser} parser
     * @return {Matcher}
     */
    static parseMatcher(parser) {
        // 1: read integer (times)
        let timesStr = "";
        while (DIGITS.indexOf(parser.curChar) > 0) {
            timesStr += parser.curChar;
            parser.nextChar();
        }
        let times = 1;
        if (timesStr.length > 0) {
            times = parseInt(timesStr, 10);
        }
        // 2: read card matcher or ref matcher
        let matcher = null;
        if (parser.consumeChar('[', WHITE)) {
            // card matcher
            matcher = new CardMatcher(parser.readUntil(SEPARATORS).trim().toLowerCase());
            if (!parser.consumeChar(']', WHITE)) {
                parser.error("']' expected to close a card matcher");
            }
        } else if (parser.consumeChar('<', WHITE)) {
            // ref matcher
            matcher = new RefMatcher(parser.readUntil(SEPARATORS).trim());
            if (!parser.consumeChar('>', WHITE)) {
                parser.error("'>' expected to close a matcher reference");
            }
        } else if (parser.consumeChar('(', WHITE)) {
            // compound matcher
            matcher = RulesParser.parseCompound(parser);
            if (!parser.consumeChar(')', WHITE)) {
                parser.error("')' expected to close a compound matcher");
            }
        } else if (parser.consumeChar('@', WHITE)) {
            // function matcher
            matcher = RulesParser.parseFn(parser);
        } else {
            parser.error("either '[' or '<' expected to declare a matcher");
        }
        parser.skipChars(WHITE);

        if (times === 1) {
            return matcher;
        } else {
            return new TimesMatcher(times, matcher);
        }
    }

    /**
     *
     * @param {Parser} parser
     * @return {Matcher}
     */
    static parseCompound(parser) {
        let matchers = [];
        let compound = null;
        while (!parser.eof && parser.curChar !== ')') {
            if (matchers.length === 1) {
                // read "&&" or "||"
                if (parser.consumeChar('&', WHITE)) {
                    // optional doubled
                    parser.consumeChar('&', "");
                    compound = '&';
                } else if (parser.consumeChar('|', WHITE)) {
                    // optional doubled
                    parser.consumeChar('|', "");
                    compound = '|';
                } else {
                    parser.error("either '&' or '|' expected to assemble several matchers");
                }
            } else if (matchers.length > 1) {
                if (!parser.consumeChar(compound, WHITE)) {
                    parser.error("'" + compound + "' expected");
                }
                // optional doubled
                parser.consumeChar(compound, "");
            }

            matchers.push(RulesParser.parseMatcher(parser));
        }

        if (matchers.length === 0) {
            parser.error("you should declare at least one matcher");
        }

        if (matchers.length === 1) {
            return matchers[0];
        } else if (compound === '&') {
            return new AndMatcher(matchers);
        } else {
            return new OrMatcher(matchers);
        }
    }

    /**
     *
     * @param {Parser} parser
     * @return {Matcher}
     */
    static parseFn(parser) {
        let name = parser.readUntil(SEPARATORS).trim();
        if (name.length === 0) {
            parser.error("function name may not be empty");
        }
        switch (name) {
            case "any":
            case "all": {
                let matchers = RulesParser.parseMatcherList(parser);
                if (matchers.length === 0) {
                    parser.error("you shall provide at least one matcher");
                }
                if (matchers.length === 1) {
                    return matchers[0];
                }
                return name === "any" ? new OrMatcher(matchers) : new AndMatcher(matchers);
            }
            case "xof":
            case "atleast": {
                let arg1 = RulesParser.parseFnArg(parser);
                if (arg1.length === 0) {
                    parser.error("1st arg must be a valid integer");
                }
                let nb = 0;
                try {
                    nb = parseInt(arg1, 10);
                } catch (nfe) {
                    parser.error("1st arg must be a valid integer");
                }
                if (nb < 0) {
                    parser.error("1st arg must be a positive integer");
                }
                let matchers = RulesParser.parseMatcherList(parser);
                if (nb === 0) {
                    return new NoopMatcher();
                }
                if (nb > matchers.length) {
                    parser.error("1st arg cannot exceed matchers size");
                }
                if (matchers.length === 0) {
                    parser.error("you shall provide at least one matcher");
                }
                if (matchers.length === 1) {
                    return matchers[0];
                }
                if (nb === 1) {
                    return new OrMatcher(matchers);
                } else if (nb === matchers.length) {
                    new AndMatcher(matchers);
                } else {
                    // make all combinations of NB among matchers:
                    // any( all(M1, M2, ... Mn), ...)
                    let allCombinations = Util.getAllPossibleCombinations(matchers, nb);
                    return new OrMatcher(allCombinations.map(combination => new AndMatcher(combination)));
                }
            }
            default: {
                parser.error("unknown function @" + name);
            }
        }
        return null;
    }

    /**
     *
     * @param {Parser} parser
     * @return {Array.<Matcher>}
     */
    static parseMatcherList(parser) {
        let matchers = [];
        if (!parser.consumeChar('(', WHITE)) {
            parser.error("'(' expected (matchers list)");
        }
        while (!parser.consumeChar(')', WHITE) && !parser.eof) {
            matchers.push(RulesParser.parseMatcher(parser));
        }
        return matchers;
    }

    /**
     *
     * @param {Parser} parser
     * @return {String}
     */
    static parseFnArg(parser) {
        if (!parser.consumeChar('(', WHITE)) {
            parser.error("'(' expected (open function arg)");
        }
        let arg = parser.readUntil(SEPARATORS);
        if (!parser.consumeChar(')', WHITE)) {
            parser.error("')' expected (close function arg)");
        }
        return arg;
    }
}

class Simulation {
    /**
     * Simulates a large number of draws on several deck variants
     * @param {Rules} rules
     * @param {Array.<Deck>} decks
     * @param {number} iterations
     * @param {number} draw
     */
    constructor(rules, decks, iterations, draw) {
        this.rules = rules;
        this.decks = decks;
        this.iterations = iterations;
        this.draw = draw;
        this.noMatchCount = new Array(decks.length).fill(0);
        this.matchCount = {};
        rules.criterias.forEach(crit => this.matchCount[crit.name] = new Array(decks.length).fill(0));
    }

    formatHint(count) {
        return count + "/" + this.iterations;
    }

    formatCount(count) {
        return (count * 100.0 / this.iterations).toPrecision(2) + "%";
    }

    toTable() {
        let html = '<table class="table table-sm">';

        // header
        html += '<tr><th scope="col">#</th><th scope="col">rules</th>';
        html += '<th scope="col">main (' + this.decks[0].main.cards.length + ' cards)</th>';
        for (let i = 1; i < this.decks.length; i++) {
            html += '<th scope="col">alt.' + i + ' (' + this.decks[i].main.cards.length + ' cards)</th>';
        }
        html += "</tr>";

        // criterias
        this.rules.criterias.forEach((crit, idx) => {
            html += '<tr>';
            html += '<td scope="row">' + (idx + 1) + '</td>';
            html += '<td>' + crit.name + '</td>';
            for (let i = 0; i < this.decks.length; i++) {
                html += '<td title="' + this.formatHint(this.matchCount[crit.name][i]) + '">' + this.formatCount(this.matchCount[crit.name][i]) + '</td>';
            }
            html += "</tr>";
        });

        // no match count
        html += '<tr class="table-danger">';
        html += '<td scope="row">---</td>';
        html += '<td>no match</td>';
        for (let i = 0; i < this.decks.length; i++) {
            html += '<td title="' + this.formatHint(this.noMatchCount[i]) + '">' + this.formatCount(this.noMatchCount[i]) + '</td>';
        }
        html += "</tr>";


        html += "</table>";
        return html;
    }
}

class Simulator {
    /**
     * Simulates a large number of draws on several deck variants
     * @param {Rules} rules
     * @param {Array.<Deck>} decks
     * @param {number} iterations
     * @param {number} draw
     * @return {Simulation}
     */
    static simulate(rules, decks, iterations, draw) {
        let results = new Simulation(rules, decks, iterations, draw);
        for (let d = 0; d < decks.length; d++) {
            let deck = decks[d];
            for (let it = 0; it < iterations; it++) {
                let hand = deck.main.shuffle().draw(draw);
                let matching = rules.matches(hand);
                if (matching != null) {
                    // increment match count
                    results.matchCount[matching.name][d]++;
                } else {
                    results.noMatchCount[d]++;
                }
            }
        }
        return results;
    }
}
