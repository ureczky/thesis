package hu.ureczky.utils.astro.distancecalculator;

import hu.ureczky.utils.astro.Astronomy;

public class DC_Sphere extends DistanceCalculator {
     
    @Override
    protected double getDistance() {
        double cLat1 = Math.cos(mLat1);
        double cLat2 = Math.cos(mLat2);
        double sLat1 = Math.sin(mLat1);
        double sLat2 = Math.sin(mLat2);
        double dLon  = Math.abs(mLon2 - mLon1);
        double cDLon = Math.cos(dLon);
        double angle = Math.acos(cLat1 * cLat2 * cDLon + sLat1 * sLat2);
        double dist_m = Astronomy.EARTH_RADIUS * angle;
        
        return dist_m;
    }
    
}
