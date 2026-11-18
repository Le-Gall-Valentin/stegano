package stegano.layout;

/**
 * Order in which the bit slots of an image are visited.
 *
 * <p>A slot is the least significant bit of one colour component: slot {@code s} is
 * component {@code s % 3} (0 = red, 1 = green, 2 = blue) of pixel {@code s / 3}, pixels
 * being numbered row by row from the top-left corner. An order is stateful: each call
 * to {@link #next()} yields a new slot, never the same one twice.
 */
public sealed interface SlotOrder permits SequentialOrder {

    /**
     * Returns the next slot to read or write.
     *
     * @return a slot index
     */
    long next();
}
