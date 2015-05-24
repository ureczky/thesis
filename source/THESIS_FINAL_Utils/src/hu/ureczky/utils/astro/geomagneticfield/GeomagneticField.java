package hu.ureczky.utils.astro.geomagneticfield;

/**
 * Estimates magnetic field at a given point on Earth, and in particular,
 * to compute the magnetic declination from true north.
 */
public interface GeomagneticField {
        
    /**
     * @param gdLatitudeDeg  Latitude  in WGS84 geodetic coordinates -- positive is east.
     * @param gdLongitudeDeg Longitude in WGS84 geodetic coordinates -- positive is north.
     * @param altitudeMeters Altitude  in WGS84 geodetic coordinates, in meters.
     */
    public void setParameters(float gdLatitudeDeg, float gdLongitudeDeg, float altitudeMeters);
        
    /** @return The X (northward) component of the magnetic field in nanoteslas. */
    public float getX();

    /** @return The Y (eastward) component of the magnetic field in nanoteslas. */
    public float getY();

    /** @return The Z (downward) component of the magnetic field in nanoteslas. */
    public float getZ();

    /**
     * @return The declination of the horizontal component of the magnetic field from true north, in degrees
     * (i.e. positive means the magnetic field is rotated east that much from true north).
     */
    public float getDeclination();

    /** @return The inclination of the magnetic field in degrees -- Positive means the magnetic field is rotated downwards. */
    public float getInclination();

    /** @return  Horizontal component of the field strength in nanoteslas. */
    public float getHorizontalStrength();

    /** @return  Total field strength in nanoteslas. */
    public float getFieldStrength();

}
