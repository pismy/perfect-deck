package org.mtgpeasant.perfectdeck.goldfish;

public class GameWonException extends RuntimeException {
    public GameWonException(String reason) {
        super(reason);
    }
}
