package stegano.crypto;

import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Arrays;

import javax.crypto.AEADBadTagException;
import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

import stegano.StegoException;

/**
 * Password-based authenticated encryption: PBKDF2-HMAC-SHA256 key derivation and
 * AES-256-GCM.
 *
 * <p>PBKDF2 derives 64 bytes from the password and a random salt: the first 32 form the
 * AES key, the last 32 the seed of the scattering order.
 */
public final class PasswordCipher {

    /** Length of the random PBKDF2 salt, in bytes. */
    public static final int SALT_LENGTH = 16;

    /** Length of the random GCM nonce, in bytes. */
    public static final int NONCE_LENGTH = 12;

    /** Length of the GCM authentication tag appended to the ciphertext, in bytes. */
    public static final int TAG_LENGTH = 16;

    /**
     * PBKDF2 iteration count of format version 1, as recommended by OWASP for
     * PBKDF2-HMAC-SHA256. Changing it requires a new format version.
     */
    public static final int PBKDF2_ITERATIONS_V1 = 600_000;

    private static final int KEY_LENGTH = 32;
    private static final int SEED_LENGTH = 32;
    private static final SecureRandom RANDOM = new SecureRandom();

    private PasswordCipher() {
    }

    /**
     * Keys derived from a password and a salt.
     *
     * @param aesKey      the AES-256 key
     * @param scatterSeed the 32-byte seed of the scattering order
     */
    public record DerivedKeys(SecretKey aesKey, byte[] scatterSeed) {
    }

    /**
     * Draws cryptographically secure random bytes, for salts and nonces.
     *
     * @param length the number of bytes
     * @return fresh random bytes
     */
    public static byte[] randomBytes(int length) {
        byte[] bytes = new byte[length];
        RANDOM.nextBytes(bytes);
        return bytes;
    }

    /**
     * Derives the AES key and the scattering seed from a password.
     *
     * @param password the password
     * @param salt     a {@value #SALT_LENGTH}-byte salt
     * @return the derived keys
     */
    public static DerivedKeys derive(String password, byte[] salt) {
        char[] chars = password.toCharArray();
        PBEKeySpec spec = new PBEKeySpec(chars, salt, PBKDF2_ITERATIONS_V1, 8 * (KEY_LENGTH + SEED_LENGTH));
        try {
            byte[] material = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded();
            SecretKey aesKey = new SecretKeySpec(material, 0, KEY_LENGTH, "AES");
            byte[] seed = Arrays.copyOfRange(material, KEY_LENGTH, KEY_LENGTH + SEED_LENGTH);
            Arrays.fill(material, (byte) 0);
            return new DerivedKeys(aesKey, seed);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("PBKDF2WithHmacSHA256 is unavailable", e);
        } finally {
            spec.clearPassword();
            Arrays.fill(chars, '\0');
        }
    }

    /**
     * Encrypts and authenticates a plaintext.
     *
     * @param key            the AES key
     * @param nonce          a {@value #NONCE_LENGTH}-byte nonce, never reused with the same key
     * @param plaintext      the data to encrypt
     * @param associatedData data authenticated but not encrypted
     * @return the ciphertext followed by the {@value #TAG_LENGTH}-byte tag
     */
    public static byte[] encrypt(SecretKey key, byte[] nonce, byte[] plaintext, byte[] associatedData) {
        try {
            return cipher(Cipher.ENCRYPT_MODE, key, nonce, associatedData).doFinal(plaintext);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("AES-GCM encryption failed", e);
        }
    }

    /**
     * Checks the authenticity of a ciphertext and decrypts it.
     *
     * @param key            the AES key
     * @param nonce          the nonce used for encryption
     * @param ciphertext     the ciphertext followed by its tag
     * @param associatedData the data authenticated at encryption time
     * @return the plaintext
     * @throws StegoException if authentication fails: wrong password or tampered data
     */
    public static byte[] decrypt(SecretKey key, byte[] nonce, byte[] ciphertext, byte[] associatedData)
            throws StegoException {
        try {
            return cipher(Cipher.DECRYPT_MODE, key, nonce, associatedData).doFinal(ciphertext);
        } catch (AEADBadTagException e) {
            throw new StegoException("Incorrect password or corrupted data.", e);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("AES-GCM decryption failed", e);
        }
    }

    private static Cipher cipher(int mode, SecretKey key, byte[] nonce, byte[] associatedData)
            throws GeneralSecurityException {
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(mode, key, new GCMParameterSpec(8 * TAG_LENGTH, nonce));
        cipher.updateAAD(associatedData);
        return cipher;
    }
}
