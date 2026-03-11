package eu.f.m;

/**
 * An IC Case
 * @param name {@link String}
 * @param pinCount {@code int}
 * @param width {@code int}
 * @param height {@code int}
 * @param bottomLeftPin {@code int} or {@literal pin_number} - the ID of the pin #1
 * @param refSize {@code int} chip name font size
 * @param dotPin1Size {@code int} size of th circle that represents pin #1
 * @param dipHeight
 * @param pinSpacingFactor {@code float} multiples of 2.54 mm (tenth of an inch)
 */
record ICPackage(String name, int pinCount, int width, int height, int bottomLeftPin,
                 int refSize, int dotPin1Size, int dipHeight, float pinSpacingFactor) {

    /**
     *
     * @return {@code int} the ID (number) of the bottom left pin, considered as being #1
     */
    int getPinNumber () {
        return bottomLeftPin();
    }
}
