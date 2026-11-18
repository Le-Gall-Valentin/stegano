package stegano.cli;

import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Turns command-line arguments into a {@link Command}. Options may appear in any order;
 * missing, unknown or duplicate arguments are rejected.
 */
public final class CommandParser {

    private static final String INPUT = "-i";
    private static final String OUTPUT = "-o";
    private static final String SECRET = "-s";

    private CommandParser() {
    }

    /**
     * Parses a command line.
     *
     * @param args the arguments given to the program
     * @return the command they describe
     * @throws UsageException if the arguments are invalid
     */
    public static Command parse(String[] args) throws UsageException {
        if (args.length == 0) {
            throw new UsageException("Missing command.");
        }
        String[] rest = Arrays.copyOfRange(args, 1, args.length);
        return switch (args[0]) {
            case "hide" -> parseHide(rest);
            case "reveal" -> parseReveal(rest);
            case "help", "-h", "--help" -> {
                Arguments.scan(rest, Set.of()).requirePositionals(0);
                yield new Command.Help();
            }
            default -> throw new UsageException("Unknown command: " + args[0]);
        };
    }

    private static Command parseHide(String[] args) throws UsageException {
        Arguments arguments = Arguments.scan(args, Set.of(INPUT, OUTPUT, SECRET));
        arguments.requirePositionals(0);
        Path input = toPath(arguments.required(INPUT));
        Path output = toPath(arguments.required(OUTPUT));
        Path fileName = output.getFileName();
        if (fileName == null || !fileName.toString().toLowerCase(Locale.ROOT).endsWith(".png")) {
            throw new UsageException("The output file must have a .png extension: "
                    + "a lossy format would destroy the hidden bits.");
        }
        String secret = arguments.required(SECRET);
        if (secret.isEmpty()) {
            throw new UsageException("The secret string must not be empty.");
        }
        return new Command.Hide(input, output, secret);
    }

    private static Command parseReveal(String[] args) throws UsageException {
        Arguments arguments = Arguments.scan(args, Set.of());
        return new Command.Reveal(toPath(arguments.requirePositionals(1).getFirst()));
    }

    private static Path toPath(String value) throws UsageException {
        if (value.isBlank()) {
            throw new UsageException("A file path must not be empty.");
        }
        try {
            return Path.of(value);
        } catch (InvalidPathException e) {
            throw new UsageException("Invalid file path: " + value);
        }
    }

    /**
     * Options with a value and positional arguments, split from a raw argument list.
     */
    private record Arguments(Map<String, String> options, List<String> positionals) {

        static Arguments scan(String[] args, Set<String> knownOptions) throws UsageException {
            Map<String, String> options = new HashMap<>();
            List<String> positionals = new ArrayList<>();
            for (int i = 0; i < args.length; i++) {
                String arg = args[i];
                if (knownOptions.contains(arg)) {
                    if (i + 1 == args.length) {
                        throw new UsageException("Missing value for option " + arg + ".");
                    }
                    if (options.putIfAbsent(arg, args[++i]) != null) {
                        throw new UsageException("Duplicate option " + arg + ".");
                    }
                } else if (arg.startsWith("-") && arg.length() > 1) {
                    throw new UsageException("Unknown option " + arg + ".");
                } else {
                    positionals.add(arg);
                }
            }
            return new Arguments(options, positionals);
        }

        String required(String option) throws UsageException {
            String value = options.get(option);
            if (value == null) {
                throw new UsageException("Missing option " + option + ".");
            }
            return value;
        }

        List<String> requirePositionals(int count) throws UsageException {
            if (positionals.size() < count) {
                throw new UsageException("Missing argument: an image file is expected.");
            }
            if (positionals.size() > count) {
                throw new UsageException("Unexpected argument: " + positionals.get(count));
            }
            return positionals;
        }
    }
}
