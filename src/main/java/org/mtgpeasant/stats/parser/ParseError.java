package org.mtgpeasant.stats.parser;

import java.io.PrintStream;
import java.io.PrintWriter;

public class ParseError extends Exception {
	public static int RC_OK = 0;
	public static int RC_INTERNAL_ERROR = 100;
	public static int RC_SYNTAX_ERROR = 101;
	public static int RC_FUNCTION_NOT_FOUND = 102;
	public static int RC_TYPE_NOT_FOUND_ERROR = 103;
	public static int RC_APPLICATION_ERROR = 200;

	private int _code;
	private int _lineNb;
	private int _colNb;
	private Throwable _ex;
	private String _curLine;

	/**
	 * constructor for exception encapsulation
	 */
	public ParseError(Throwable exception, String message, int lineNb, int colNb, String curLine) {
		super(message);
		_ex = exception;
		_lineNb = lineNb;
		_colNb = colNb;
		_curLine = curLine;
		_code = RC_APPLICATION_ERROR;
	}

	/**
	 * constructor for pure script error
	 */
	public ParseError(int code, String message, int lineNb, int colNb, String curLine) {
		super(message);
		_code = code;
		_lineNb = lineNb;
		_colNb = colNb;
		_curLine = curLine;
	}

	public Throwable getWrappedException() {
		return _ex;
	}

	public String getLongMessage() {
		if (_ex != null)
			// return
			// getMessage()+"\nWrapping: "+_ex.getMessage()+"\nat line "+_lineNb+":\n"+_curLine+"\n"+getCurCharArrow();
			return getMessage() + " at line " + _lineNb + ":\nWrapping: " + _ex.getMessage() + "\n" + _curLine + "\n"
					+ getCurCharArrow();

		// return
		// getMessage()+"\nat line "+_lineNb+":\n"+_curLine+"\n"+getCurCharArrow();
		return getMessage() + " at line " + _lineNb + ":\n" + _curLine + "\n" + getCurCharArrow();
	}

	public void printStackTrace() {
		if (_ex != null)
			_ex.printStackTrace();
		else
			super.printStackTrace();
	}

	public void printStackTrace(PrintStream iStream) {
		if (_ex != null)
			_ex.printStackTrace(iStream);
		else
			super.printStackTrace(iStream);
	}

	public void printStackTrace(PrintWriter iWriter) {
		if (_ex != null)
			_ex.printStackTrace(iWriter);
		else
			super.printStackTrace(iWriter);
	}

	public int getCode() {
		return _code;
	}

	public int getLine() {
		return _lineNb;
	}

	public int getColumn() {
		return _colNb;
	}

	public String getScriptLine() {
		return _curLine;
	}

	public String getCurCharArrow() {
		StringBuffer sb = new StringBuffer();
		for (int i = 0; i < _colNb - 1; i++)
			sb.append(' ');
		sb.append('^');
		return sb.toString();
	}
}
