package org.mtgpeasant.perfectdeck.goldfish;

public class GameInternalError extends RuntimeException {
    public GameInternalError(String message, Throwable cause) {
        super(message, cause);
    }
}
