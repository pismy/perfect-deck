const Util = function() {
};

Util.getCombinations = function(array, size, start, initialStuff, output) {
    if (initialStuff.length >= size) {
        output.push(initialStuff);
    } else {
        var i;

        for (i = start; i < array.length; ++i) {
            Util.getCombinations(array, size, i + 1, initialStuff.concat(array[i]), output);
        }
    }
    return output;
}

Util.getAllPossibleCombinations = function(array, size) {
    return Util.getCombinations(array, size, 0, [], []);
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
     * @param {Array} input text to parse (array of chars)
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

    get curChar() {
        if(this.curIdx >= this.input.length) {
            return null;
        } else {
            return this.input[this.curIdx];
        }
    }

    get eof() {
        return this.curChar == null;
    }

    nextChar() {
        // --- change line if current char is LF
        if (this.curChar == '\n') {
            this.lineNb++;
            this.colNb = -1;
            this.curLine.length = 0;
        }

        // --- advance
        this.curIdx++;

        // ---
        if (!this.eof && this.curChar != '\n' && this.curChar != '\r') {
            this.curLine.push(this.curChar);
            this.colNb++;
        }
        return this.curChar;
    }

    /**
     * Reads and consumes the given char.
     *
     * @param {char} c
     *            the character to read
     * @param {String} skip
     *            chars allowed to skip
     * @return boolean true if the given char was read
     * @throws ParseError
     */
    consumeChar(c, skip) {
        if (skip != null)
            this.skipChars(skip);
        if (this.curChar != c)
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
        return this.cards.length == 0;
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
     */
    has(card) {
        return this.cards.indexOf(card) >= 0;
    }

    /**
     * Removes the given card.
     * @param {String} card card name.
     */
    remove(card) {
        let copy = Array.from(this.cards);
        copy.remove(card);
        return new Cards(copy);
    }

    /**
     * Adds the given card.
     * @param {String} card card name.
     */
    add(card) {
        let copy = Array.from(this.cards);
        copy.push(card);
        return new Cards(copy);
    }
}

const CARD_LINE = /(SB[:\s]\s*)?(?:(\d+)x?\s+)?(?:\[(.*)\]\s*)?(.+)/;

/**
 * Parses a card line
 * @param {String} line line
 * @returns {*}
 */
const parseCardLine = function (line) {
    line = line.trim();
    if (line.length == 0 || line.startsWith("#")) {
        // empty or commented line
        return null;
    } else {
        var m = CARD_LINE.exec(line);
        return {
            main: m[1] == null,
            count: m[2] == null ? 0 : parseInt(m[2], 10),
            extension: m[3],
            name: m[4]
        }
    }
}

/**
 * Parses a deck
 * @param {String} text deck text
 */
const parseDeck = function (text) {
    let main = [];
    let side = [];
    text.split("\n").forEach((line) => {
        let c = parseCardLine(line);
        for (let i = 0; i < c.count; i++) {
            if (c.main) {
                main.push(c.name)
            } else {
                side.push(c.name)
            }
        }
    });
    return {
        main: new Cards(main),
        side: new Cards(side)
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
     * @returns boolean
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
     *
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
     *
     * @param {Cards} deck
     * @param {Object.<string, Matcher>} matchers
     */
    constructor(deck, matchers) {
        this.deck = deck;
        this.matchers = matchers;
    }

    /**
     *
     * @param {string} name
     * @return {Matcher}
     */
    findByName(name) {
        return this.matchers[name];
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
        if(!context.deck.has(card)) {
            validation.warnings.push("Card [" + card + "] not found in deck");
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
        for (let matcher in this.matchers) {
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
        return this.matchers
            .map((matcher) => {matcher.matches(stream, context)})
            .flatMap();
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
        if (context.findByName(this.name) == null) {
            validation.errors.push("Matcher <" + this.name + "> not found");
        }
    }

    matches(stream, context) {
        return context.findByName(this.name).matches(stream, context);
    }
}

const SEPARATORS = "[]<>()";
const WHITE = " \t";
const DIGITS = "0123456789";

/**
 *
 * @param {String} line
 * @return {*}
 */
function parseRuleDeclaration(line) {
    let parser = new Parser(line);

    // 1: read name
    if (!parser.consumeChar('<', WHITE)) {
        parser.error("'<' expected to open a matcher name declaration");
    }
    let isCriteria = parser.consumeChar('<', "");
    let name = parser.readUntil(SEPARATORS).trim();
    if (name.length == 0) {
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
        matcher = parseFn(parser);
    } else {
        matcher = parseCompound(parser);
    }

    return {
        name: name,
        criteria: isCriteria,
        matcher : matcher
    }
}

/**
 * 
 * @param {Parser} parser
 * @return {Matcher}
 */
function parseMatcher(parser) {
    // 1: read integer (times)
    let timesStr = "";
    while (DIGITS.indexOf(parser.curChar) > 0) {
        timesStr += parser.curChar;
        parser.nextChar();
    }
    let times = 1;
    if (!timesStr.length == 0) {
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
        matcher = parseCompound(parser);
        if (!parser.consumeChar(')', WHITE)) {
            parser.error("')' expected to close a compound matcher");
        }
    } else if (parser.consumeChar('@', WHITE)) {
        // function matcher
        matcher = parseFn(parser);
    } else {
        parser.error("either '[' or '<' expected to declare a matcher");
    }
    parser.skipChars(WHITE);

    if (times == 1) {
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
function parseCompound(parser) {
    let matchers = [];
    let compound = null;
    while (!parser.eof && parser.curChar != ')') {
        if (matchers.length == 1) {
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

        matchers.push(parseMatcher(parser));
    }

    if (matchers.length == 0) {
        parser.error("you should declare at least one matcher");
    }

    if (matchers.length == 1) {
        return matchers.get(0);
    } else if (compound == '&') {
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
function parseFn(parser) {
    let name = parser.readUntil(SEPARATORS).trim();
    if (name.length == 0) {
        parser.error("function name may not be empty");
    }
    switch (name) {
        case "any":
        case "all": {
            let matchers = parseMatcherList(parser);
            if (matchers.length == 0) {
                parser.error("you shall provide at least one matcher");
            }
            if (matchers.length == 1) {
                return matchers.get(0);
            }
            return name == "any" ? new OrMatcher(matchers) : new AndMatcher(matchers);
        }
        case "xof":
        case "atleast": {
            let arg1 = parseFnArg(parser);
            if (arg1.length == 0) {
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
            let matchers = parseMatcherList(parser);
            if (nb == 0) {
                return new NoopMatcher();
            }
            if (nb > matchers.length) {
                parser.error("1st arg cannot exceed matchers size");
            }
            if (matchers.length == 0) {
                parser.error("you shall provide at least one matcher");
            }
            if (matchers.length == 1) {
                return matchers.get(0);
            }
            if (nb == 1) {
                return new OrMatcher(matchers);
            } else if (nb == matchers.length) {
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
function parseMatcherList(parser) {
    let matchers = [];
    if (!parser.consumeChar('(', WHITE)) {
        parser.error("'(' expected (matchers list)");
    }
    while (!parser.consumeChar(')', WHITE) && !parser.eof) {
        matchers.push(parseMatcher(parser));
    }
    return matchers;
}

/**
 *
 * @param {Parser} parser
 * @return {String}
 */
function parseFnArg(parser) {
    if (!parser.consumeChar('(', WHITE)) {
        parser.error("'(' expected (open function arg)");
    }
    let arg = parser.readUntil(SEPARATORS);
    if (!parser.consumeChar(')', WHITE)) {
        parser.error("')' expected (close function arg)");
    }
    return arg;
}














var StackDigester = function (exclude_no_source, inclusion_patterns, exclusion_patterns) {
    // Regexp to capture the Error classname from the first stack trace line
    // group 1: error classname
    this.error_pattern = /((?:[\w$]+\.){2,}[\w$]+):/

    // Regexp to extract stack trace elements information
    // group 1: classname+method
    // group 2: filename (optional)
    // group 3: line number (optional)
    this.stack_element_pattern = /^\s+at\s+((?:[\w$]+\.){2,}[\w$]+)\((?:([^:]+)(?::(\d+))?)?\)/

    this.exclude_no_source = exclude_no_source;

    this.inclusion_patterns = inclusion_patterns;

    this.exclusion_patterns = exclusion_patterns;

    this.compute = function(stack_trace, debugged_stack) {
        var digest = md5.create();

        // 1: extract error class from first line
        var cur_stack_trace_line = stack_trace.shift();
        var error_class = cur_stack_trace_line.match(this.error_pattern);

        // populate debugged stack
        var highlighted_line = cur_stack_trace_line.replace(this.error_pattern, "<span class='error-class' title='Error class'>$1</span>:");
        debugged_stack.push("<div class='line error'><span class='marker' title='Stack error'>E</span>" + highlighted_line+"</div>");

        // digest: error classname
        digest.update(error_class[1]);

        // 2: read all stack trace elements until stack trace is empty or we hit the next error
        var ste_count = 0;
        while (stack_trace.length > 0) {
            cur_stack_trace_line = stack_trace[0];
            if (cur_stack_trace_line.startsWith(' ') || cur_stack_trace_line.startsWith('\t')) {
                // current line starts with a whitespace: is it a stack trace element ?
                var stack_element = cur_stack_trace_line.match(this.stack_element_pattern);
                if (stack_element) {
                    // current line is a stack trace element
                    ste_count += 1;
                    var excluded = this.is_excluded(stack_element);
                    if (!excluded) {
                        // digest: STE classname and method
                        digest.update(stack_element[1]);
                        // digest: line number (if present)
                        if (stack_element[3]) {
                            digest.update(stack_element[3]);
                        }
                    }
                    // populate debugged stack
                    var highlighted_line = cur_stack_trace_line.replace(this.stack_element_pattern, function(match, classname_and_method, file, line) {
                        var fileAndLine = file ? "<span class='file' title='File'>" + file + "</span>" + (line ? ":<span class='ln' title='Line'>" + line + "</span>" : "") : "";
                        return "\tat <span class='classname-and-method' title='Classname and method'>" + classname_and_method + "</span>(" + fileAndLine + ")";
                    });
                    if(excluded) {
                        debugged_stack.push("<div class='line excluded ste'><span class='marker' title='"+excluded+"'>-</span>" + highlighted_line + "</div>");
                    } else {
                        debugged_stack.push("<div class='line match ste'><span class='marker'>+</span>" + highlighted_line + "</div>");
                    }
                } else {
                    debugged_stack.push("<div class='line ignored'><span class='marker'>?</span>" + cur_stack_trace_line + "</div>");
                }
            } else if (ste_count > 0) {
                // current line doesn't start with a whitespace and we've already read stack trace elements: it looks like the next error in the stack
                break
            } else {
                // current line doesn't start with a whitespace and we've not read any stack trace element yet: it looks like a wrapping error message
                debugged_stack.push("<div class='line ignored'><span class='marker'>?</span>" + cur_stack_trace_line+"</div>");
            }
            // move to next line
            stack_trace.shift();
        }

        // 3: if stack trace not empty, compute digest for next error
        if (stack_trace.length > 1) {
            digest.update(this.compute(stack_trace, debugged_stack));
        }

        return digest.hex();
    }

    // Determines whether the given stack trace element (Regexp match) should be excluded from digest computation
    this.is_excluded = function(stack_element) {
        // 1: exclude elements without source info ?
        var lineNb = stack_element[3];
        if (this.exclude_no_source && !lineNb) {
            return "no source info";
        }
        var classnameAndMethod = stack_element[1];
        // 2: Regex based inclusion
        if(this.inclusion_patterns.length > 0) {
            var includedBy = this.inclusion_patterns.find(function (pattern) {
                return classnameAndMethod.match(pattern);
            });
            if(!includedBy) {
                return "not matching any inclusion pattern"
            }
        }
        // 3: Regex based exclusion
        var excludedBy = this.exclusion_patterns.find(function (pattern) {
            return classnameAndMethod.match(pattern);
        });
        return excludedBy ? "excluded by "+excludedBy.toString() : null;
    }
};
