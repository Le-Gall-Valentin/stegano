package stegano.core;

import java.nio.charset.StandardCharsets;

import stegano.StegoException;
import stegano.format.Header;
import stegano.image.LsbCarrier;
import stegano.layout.SequentialOrder;
import stegano.layout.SlotOrder;

/**
 * Hides secrets into, and reveals them from, the least significant bits of an image.
 *
 * <p>The header is written first, followed by the payload (the UTF-8 encoded secret),
 * both sequentially from the first slot.
 */
public final class Steganographer {

    /** Message reported when a secret does not fit into an image. */
    public static final String CAPACITY_EXCEEDED =
            "The secret string exceeds the maximum length supported by the image.";

    private Steganographer() {
    }

    /**
     * Hides a secret in a carrier, leaving it untouched if the secret does not fit.
     *
     * @param carrier the image to write into
     * @param secret  a non-empty secret
     * @throws StegoException if the secret exceeds the capacity of the image
     */
    public static void hide(LsbCarrier carrier, String secret) throws StegoException {
        if (secret.isEmpty()) {
            throw new IllegalArgumentException("the secret must not be empty");
        }
        byte[] payload = secret.getBytes(StandardCharsets.UTF_8);
        if (payload.length > maxPayloadLength(carrier)) {
            throw new StegoException(CAPACITY_EXCEEDED);
        }
        SlotOrder order = new SequentialOrder(0);
        carrier.write(Header.describing(payload, false, false).toBytes(), order);
        carrier.write(payload, order);
    }

    /**
     * Reveals the secret hidden in a carrier.
     *
     * @param carrier the image to read from
     * @return the secret
     * @throws StegoException if the image holds no valid secret
     */
    public static String reveal(LsbCarrier carrier) throws StegoException {
        if (carrier.slotCount() < 8L * Header.SIZE) {
            throw new StegoException("No hidden secret was found in this image.");
        }
        SlotOrder order = new SequentialOrder(0);
        Header header = Header.parse(carrier.read(Header.SIZE, order));
        if (header.encrypted()) {
            throw new StegoException("Unsupported format: encrypted secrets are not supported.");
        }
        if (header.payloadLength() > maxPayloadLength(carrier)) {
            throw new StegoException("Invalid data: the declared length exceeds the image capacity.");
        }
        byte[] payload = carrier.read((int) header.payloadLength(), order);
        if (!header.checksumMatches(payload)) {
            throw new StegoException("Corrupted data: the checksum does not match.");
        }
        return new String(payload, StandardCharsets.UTF_8);
    }

    /**
     * Returns the largest payload, in bytes, that fits after the header.
     */
    private static long maxPayloadLength(LsbCarrier carrier) {
        return Math.max(0, carrier.slotCount() / 8 - Header.SIZE);
    }
}
