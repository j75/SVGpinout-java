package eu.f.m;

/**
 * DTO that contains the coordinates where various texts (pin number, its name, ...) will be drawn.
 *
 * @param pinInsert {@code double[1]}
 * @param numberInsert {@code double[2]} pin number
 * @param textInsert {@code float[2]} pin name
 * @param arrowInsert {@code int[2]}
 * @param pinSize {@code int[2]}
 * @param textRotationAngle {@code short}
 * @param arrowRotationAngle {@code short}
 *
 * @author Marian-N. I.
 */
record InsertCoordinates(double[] pinInsert, double[] numberInsert, float[] textInsert,
                        int[] arrowInsert, int[] pinSize, short textRotationAngle, short arrowRotationAngle) {
}
