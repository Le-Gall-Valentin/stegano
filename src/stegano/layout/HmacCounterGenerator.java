package stegano.layout;

import java.nio.ByteBuffer;
import java.security.GeneralSecurityException;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/**
 * Deterministic pseudo-random generator, identical on every platform and JVM.
 *
 * <p>The byte stream is the concatenation of the blocks
 * {@code HMAC-SHA256(seed, uint64_be(k))} for {@code k = 0, 1, 2...}. Each draw consumes
 * the next 8 bytes of the stream as an unsigned big-endian 64-bit integer.
 */
public final class HmacCounterGenerator {

    private static final String ALGORITHM = "HmacSHA256";

    private final Mac mac;
    private final ByteBuffer counter = ByteBuffer.allocate(Long.BYTES);
    private ByteBuffer block = ByteBuffer.allocate(0);
    private long blockIndex;

    /**
     * Creates a generator.
     *
     * @param seed the HMAC key determining the whole stream
     */
    public HmacCounterGenerator(byte[] seed) {
        try {
            mac = Mac.getInstance(ALGORITHM);
            mac.init(new SecretKeySpec(seed, ALGORITHM));
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException(ALGORITHM + " is unavailable", e);
        }
    }

    /**
     * Returns a uniformly distributed integer in {@code [0, bound)}. Draws whose value falls
     * in the incomplete last interval of size {@code bound} are rejected, so there is no
     * modulo bias.
     *
     * @param bound the exclusive upper bound, strictly positive
     * @return the next integer in {@code [0, bound)}
     */
    public long nextBelow(long bound) {
        if (bound <= 0) {
            throw new IllegalArgumentException("bound must be positive: " + bound);
        }
        while (true) {
            long draw = nextLong();
            long value = Long.remainderUnsigned(draw, bound);
            // Accept when draw - value + bound does not exceed 2^64, i.e. the whole
            // interval [draw - value, draw - value + bound) lies within the 64-bit range.
            if (Long.compareUnsigned(draw - value, -bound) <= 0) {
                return value;
            }
        }
    }

    private long nextLong() {
        if (block.remaining() < Long.BYTES) {
            counter.clear().putLong(blockIndex++).flip();
            mac.update(counter);
            block = ByteBuffer.wrap(mac.doFinal());
        }
        return block.getLong();
    }
}
