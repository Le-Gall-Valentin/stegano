package stegano.image;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

import javax.imageio.ImageIO;

import stegano.StegoException;

/**
 * Reads and writes PNG files, turning every failure into a clear {@link StegoException}.
 */
public final class PngFiles {

    private static final byte[] PNG_SIGNATURE = {
            (byte) 0x89, 'P', 'N', 'G', '\r', '\n', 0x1A, '\n'
    };

    private PngFiles() {
    }

    /**
     * Reads a PNG image.
     *
     * @param path the file to read
     * @return the decoded image, in its original type
     * @throws StegoException if the file is missing, unreadable, not a PNG or corrupted
     */
    public static BufferedImage read(Path path) throws StegoException {
        if (!Files.exists(path)) {
            throw new StegoException("File not found: " + path);
        }
        if (!Files.isRegularFile(path) || !Files.isReadable(path)) {
            throw new StegoException("Cannot read file: " + path);
        }
        try {
            if (!hasPngSignature(path)) {
                throw new StegoException("Not a PNG image: " + path);
            }
            BufferedImage image = ImageIO.read(path.toFile());
            if (image == null) {
                throw new StegoException("Cannot decode PNG image: " + path);
            }
            return image;
        } catch (IOException | RuntimeException e) {
            // ImageIO may report a malformed file with unchecked exceptions as well.
            throw new StegoException("Cannot decode PNG image (corrupted file?): " + path, e);
        }
    }

    /**
     * Writes an image in PNG format, replacing any existing file.
     *
     * @param image the image to write
     * @param path  the destination file, whose parent directory must already exist
     * @throws StegoException if the destination directory is missing or the file cannot be written
     */
    public static void write(BufferedImage image, Path path) throws StegoException {
        Path directory = path.toAbsolutePath().getParent();
        if (directory == null || !Files.isDirectory(directory)) {
            throw new StegoException("Output directory does not exist: " + directory);
        }
        if (Files.isDirectory(path)) {
            throw new StegoException("Output path is a directory: " + path);
        }
        try {
            if (!ImageIO.write(image, "png", path.toFile())) {
                throw new StegoException("No PNG writer is available in this Java runtime.");
            }
        } catch (IOException e) {
            throw new StegoException("Cannot write file: " + path, e);
        }
    }

    private static boolean hasPngSignature(Path path) throws IOException {
        try (InputStream in = Files.newInputStream(path)) {
            return Arrays.equals(in.readNBytes(PNG_SIGNATURE.length), PNG_SIGNATURE);
        }
    }
}
