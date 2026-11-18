package stegano.layout;

import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;

/**
 * Visits the slots of a range in a pseudo-random order derived from a seed.
 *
 * <p>The order is a Fisher-Yates shuffle of the virtual array
 * {@code [firstSlot, firstSlot + 1, ..., endSlot - 1]}, computed lazily: step {@code i}
 * draws {@code j = i + nextBelow(n - i)} from an {@link HmacCounterGenerator}, swaps
 * the elements at {@code i} and {@code j}, then returns the element now at {@code i}.
 * Only the swapped elements are stored, so producing {@code k} slots uses
 * {@code O(k)} memory whatever the size of the image.
 */
public final class ScatteredOrder implements SlotOrder {

    private final HmacCounterGenerator generator;
    private final long firstSlot;
    private final long size;
    /** Elements of the virtual array that differ from their index, keyed by index. */
    private final Map<Long, Long> displaced = new HashMap<>();
    private long index;

    /**
     * Creates an order over the slots {@code [firstSlot, endSlot)}.
     *
     * @param seed      the seed of the generator
     * @param firstSlot the first slot of the range, inclusive
     * @param endSlot   the end of the range, exclusive
     */
    public ScatteredOrder(byte[] seed, long firstSlot, long endSlot) {
        if (firstSlot < 0 || endSlot < firstSlot) {
            throw new IllegalArgumentException("invalid range [" + firstSlot + ", " + endSlot + ")");
        }
        this.generator = new HmacCounterGenerator(seed);
        this.firstSlot = firstSlot;
        this.size = endSlot - firstSlot;
    }

    /**
     * {@inheritDoc}
     *
     * @throws NoSuchElementException if every slot of the range has already been returned
     */
    @Override
    public long next() {
        if (index == size) {
            throw new NoSuchElementException("all " + size + " slots have been visited");
        }
        long j = index + generator.nextBelow(size - index);
        long elementAtJ = displaced.getOrDefault(j, j);
        long elementAtIndex = displaced.getOrDefault(index, index);
        displaced.put(j, elementAtIndex);
        displaced.remove(index); // position index is never read again
        index++;
        return firstSlot + elementAtJ;
    }
}
