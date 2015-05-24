package hu.ureczky.utils.astro;

public class Astronomy {
    
    public final static double METERS_PER_KILOMETER = 1000;
    
    /** Astronomical Unit in km. */
    public static final double AU = 149597870.691;
    
    /** Earth radius (~ 6372 km) */
    public static final double EARTH_RADIUS = 6372797.0;
    
    /** Earth perimeter (~ 40.000km) */
    public static final double EARTH_PERIMETER = 2 * Math.PI * EARTH_RADIUS;
    
    // WGS-84 ellipsoid params:
    public static final double EARTH_A = 6378137.0;       // Semi-major axis = equatorial radius ("horizontal radius") in meters
    public static final double EARTH_B = 6356752.314245;  // Semi-minor axis = polar distance ("vertical radius") in meters
    public static final double EARTH_F = 1/298.257223563; // Flattening (≈ 3.35 ‰ = 0.0335 %): (a-b)/a
        
    // Budapest's latitude and longitude in degrees and in radians
    public final static double LAT_BUD_DEG = 47.498; // latitude  of the 0 km stone (+-50m precision)
    public final static double LON_BUD_DEG = 19.041; // longitude of the 0 km stone (+-50m precision)    
    public final static double LAT_BUD_RAD = Math.toRadians(LAT_BUD_DEG);
    public final static double LON_BUD_RAD = Math.toRadians(LON_BUD_DEG);
        
}
