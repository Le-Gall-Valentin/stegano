package stegano.core;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Optional;

import stegano.StegoException;
import stegano.crypto.PasswordCipher;
import stegano.crypto.PasswordCipher.DerivedKeys;
import stegano.format.Header;
import stegano.image.LsbCarrier;
import stegano.layout.ScatteredOrder;
import stegano.layout.SequentialOrder;
import stegano.layout.SlotOrder;

/**
 * Hides secrets into, and reveals them from, the least significant bits of an image.
 *
 * <p>Without a password, the header is followed by the UTF-8 encoded secret, both written
 * sequentially from the first slot. With a password, the payload is
 * {@code salt || nonce || ciphertext || tag}: the header and the salt are written
 * sequentially, the rest is scattered over the remaining slots in an order derived from
 * the password and the salt.
 */
public final class Steganographer {

    /** Message reported when a secret does not fit into an image. */
    public static final String CAPACITY_EXCEEDED =
            "The secret string exceeds the maximum length supported by the image.";

    private static final String WRONG_PASSWORD = "Incorrect password or corrupted data.";

    /** Bytes added to an encrypted secret: salt, nonce and authentication tag. */
    private static final int ENCRYPTION_OVERHEAD =
            PasswordCipher.SALT_LENGTH + PasswordCipher.NONCE_LENGTH + PasswordCipher.TAG_LENGTH;

    /** Number of slots always read and written sequentially when encrypted: header and salt. */
    private static final long ENCRYPTED_SEQUENTIAL_SLOTS = 8L * (Header.SIZE + PasswordCipher.SALT_LENGTH);

    private Steganographer() {
    }

    /**
     * Maximum secret sizes supported by an image.
     *
     * @param withoutPassword the maximum size of a plain secret, in UTF-8 bytes
     * @param withPassword    the maximum size of an encrypted secret, in UTF-8 bytes
     */
    public record Capacity(long withoutPassword, long withPassword) {
    }

    /**
     * A revealed secret.
     *
     * @param secret    the secret
     * @param encrypted whether it was protected by a password
     */
    public record Revealed(String secret, boolean encrypted) {
    }

    /**
     * Computes the maximum secret sizes supported by an image.
     *
     * @param carrier the image
     * @return its capacity, with and without password
     */
    public static Capacity capacity(LsbCarrier carrier) {
        // At most 3 * Integer.MAX_VALUE slots: always below Header.MAX_PAYLOAD_LENGTH bytes.
        long payloadCapacity = carrier.slotCount() / 8 - Header.SIZE;
        return new Capacity(
                Math.max(0, payloadCapacity),
                Math.max(0, payloadCapacity - ENCRYPTION_OVERHEAD));
    }

    /**
     * Hides a secret in a carrier, leaving it untouched if the secret does not fit.
     *
     * @param carrier  the image to write into
     * @param secret   a non-empty secret
     * @param password if present, the password used to encrypt and scatter the secret
     * @throws StegoException if the secret exceeds the capacity of the image
     */
    public static void hide(LsbCarrier carrier, String secret, Optional<String> password) throws StegoException {
        if (secret.isEmpty()) {
            throw new IllegalArgumentException("the secret must not be empty");
        }
        byte[] plaintext = secret.getBytes(StandardCharsets.UTF_8);
        Capacity capacity = capacity(carrier);
        long maxLength = password.isPresent() ? capacity.withPassword() : capacity.withoutPassword();
        if (plaintext.length > maxLength) {
            throw new StegoException(CAPACITY_EXCEEDED);
        }
        SlotOrder sequential = new SequentialOrder(0);
        if (password.isEmpty()) {
            carrier.write(Header.describing(plaintext, false, false).toBytes(), sequential);
            carrier.write(plaintext, sequential);
            return;
        }
        byte[] salt = PasswordCipher.randomBytes(PasswordCipher.SALT_LENGTH);
        byte[] nonce = PasswordCipher.randomBytes(PasswordCipher.NONCE_LENGTH);
        DerivedKeys keys = PasswordCipher.derive(password.get(), salt);
        byte[] sealed = PasswordCipher.encrypt(keys.aesKey(), nonce, plaintext, Header.associatedData(true, true));
        byte[] scatteredPart = concat(nonce, sealed);
        byte[] payload = concat(salt, scatteredPart);

        carrier.write(Header.describing(payload, true, true).toBytes(), sequential);
        carrier.write(salt, sequential);
        carrier.write(scatteredPart, scatteredOrder(carrier, keys));
    }

    /**
     * Reveals the secret hidden in a carrier.
     *
     * @param carrier  the image to read from
     * @param password the password, required if the secret is encrypted and ignored otherwise
     * @return the secret, and whether it was encrypted
     * @throws StegoException if the image holds no valid secret, or the password is missing or wrong
     */
    public static Revealed reveal(LsbCarrier carrier, Optional<String> password) throws StegoException {
        if (carrier.slotCount() < 8L * Header.SIZE) {
            throw new StegoException("No hidden secret was found in this image.");
        }
        SlotOrder sequential = new SequentialOrder(0);
        Header header = Header.parse(carrier.read(Header.SIZE, sequential));
        long minLength = header.encrypted() ? ENCRYPTION_OVERHEAD : 0;
        if (header.payloadLength() > capacity(carrier).withoutPassword() || header.payloadLength() < minLength) {
            throw new StegoException("Invalid data: the declared length is inconsistent with the image capacity.");
        }
        int length = (int) header.payloadLength();

        if (!header.encrypted()) {
            byte[] payload = carrier.read(length, sequential);
            if (!header.checksumMatches(payload)) {
                throw new StegoException("Corrupted data: the checksum does not match.");
            }
            return new Revealed(new String(payload, StandardCharsets.UTF_8), false);
        }

        if (password.isEmpty()) {
            throw new StegoException("This secret is password-protected: use -p <password> to reveal it.");
        }
        byte[] salt = carrier.read(PasswordCipher.SALT_LENGTH, sequential);
        DerivedKeys keys = PasswordCipher.derive(password.get(), salt);
        SlotOrder rest = header.scattered() ? scatteredOrder(carrier, keys) : sequential;
        byte[] scatteredPart = carrier.read(length - PasswordCipher.SALT_LENGTH, rest);
        if (!header.checksumMatches(concat(salt, scatteredPart))) {
            // A wrong password yields a wrong scattering order, hence wrong bytes: both
            // causes are indistinguishable at this point.
            throw new StegoException(WRONG_PASSWORD);
        }
        byte[] nonce = Arrays.copyOfRange(scatteredPart, 0, PasswordCipher.NONCE_LENGTH);
        byte[] sealed = Arrays.copyOfRange(scatteredPart, PasswordCipher.NONCE_LENGTH, scatteredPart.length);
        byte[] plaintext = PasswordCipher.decrypt(keys.aesKey(), nonce, sealed, header.associatedData());
        return new Revealed(new String(plaintext, StandardCharsets.UTF_8), true);
    }

    private static SlotOrder scatteredOrder(LsbCarrier carrier, DerivedKeys keys) {
        return new ScatteredOrder(keys.scatterSeed(), ENCRYPTED_SEQUENTIAL_SLOTS, carrier.slotCount());
    }

    private static byte[] concat(byte[] first, byte[] second) {
        byte[] result = Arrays.copyOf(first, first.length + second.length);
        System.arraycopy(second, 0, result, first.length, second.length);
        return result;
    }
}
