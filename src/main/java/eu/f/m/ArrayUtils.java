package eu.f.m;

/**
 * Utilities to handle conversion from various type parameters to an array of a defined type.
 * This utility facilitates conversion from Python code to java.
 *
 * @author Marian-N. I.
 */
final class ArrayUtils {
    private ArrayUtils () { /* prevents instantiation */ }

    static double[] toDoubleArray(double val1, double val2) {
        return new double[] {val1, val2};
    }

    static float[] toFloatArray(double val1, double val2) {
        return new float[] {(float)val1, (float)val2};
    }

    static int[] toIntArray(double val1, double val2) {
        return new int[] {(int)val1, (int)val2};
    }
}
