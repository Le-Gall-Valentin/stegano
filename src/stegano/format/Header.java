package stegano.format;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.zip.CRC32;

import stegano.StegoException;

/**
 * Fixed-size header always stored in clear, sequentially, at the very beginning of the image.
 *
 * <pre>
 * offset  size  field
 *      0     3  signature "STG"
 *      3     1  format version (1)
 *      4     1  flags: bit 0 = encrypted, bit 1 = scattered, other bits reserved (0)
 *      5     4  payload length in bytes, unsigned big-endian
 *      9     4  CRC32 of the payload, big-endian
 * </pre>
 *
 * @param encrypted     whether the payload is encrypted with a password
 * @param scattered     whether the payload is spread over the image in a password-derived order
 * @param payloadLength payload length in bytes
 * @param crc           CRC32 of the payload
 */
public record Header(boolean encrypted, boolean scattered, long payloadLength, int crc) {

    /** Size of the encoded header, in bytes. */
    public static final int SIZE = 13;

    /** Format version written by this implementation. */
    public static final int VERSION = 1;

    /** Largest payload length the 4-byte length field can describe. */
    public static final long MAX_PAYLOAD_LENGTH = 0xFFFF_FFFFL;

    private static final byte[] SIGNATURE = {'S', 'T', 'G'};
    private static final int FLAG_ENCRYPTED = 0b01;
    private static final int FLAG_SCATTERED = 0b10;
    private static final int KNOWN_FLAGS = FLAG_ENCRYPTED | FLAG_SCATTERED;

    /**
     * Validates the header invariants.
     *
     * @throws IllegalArgumentException if the length does not fit in 4 unsigned bytes,
     *                                  or if the payload is scattered without being encrypted
     */
    public Header {
        if (payloadLength < 0 || payloadLength > MAX_PAYLOAD_LENGTH) {
            throw new IllegalArgumentException("payload length out of range: " + payloadLength);
        }
        if (scattered && !encrypted) {
            throw new IllegalArgumentException("a scattered payload must be encrypted");
        }
    }

    /**
     * Builds the header describing a given payload.
     *
     * @param payload   the bytes stored after the header
     * @param encrypted whether the payload is encrypted
     * @param scattered whether the payload is scattered
     * @return the matching header, with the payload length and CRC32 filled in
     */
    public static Header describing(byte[] payload, boolean encrypted, boolean scattered) {
        return new Header(encrypted, scattered, payload.length, crcOf(payload));
    }

    /**
     * Encodes this header.
     *
     * @return the {@value #SIZE} bytes of the header
     */
    public byte[] toBytes() {
        return ByteBuffer.allocate(SIZE)
                .put(associatedData())
                .putInt((int) payloadLength)
                .putInt(crc)
                .array();
    }

    /**
     * Returns the signature, version and flags, which are authenticated along with an
     * encrypted payload so that they cannot be tampered with unnoticed.
     *
     * @return the first 5 bytes of the encoded header
     */
    public byte[] associatedData() {
        return associatedData(encrypted, scattered);
    }

    /**
     * Returns the signature, version and flags of a header with the given flags. Unlike
     * {@link #associatedData()}, it is available before the payload, hence the header, exists.
     *
     * @param encrypted whether the payload is encrypted
     * @param scattered whether the payload is scattered
     * @return the first 5 bytes of the encoded header
     */
    public static byte[] associatedData(boolean encrypted, boolean scattered) {
        return ByteBuffer.allocate(SIGNATURE.length + 2)
                .put(SIGNATURE)
                .put((byte) VERSION)
                .put((byte) flags(encrypted, scattered))
                .array();
    }

    /**
     * Checks a payload against the CRC32 recorded in this header.
     *
     * @param payload the payload read from the image
     * @return {@code true} if the checksums match
     */
    public boolean checksumMatches(byte[] payload) {
        return crcOf(payload) == crc;
    }

    /**
     * Decodes and validates a header.
     *
     * @param bytes exactly {@value #SIZE} bytes read from the image
     * @return the decoded header
     * @throws StegoException if the signature is missing or the format is not supported
     */
    public static Header parse(byte[] bytes) throws StegoException {
        if (bytes.length != SIZE) {
            throw new IllegalArgumentException("a header is " + SIZE + " bytes long, got " + bytes.length);
        }
        ByteBuffer buffer = ByteBuffer.wrap(bytes);
        byte[] signature = new byte[SIGNATURE.length];
        buffer.get(signature);
        if (!Arrays.equals(signature, SIGNATURE)) {
            throw new StegoException("No hidden secret was found in this image.");
        }
        int version = Byte.toUnsignedInt(buffer.get());
        if (version != VERSION) {
            throw new StegoException("Unsupported format: unknown version " + version + ".");
        }
        int flags = Byte.toUnsignedInt(buffer.get());
        if ((flags & ~KNOWN_FLAGS) != 0) {
            throw new StegoException("Unsupported format: reserved flags are set.");
        }
        boolean encrypted = (flags & FLAG_ENCRYPTED) != 0;
        boolean scattered = (flags & FLAG_SCATTERED) != 0;
        if (scattered && !encrypted) {
            throw new StegoException("Unsupported format: a scattered secret must be encrypted.");
        }
        long payloadLength = Integer.toUnsignedLong(buffer.getInt());
        int crc = buffer.getInt();
        return new Header(encrypted, scattered, payloadLength, crc);
    }

    private static int flags(boolean encrypted, boolean scattered) {
        return (encrypted ? FLAG_ENCRYPTED : 0) | (scattered ? FLAG_SCATTERED : 0);
    }

    private static int crcOf(byte[] data) {
        CRC32 crc32 = new CRC32();
        crc32.update(data);
        return (int) crc32.getValue();
    }
}
