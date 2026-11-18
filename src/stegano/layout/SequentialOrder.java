package stegano.layout;

/**
 * Visits the slots one after the other, starting from a given slot.
 */
public final class SequentialOrder implements SlotOrder {

    private long nextSlot;

    /**
     * Creates an order starting at the given slot.
     *
     * @param firstSlot the first slot returned by {@link #next()}
     */
    public SequentialOrder(long firstSlot) {
        if (firstSlot < 0) {
            throw new IllegalArgumentException("negative slot: " + firstSlot);
        }
        this.nextSlot = firstSlot;
    }

    @Override
    public long next() {
        return nextSlot++;
    }
}
