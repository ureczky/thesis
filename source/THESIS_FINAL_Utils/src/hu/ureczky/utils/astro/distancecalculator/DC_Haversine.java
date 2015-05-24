package hu.ureczky.utils.astro.distancecalculator;

import hu.ureczky.utils.astro.Astronomy;

public class DC_Haversine extends DistanceCalculator {
     
    @Override
    protected double getDistance() {
        
        // Calculate the angle difference (on the great circle)
        double havDiffAngle = haversine(mLat2 - mLat1) + Math.cos(mLat1) * Math.cos(mLat2) * haversine(mLon2 - mLon1);
        double diffAngle = inv_haversine(havDiffAngle); // radian
        double dist_m = Astronomy.EARTH_RADIUS * diffAngle;
        
        return dist_m;
    }
        
    /**
     * Haversine function
     * @param angle angle in radian
     * @return sin^2(angle/2) 
     */
    private static double haversine(double angle) {
        double sin2 = Math.sin(angle/2);
        return sin2 * sin2;
    }
    
    /**
     * Inverse Haversine function
     * @param haversine
     * @return angle in radians
     */
    private static double inv_haversine(double haversine) {
        return 2 * Math.asin(Math.sqrt(haversine));
    }
    
}
