package org.mtgpeasant.perfectdeck.common.utils;

import java.io.Reader;

public class ParseHelper {
	private String NEWLINE_CHARSET = "\n\r";
	private String OPENING_COMMENT_BLOCK = null;
	private String CLOSING_COMMENT_BLOCK = null;

	private Reader _input;

	// --- behavior
	private boolean emptyStringReturnsNull = false;

	// --- read variables
	private StringBuffer _curLine;
	private int curChar = Integer.MIN_VALUE;
	private int[] forseeBuffer = new int[1];
	private int availableInForesee = 0;
	private int lineNb = 0;
	private int colNb = -1;
	private StringBuffer _buffer;

	public ParseHelper(Reader iInput) {
		_input = iInput;
		_buffer = new StringBuffer();
		_curLine = new StringBuffer();
	}

	public void setEmptyStringReturnsNull(boolean emptyStringReturnsNull) {
		this.emptyStringReturnsNull = emptyStringReturnsNull;
	}

	public boolean isEmptyStringReturnsNull() {
		return emptyStringReturnsNull;
	}

	public void setCommentBlockTags(String iOpening, String iClosing) {
		OPENING_COMMENT_BLOCK = iOpening;
		CLOSING_COMMENT_BLOCK = iClosing;
	}

	// +------------------------------------------------
	// | read functions
	// +------------------------------------------------
	public int curChar() throws ParseError {
		if (curChar == Integer.MIN_VALUE)
			nextChar();
		return curChar;
	}

	/**
	 * Reads a char at a next position (from the cursor), but does not consume
	 * it.
	 */
	public int foreseeChar(int iNbChars) throws ParseError {
		if (iNbChars == 0)
			return curChar;
		if (iNbChars > forseeBuffer.length) {
			int[] buff = forseeBuffer;
			forseeBuffer = new int[iNbChars * 2];
			System.arraycopy(buff, 0, forseeBuffer, 0, buff.length);
		}
		while (availableInForesee < iNbChars) {
			try {
				forseeBuffer[availableInForesee++] = _input.read();
			} catch (Exception e) {
				rethrow("IO Exception", e);
			}
		}
		return forseeBuffer[iNbChars - 1];
	}

	private int doReadNextChar() throws ParseError {
		// --- change line if current char is LF
		if (curChar == '\n') {
			lineNb++;
			colNb = -1;
			_curLine.setLength(0);
		}

		if (availableInForesee > 0) {
			curChar = forseeBuffer[0];
			availableInForesee--;
			for (int i = 0; i < availableInForesee; i++)
				forseeBuffer[i] = forseeBuffer[i + 1];
		} else {
			try {
				curChar = _input.read();
			} catch (Exception e) {
				rethrow("IO Exception", e);
			}
		}

		if (curChar != '\n' && curChar != '\r') {
			_curLine.append((char) curChar);
			colNb++;
		}
		return curChar;
	}

	/**
	 * Reads next char and skips comments
	 * 
	 * @return
	 * @throws ParseError
	 */
	public int nextChar() throws ParseError {
		doReadNextChar();
		if (OPENING_COMMENT_BLOCK != null && CLOSING_COMMENT_BLOCK != null) {
			boolean isOpeningComment = true;
			for (int i = 0; i < OPENING_COMMENT_BLOCK.length(); i++) {
				if (foreseeChar(i) != OPENING_COMMENT_BLOCK.charAt(i)) {
					isOpeningComment = false;
					break;
				}
			}
			if (isOpeningComment) {
				// --- now consume opening tag
				for (int i = 0; i < OPENING_COMMENT_BLOCK.length(); i++)
					doReadNextChar();
				// --- then read until closing tag
				StringBuffer comment = new StringBuffer();
				while (curChar >= 0) {
					boolean isClosingComment = true;
					for (int i = 0; i < CLOSING_COMMENT_BLOCK.length(); i++) {
						if (foreseeChar(i) != CLOSING_COMMENT_BLOCK.charAt(i)) {
							isClosingComment = false;
							break;
						}
					}
					if (isClosingComment) {
						// --- now consume closing tag
						for (int i = 0; i < CLOSING_COMMENT_BLOCK.length(); i++)
							doReadNextChar();

						// --- a comment is equivalent to a separator
						curChar = ' ';
						break;
					}
					// --- closing tag not readched: read next char
					comment.append((char) doReadNextChar());
				}
			}
		}
		return curChar;
	}

	/**
	 * Reads and consumes the given char.
	 * 
	 * @param c
	 *            the chararcter to read
	 * @param skip
	 *            chars allowed to skip
	 * @return true if the given char was read
	 * @throws ParseError
	 */
	public boolean consumeChar(char c, String skip) throws ParseError {
		if (skip != null)
			skipChars(skip);
		if (curChar != c)
			return false;

		// --- expected char has been read
		nextChar();
		return true;
	}

	/**
	 * Skips chars belonging to the given string.
	 * 
	 * @param chars
	 *            characters to skip.
	 * @throws ParseError
	 */
	public void skipChars(String chars) throws ParseError {
		while (curChar >= 0 && chars.indexOf(curChar) >= 0)
			nextChar();
	}

	/**
	 * Reads until EOF or a character from the given string is encountered.
	 * 
	 * @param chars
	 *            stop characters.
	 * @return characters read
	 * @throws ParseError
	 */
	public String readUntil(String chars) throws ParseError {
		_buffer.setLength(0);
		while (curChar >= 0 && chars.indexOf(curChar) < 0) {
			_buffer.append((char) curChar);
			nextChar();
		}
		if (emptyStringReturnsNull && _buffer.length() == 0)
			return null;
		return _buffer.toString();
	}

	/**
	 * Reads until EOF or a character different from the given string is
	 * encountered.
	 * 
	 * @param chars
	 *            accepted characters.
	 * @return characters read
	 * @throws ParseError
	 */
	public String readWhile(String chars) throws ParseError {
		_buffer.setLength(0);
		while (curChar >= 0 && chars.indexOf(curChar) >= 0) {
			_buffer.append((char) curChar);
			nextChar();
		}
		if (emptyStringReturnsNull && _buffer.length() == 0)
			return null;
		return _buffer.toString();
	}

	public boolean consumeString(String string) throws ParseError {
		boolean matches = true;
		foreseeChar(string.length());
		for (int i = 0; i < string.length(); i++) {
			if (foreseeChar(i) != string.charAt(i)) {
				matches = false;
				break;
			}
		}
		if (matches) {
			for (int i = 0; i < string.length(); i++)
				consumeChar(string.charAt(i), null);
		}
		return matches;
	}

	public String readUntilString(String iChars) throws ParseError {
		StringBuffer str = new StringBuffer();
		while (true) {
			foreseeChar(iChars.length());
			// boolean matches = true;
			for (int i = 0; i < iChars.length(); i++) {
				if (foreseeChar(i) != iChars.charAt(i)) {
					// matches = false;
					break;
				}
			}
			if (consumeString(iChars))
				return str.toString();

			str.append((char) curChar());
			nextChar();
		}
	}

	public int getLineNumber() {
		return lineNb;
	}

	public int getColumnNumber() {
		return colNb;
	}

	public void rethrow(String message, Throwable th) throws ParseError {
		int l = lineNb;
		int c = colNb;
		readUntil(NEWLINE_CHARSET);
		throw new ParseError(th, message, l, c, _curLine.toString());
	}

	public void error(int type, String message) throws ParseError {
		int l = lineNb;
		int c = colNb;
		readUntil(NEWLINE_CHARSET);
		throw new ParseError(type, message, l, c, _curLine.toString());
	}
}
