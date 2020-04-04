package org.mtgpeasant.perfectdeck.goldfish;

public class GameInternalError extends RuntimeException {
    private final String gameLogs;

    public GameInternalError(String message, String gameLogs, Throwable cause) {
        super(message, cause);
        this.gameLogs = gameLogs;
    }

    @Override
    public String getMessage() {
        return super.getMessage() + "\n"
                + gameLogs + "\n"
                + "Error: " + getCause().getMessage();
    }
}
