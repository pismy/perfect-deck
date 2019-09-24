package org.mtgpeasant.stats.utils;

import com.google.common.base.Strings;
import lombok.Getter;
import lombok.Setter;

@Getter
public class ParseError extends Exception {
    public static int RC_OK = 0;
    public static int RC_INTERNAL_ERROR = 100;
    public static int RC_SYNTAX_ERROR = 101;
    public static int RC_APPLICATION_ERROR = 200;

    private int errCode;
    @Setter
    private int line;
    private int column;
    private String curLine;

    /**
     * constructor for exception encapsulation
     */
    public ParseError(Throwable cause, String message, int line, int column, String curLine) {
        super(message, cause);
        this.line = line;
        this.column = column;
        this.curLine = curLine;
        errCode = RC_APPLICATION_ERROR;
    }

    /**
     * constructor for pure script error
     */
    public ParseError(int code, String message, int line, int column, String curLine) {
        super(message);
        errCode = code;
        this.line = line;
        this.column = column;
        this.curLine = curLine;
    }

    @Override
    public String getMessage() {
        return super.getMessage() + " at line " + line + ":\n"
                + curLine + "\n"
                + getCurCharArrow();
    }

    private String getCurCharArrow() {
        return Strings.repeat("-", column) + '^';
    }
}
