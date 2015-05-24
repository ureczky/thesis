package hu.ureczky.utils.astro.distancecalculator;

import hu.ureczky.utils.astro.Astronomy;

public class DC_Plane extends DistanceCalculator {
     
    @Override
    protected double getDistance() {
        double dLon = normalizeLongitude(mLon2 - mLon1);
        double dLat = mLat2 - mLat1;
        double diffAngle = Math.sqrt(dLon * dLon + dLat * dLat);
        double dist_m = Astronomy.EARTH_RADIUS * diffAngle;
        
        return dist_m;
    }
        
}
