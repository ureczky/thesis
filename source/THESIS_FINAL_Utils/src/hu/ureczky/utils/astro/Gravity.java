package hu.ureczky.utils.astro;

import android.util.Log;

public class Gravity {

    private static final String TAG = "Gravity";
    
    // Constants
    private static final double g_eq = 9.7803267714;
    private static final double k    = 0.00193185138639;
    private static final double e2   = 0.00669437999013;
    
    /**
     * International Gravity Formula (IGF) - IGF84
     * Calculate the gravitational acceleration
     * on the WGS84 (World Geodetic System 1984) ellipsoid.
     * @see http://principles.ou.edu/earth_figure_gravity/
     * @param latRad - latitude in radians. [-Pi..Pi], but symmetric, so [0...Pi] is giving the same result
     * @return gravitational acceleration in m/s^2
     */
    public static double gravityFromLatitude(double latRad) {

        double latSin = Math.sin(latRad); // sin(latitude)
        double latSin2 = latSin * latSin; // sin^2(latitude)

        double g = g_eq * (1 + k * latSin2) / Math.sqrt(1 - e2 * latSin2);
        return g;
    }
    
    /**
     * Inverse IGF
     * @see calcGravity
     * Calculate the 
     * @param g - gravitational acceleration in m/s^2
     * @return latitude in radians in range of [0..90]
     */
    public static double latitudeFromGravity(double g) {
        double G = g * g / g_eq / g_eq; // just for shortening
        
        // sin^2(latitude)
        double sin2Lat = (-2 * k - e2 * G + Math.sqrt((2*k+e2*G)*(2*k+e2*G) - 4*k*k*(1-G))) / (2*k*k);
        
        double lat_rad = Math.asin(Math.sqrt(sin2Lat));
        return lat_rad;
    }

    // Usage example
    public static void test() {
        
        for(double latDeg = 0.0; latDeg <= 90.0; latDeg += 10.0) {
        
            //double latDeg = 47.5; //Budapest
            Log.d(TAG, "latitude : " + latDeg + " degree");
            
            double latRad = Math.toRadians(latDeg);
            double g = gravityFromLatitude(latRad);
            
            Log.d(TAG, "- gravity: " + g + " m/s^2");
            
            double latDegBack = Math.toDegrees(latitudeFromGravity(g));
            double diffErrorDeg = latDegBack - latDeg;
            
            Log.d(TAG, "- error  : " + diffErrorDeg + "degree");
        }
    }
    
}
