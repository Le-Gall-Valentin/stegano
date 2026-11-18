package stegano;

import java.io.PrintStream;
import java.nio.file.Path;
import java.util.Optional;

import stegano.cli.Command;
import stegano.cli.CommandParser;
import stegano.cli.UsageException;
import stegano.core.Steganographer;
import stegano.core.Steganographer.Revealed;
import stegano.image.LsbCarrier;
import stegano.image.PngFiles;

/**
 * Command-line entry point of the LSB steganography tool.
 *
 * <p>Exit codes: {@value #SUCCESS} on success, {@value #FAILURE} on a runtime error and
 * {@value #MISUSE} on an invalid command line. Errors and usage go to the standard error.
 */
public final class Main {

    /** Exit code of a successful run. */
    public static final int SUCCESS = 0;
    /** Exit code of a run that failed while processing valid arguments. */
    public static final int FAILURE = 1;
    /** Exit code of a run with an invalid command line. */
    public static final int MISUSE = 2;

    private static final String PREFIX = "--> ";
    private static final String USAGE = """
            Usage:
              java -jar stegano.jar hide -i <input.png> -o <output.png> -s "secret" [-p <password>]
              java -jar stegano.jar reveal <image.png> [-p <password>]
              java -jar stegano.jar help""";

    private final PrintStream out;
    private final PrintStream err;

    private Main(PrintStream out, PrintStream err) {
        this.out = out;
        this.err = err;
    }

    /**
     * Runs the program and exits with its status code.
     *
     * @param args the command-line arguments
     */
    public static void main(String[] args) {
        System.exit(run(args, System.out, System.err));
    }

    /**
     * Runs the program without exiting the JVM.
     *
     * @param args the command-line arguments
     * @param out  where results are printed
     * @param err  where errors and usage are printed
     * @return the exit code
     */
    public static int run(String[] args, PrintStream out, PrintStream err) {
        return new Main(out, err).run(args);
    }

    private int run(String[] args) {
        try {
            execute(CommandParser.parse(args));
            return SUCCESS;
        } catch (UsageException e) {
            error(e.getMessage());
            err.println(USAGE);
            return MISUSE;
        } catch (StegoException e) {
            error(e.getMessage());
            return FAILURE;
        } catch (OutOfMemoryError e) {
            error("Not enough memory to process this image.");
            return FAILURE;
        } catch (RuntimeException e) {
            error("Unexpected error: " + e);
            return FAILURE;
        }
    }

    private void execute(Command command) throws StegoException {
        switch (command) {
            case Command.Hide(Path input, Path output, String secret, Optional<String> password) -> {
                LsbCarrier carrier = LsbCarrier.copyOf(PngFiles.read(input));
                Steganographer.hide(carrier, secret, password);
                PngFiles.write(carrier.image(), output);
                out.println(PREFIX + "secret successfully hidden in \"" + output + "\"");
            }
            case Command.Reveal(Path image, Optional<String> password) -> {
                Revealed revealed = Steganographer.reveal(LsbCarrier.copyOf(PngFiles.read(image)), password);
                if (password.isPresent() && !revealed.encrypted()) {
                    error("Warning: this secret is not encrypted, the password was ignored.");
                }
                out.println(PREFIX + "secret message is: \"" + escapeControls(revealed.secret()) + "\"");
            }
            case Command.Help() -> out.println(USAGE);
        }
    }

    private void error(String message) {
        err.println(PREFIX + "/!\\ " + message);
    }

    /**
     * Escapes control characters other than line feed and tab as {@code \}{@code uXXXX}, so that
     * a secret read from an untrusted image cannot inject terminal escape sequences.
     */
    private static String escapeControls(String text) {
        StringBuilder escaped = new StringBuilder(text.length());
        text.chars().forEach(c -> {
            if (Character.isISOControl(c) && c != '\n' && c != '\t') {
                escaped.append(String.format("\\u%04X", c));
            } else {
                escaped.append((char) c);
            }
        });
        return escaped.toString();
    }
}
