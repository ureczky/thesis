package hu.ureczky.utils.astro.geomagneticfield.wmm;

/**
 * Wold Magnetic Model
 * These coefficients and the formulae used below are from:
 * NOAA Technical Report: The US/UK World Magnetic Model for (BASE_YEAR)-(BASE_YEAR+5)
 * More details about the model can be found at
 * <a href="http://www.ngdc.noaa.gov/geomag/WMM/DoDWMM.shtml">http://www.ngdc.noaa.gov/geomag/WMM/DoDWMM.shtml</a>.
 */
public interface WMM {
    public int getBaseYear();
    public float[][] getGCoeff();
    public float[][] getHCoeff();
    public float[][] getDeltaG();
    public float[][] getDeltaH();
}
