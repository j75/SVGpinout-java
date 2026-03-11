package eu.f.m;

/**
 * Represents the color to be used when drawing the pin.
 *
 * @param r {@code int} (between 0 - 255) red color
 * @param g {@code int} (between 0 - 255) green color
 * @param b {@code int} (between 0 - 255) blue color
 */
record PinColor(int r, int g, int b) {
    static final PinColor BLACK = new PinColor(0, 0, 0);
    static final PinColor GREY = new PinColor(127, 127, 127);
    static final PinColor RED = new PinColor(255, 0, 0);
}
