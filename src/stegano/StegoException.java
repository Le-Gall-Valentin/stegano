package stegano;

import java.io.Serial;

/**
 * Signals an expected runtime failure (unreadable file, missing secret, wrong password...)
 * whose message is meant to be shown as is to the end user.
 */
public final class StegoException extends Exception {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * Creates an exception with a user-facing message.
     *
     * @param message a complete sentence describing the failure
     */
    public StegoException(String message) {
        super(message);
    }

    /**
     * Creates an exception with a user-facing message and its underlying cause.
     *
     * @param message a complete sentence describing the failure
     * @param cause   the low-level exception that triggered the failure
     */
    public StegoException(String message, Throwable cause) {
        super(message, cause);
    }
}
