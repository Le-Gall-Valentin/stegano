package stegano.image;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;

import stegano.layout.SlotOrder;

/**
 * Normalised copy of an image whose red, green and blue least significant bits can be
 * read and written. The alpha channel is never touched.
 *
 * <p>Whatever its original type (palette, grayscale, 16 bits...), the source image is
 * copied into a {@link BufferedImage#TYPE_INT_ARGB} image if it has an alpha channel,
 * {@link BufferedImage#TYPE_INT_RGB} otherwise. Writing bits into a palette or
 * grayscale image directly would silently lose them.
 */
public final class LsbCarrier {

    private static final int RGB_MASK = 0x00FF_FFFF;

    private final BufferedImage image;
    private final int[] pixels;

    private LsbCarrier(BufferedImage image, int[] pixels) {
        this.image = image;
        this.pixels = pixels;
    }

    /**
     * Creates a carrier from a copy of the given image; the source is left untouched.
     *
     * @param source any image
     * @return a carrier backed by a normalised copy of {@code source}
     */
    public static LsbCarrier copyOf(BufferedImage source) {
        int width = source.getWidth();
        int height = source.getHeight();
        boolean hasAlpha = source.getColorModel().hasAlpha();
        BufferedImage copy = new BufferedImage(width, height,
                hasAlpha ? BufferedImage.TYPE_INT_ARGB : BufferedImage.TYPE_INT_RGB);
        int[] pixels = ((DataBufferInt) copy.getRaster().getDataBuffer()).getData();
        // getRGB returns non-premultiplied sRGB values; unlike Graphics.drawImage, it keeps
        // the colour of fully transparent pixels, hence their least significant bits.
        source.getRGB(0, 0, width, height, pixels, 0, width);
        if (!hasAlpha) {
            for (int i = 0; i < pixels.length; i++) {
                pixels[i] &= RGB_MASK;
            }
        }
        return new LsbCarrier(copy, pixels);
    }

    /**
     * Returns the number of available slots, i.e. three per pixel.
     *
     * @return the capacity of the image, in bits
     */
    public long slotCount() {
        return 3L * pixels.length;
    }

    /**
     * Writes bytes, most significant bit first, into the slots given by an order.
     *
     * @param data  the bytes to write
     * @param order the slots to use, advanced by {@code 8 * data.length} positions
     */
    public void write(byte[] data, SlotOrder order) {
        for (byte b : data) {
            for (int bit = 7; bit >= 0; bit--) {
                writeBit(order.next(), (b >>> bit) & 1);
            }
        }
    }

    /**
     * Reads bytes, most significant bit first, from the slots given by an order.
     *
     * @param length the number of bytes to read
     * @param order  the slots to use, advanced by {@code 8 * length} positions
     * @return the bytes read
     */
    public byte[] read(int length, SlotOrder order) {
        byte[] data = new byte[length];
        for (int i = 0; i < length; i++) {
            int value = 0;
            for (int bit = 0; bit < 8; bit++) {
                value = (value << 1) | readBit(order.next());
            }
            data[i] = (byte) value;
        }
        return data;
    }

    /**
     * Returns the image backing this carrier, reflecting every bit written so far.
     *
     * @return the normalised image, in {@code TYPE_INT_RGB} or {@code TYPE_INT_ARGB}
     */
    public BufferedImage image() {
        return image;
    }

    private int readBit(long slot) {
        return (pixels[pixelOf(slot)] >>> shiftOf(slot)) & 1;
    }

    private void writeBit(long slot, int bit) {
        int pixel = pixelOf(slot);
        int shift = shiftOf(slot);
        pixels[pixel] = (pixels[pixel] & ~(1 << shift)) | (bit << shift);
    }

    private static int pixelOf(long slot) {
        return Math.toIntExact(slot / 3);
    }

    /** Red is stored at bits 16-23, green at 8-15 and blue at 0-7 of a packed pixel. */
    private static int shiftOf(long slot) {
        return 16 - 8 * (int) (slot % 3);
    }
}
