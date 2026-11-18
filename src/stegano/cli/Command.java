package stegano.cli;

import java.nio.file.Path;
import java.util.Optional;

/**
 * A validated command, as parsed from the command line.
 */
public sealed interface Command {

    /**
     * Hides a secret into an image.
     *
     * @param input    the source PNG image
     * @param output   the PNG image to produce
     * @param secret   the non-empty secret to hide
     * @param password if present, the non-empty password used to encrypt and scatter the secret
     */
    record Hide(Path input, Path output, String secret, Optional<String> password) implements Command {
    }

    /**
     * Reveals the secret hidden in an image.
     *
     * @param image    the PNG image holding the secret
     * @param password if present, the non-empty password of an encrypted secret
     */
    record Reveal(Path image, Optional<String> password) implements Command {
    }

    /**
     * Shows the usage.
     */
    record Help() implements Command {
    }
}
