package eu.f.m;

/**
 * The edges of the chip.
 *
 * @author Marian-N. I.
 */
enum Edge {

    BOTTOM {
        @Override
        InsertCoordinates computePosition (int corner_offset, float pin_offset, int chip_h, int maxPinSize,
                                          double number_length, int chip_w,
                                          int bottom_offset, double centering, int right_offset) {
            // orig: (corner_offset + pin_offset, chip_h + pin_length + arrow_length)
            double[] pin_insert = ArrayUtils.toDoubleArray(corner_offset + pin_offset,
                    chip_h + maxPinSize + BatikDrawChip.arrow_length);
            // orig: (-pin_insert[1] - pin_length + centering, pin_insert[0] + 15.5)
            float[] text_insert = ArrayUtils.toFloatArray(-pin_insert[1] - maxPinSize + centering,
                    pin_insert[0] + YTEXT_OFFSET);
            // original: (pin_insert[0] - 10 - number_length*2, pin_insert[1] + 15.5)
            double[] number_insert = ArrayUtils.toDoubleArray(pin_insert[0] + NUMBER_OFFSET - number_length,
                    pin_insert[1] - NUMBER_OFFSET);
            int[] pin_size = ArrayUtils.toIntArray(BatikDrawChip.pin_width, maxPinSize); //(str(pin_width) + "px", str(pin_length) + "px")
            short text_rotation = -90; // "rotate(-90)"
            // orig: (pin_insert[1] + maxPinSize, -pin_insert[0] - DrawChip.pin_width);
            int[] arrow_insert = ArrayUtils.toIntArray (pin_insert[1] + maxPinSize, -pin_insert[0] - BatikDrawChip.pin_width);
            short arrow_rotation = 90; //0; // "rotate(90)"

            return new InsertCoordinates(pin_insert, number_insert, text_insert, arrow_insert, pin_size,
                    text_rotation, arrow_rotation);
        }
    },
    RIGHT {
        @Override
        InsertCoordinates computePosition (int corner_offset, float pin_offset, int chip_h, int maxPinSize,
                                          double number_length, int chip_w,
                                          int bottom_offset, double centering, int right_offset) {
            double[] pin_insert = ArrayUtils.toDoubleArray(chip_w + maxPinSize + BatikDrawChip.arrow_length, bottom_offset - pin_offset);
            float[] text_insert = ArrayUtils.toFloatArray(pin_insert[0] + centering, pin_insert[1] + YTEXT_OFFSET);
            double[] number_insert = ArrayUtils.toDoubleArray(pin_insert[0] - NUMBER_OFFSET - number_length * 2,
                    pin_insert[1] + YTEXT_OFFSET);
            int[] pin_size = ArrayUtils.toIntArray(maxPinSize, BatikDrawChip.pin_width); // (str(pin_length) + "px", str(pin_width) + "px")
            short text_rotation = 0; // "rotate(0)"
            //  (pin_insert[0] + maxPinSize, pin_insert[1]);
            int[] arrow_insert = ArrayUtils.toIntArray(pin_insert[0] + maxPinSize, pin_insert[1]);
            short arrow_rotation = 0; // -90; // "rotate(0)"

            return new InsertCoordinates(pin_insert, number_insert, text_insert, arrow_insert, pin_size,
                    text_rotation, arrow_rotation);
        }
    },
    UP {
        @Override
        InsertCoordinates computePosition(int corner_offset, float pin_offset, int chip_h, int maxPinSize,
                                         double number_length, int chip_w,
                                         int bottom_offset, double centering, int right_offset) {
            // original: pin_insert = (right_offset - pin_offset, arrow_length)
            double[] pin_insert = ArrayUtils.toDoubleArray(right_offset - pin_offset, BatikDrawChip.arrow_length);
            // original: text_insert = (-pin_insert[1] - pin_length + centering, pin_insert[0] + 15.5);
            float[] text_insert = ArrayUtils.toFloatArray(-pin_insert[1] - maxPinSize + centering + PIN_TEXT_OFFSET,
                    pin_insert[0] + YTEXT_OFFSET);
            double[] number_insert = ArrayUtils.toDoubleArray(pin_insert[0] + NUMBER_OFFSET - number_length,
                    pin_insert[1] + maxPinSize + 20);
            int[] pin_size = ArrayUtils.toIntArray(BatikDrawChip.pin_width, maxPinSize); //(str(pin_width) + "px", str(pin_length) + "px")
            short text_rotation = -90; // "rotate(-90)"
            int[] arrow_insert = ArrayUtils.toIntArray(-BatikDrawChip.arrow_length, pin_insert[0]);
            short arrow_rotation = -90; //180; // "rotate(-90)"

            return new InsertCoordinates(pin_insert, number_insert, text_insert, arrow_insert, pin_size,
                    text_rotation, arrow_rotation);
        }
    },
    LEFT {
        @Override
        InsertCoordinates computePosition(int corner_offset, float pin_offset, int chip_h, int maxPinSize,
                                         double number_length, int chip_w,
                                         int bottom_offset, double centering, int right_offset) {
            double[] pin_insert = ArrayUtils.toDoubleArray(BatikDrawChip.arrow_length, corner_offset + pin_offset);
            float[] text_insert = ArrayUtils.toFloatArray(pin_insert[0] + centering, pin_insert[1] + YTEXT_OFFSET);
            double[] number_insert = ArrayUtils.toDoubleArray(pin_insert[0] + NUMBER_OFFSET + maxPinSize,
                    pin_insert[1] + YTEXT_OFFSET);
            int[] pin_size = ArrayUtils.toIntArray(maxPinSize, BatikDrawChip.pin_width); // (str(pin_length) + "px", str(pin_width) + "px")
            short text_rotation = 0; // "rotate(0)"
            // ArrayUtils.toIntArray(-DrawChip.arrow_length, -pin_insert[1] - DrawChip.pin_width);
            int[] arrow_insert = ArrayUtils.toIntArray(-BatikDrawChip.arrow_length, -pin_insert[1] - BatikDrawChip.pin_width);
            short arrow_rotation = 180; // 90; // "rotate(180)"

            return new InsertCoordinates(pin_insert, number_insert, text_insert, arrow_insert, pin_size,
                    text_rotation, arrow_rotation);
        }
    };

    private static final int PIN_TEXT_OFFSET = 1;
    private static final float YTEXT_OFFSET = 15.5f;
    private static final int NUMBER_OFFSET = 10;

    /**
     *
     * @param corner_offset {@code int}
     * @param pin_offset {@code float}
     * @param chip_h {@code int}
     * @param maxPinSize {@code int}
     * @param number_length {@code double} length of the {@link String} representing number
     * @param chip_w {@code int}
     * @param bottom_offset {@code int}
     * @param centering {@code double}
     * @param right_offset {@code int}
     * @return {@link InsertCoordinates}
     */
    abstract InsertCoordinates computePosition (int corner_offset, float pin_offset, int chip_h, int maxPinSize,
                                               double number_length, int chip_w, int bottom_offset,
                                               double centering, int right_offset);
}
