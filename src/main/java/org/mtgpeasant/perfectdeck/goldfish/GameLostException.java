package org.mtgpeasant.perfectdeck.goldfish;

public class GameLostException extends RuntimeException {
    public GameLostException(String message) {
        super(message);
    }
}
