package org.mtgpeasant.perfectdeck.goldfish;

public class IllegalMoveException extends RuntimeException {
    public IllegalMoveException(String message) {
        super(message);
    }
}
