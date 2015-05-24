package hu.ureczky.utils.astro.distancecalculator;

import android.util.Log;

public abstract class DistanceCalculator {
    
    private static final String TAG = "DistanceCalculator";
    
    public enum MODEL_TYPE {
        PLANE,
        SPHERE,
        HAVERSINE,
        ELLIPSOID
    }
    
    public enum ANGLE_TYPE {
        DEGREE,
        RADIAN
    };
    
    private ANGLE_TYPE mAngleType;
    
    protected double mLat1;
    protected double mLat2;
    protected double mLon1;
    protected double mLon2;
    
    // Factory
    public static DistanceCalculator create(MODEL_TYPE modelType, ANGLE_TYPE angleType) {
        DistanceCalculator DC = null;
        switch(modelType) { 
            case PLANE:     DC = new DC_Plane();     break;
            case SPHERE:    DC = new DC_Sphere();    break;
            case HAVERSINE: DC = new DC_Haversine(); break;
            case ELLIPSOID: DC = new DC_Ellipsoid(); break;
            default:
                Log.e(TAG, "Unknown MODEL_TYPE");
        }
        if(DC != null)
        {
            DC.mAngleType = angleType;
        }
        return DC;
    }
        
    /**
     * Get the distance between 2 points.
     * @param lat1 latitude  of the first point. Degree or radian, according to ANGLE_TYPE
     * @param lon1 longitude of the first point. Degree or radian, according to ANGLE_TYPE
     * @param lat2 latitude  of the other point. Degree or radian, according to ANGLE_TYPE
     * @param lon2 longitude of the other point. Degree or radian, according to ANGLE_TYPE
     * @return distance in meters
     */
    public double getDistance(double lat1, double lon1, double lat2, double lon2) {
        
        // Convert angles if needed
        switch(mAngleType){
            case DEGREE:
                mLat1 = Math.toRadians(lat1);
                mLon1 = Math.toRadians(lon1);
                mLat2 = Math.toRadians(lat2);
                mLon2 = Math.toRadians(lon2);
                break;
            case RADIAN:
                mLat1 = lat1;
                mLon1 = lon1;
                mLat2 = lat2;
                mLon2 = lon2;
                break;
            default:
                Log.e(TAG, "Unknown ANGLE_TYPE");
        }
        
        return getDistance();
    }
    
    /**
     * Distance calculating algorithm's implementation.
     * Should be overridden by the subclasses.
     */
    protected abstract double getDistance();
        
    protected double normalizeLongitude(double lon)
    {
        if(lon < -Math.PI) return 2 * Math.PI + lon;
        if(lon > +Math.PI) return lon - 2 * Math.PI;
        return lon;
    }
        
}
