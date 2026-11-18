package stegano.cli;

import java.io.Serial;

/**
 * Signals that the command line is invalid; the usage should be shown along with the message.
 */
public final class UsageException extends Exception {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * Creates an exception describing what is wrong with the command line.
     *
     * @param message a complete sentence describing the problem
     */
    public UsageException(String message) {
        super(message);
    }
}
