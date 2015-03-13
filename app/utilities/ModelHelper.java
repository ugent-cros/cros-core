package utilities;

/**
 * Created by Benjamin on 11/03/2015.
 */
public class ModelHelper {

    public static boolean compareFloatingPoints(double a, double b) {
        return Math.abs(a - b) < 0.000001;
    }

    public static boolean compareFloatingPoints(float a, float b) {
        return Math.abs(a - b) < 0.000001;
    }

    public static boolean compareFloatingPoints(float a, double b) {
        return Math.abs(a - b) < 0.000001;
    }
}
