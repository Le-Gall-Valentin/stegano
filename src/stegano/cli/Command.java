package stegano.cli;

import java.nio.file.Path;

/**
 * A validated command, as parsed from the command line.
 */
public sealed interface Command {

    /**
     * Hides a secret into an image.
     *
     * @param input  the source PNG image
     * @param output the PNG image to produce
     * @param secret the non-empty secret to hide
     */
    record Hide(Path input, Path output, String secret) implements Command {
    }

    /**
     * Reveals the secret hidden in an image.
     *
     * @param image the PNG image holding the secret
     */
    record Reveal(Path image) implements Command {
    }

    /**
     * Shows the usage.
     */
    record Help() implements Command {
    }
}
