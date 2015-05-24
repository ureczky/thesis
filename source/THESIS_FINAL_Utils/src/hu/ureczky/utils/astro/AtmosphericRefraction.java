package hu.ureczky.utils.astro;

public class AtmosphericRefraction {
    
    private double mBaseRefrDeg        = 0; // Base refraction in arcmins [']
    private double mPressureCorrection    = 1;
    private double mTemperatureCorrection = 1;
    
    // Conversion values
    private static final int    ARCMINS_PER_DEGREE = 60;
    private static final double DEGREES_PER_ARCMIN = 1.0 / ARCMINS_PER_DEGREE;
    
    // Coefficients for the two equations
    private static final double[] COEFFS_BENNET      = new double[] {1.00, 7.31, 4.40};
    private static final double[] COEFFS_SAEMUNDSSON = new double[] {1.02, 10.3, 5.11};
    
    // Helper method
    private AtmosphericRefraction setElevation(double elevDeg, double[] coeffs) {
        double angleDeg = elevDeg + coeffs[1] / (elevDeg + coeffs[2]);
        mBaseRefrDeg = DEGREES_PER_ARCMIN * coeffs[0] / Math.tan(Math.toRadians(angleDeg));
        return this;
    }
        
    /**
     * Bennett formula for apparent elevation (what we see the object, destorted with atmospheric refraction)
     * @param apparent elevation angle in degrees [°]
     * @return this (for Builder-like usage)
     */
    public AtmosphericRefraction setApparentElevation(double app_elev_deg) {
        return setElevation(app_elev_deg, COEFFS_BENNET);
    }
    
    /**
     * Saemundsson formula for true elevation (where the object really is)
     * @param elevation angle in degrees [°]
     * @return this (for Builder-like usage)
     */
    public AtmosphericRefraction setTrueElevation(double true_elev_deg) {
        return setElevation(true_elev_deg, COEFFS_SAEMUNDSSON);
    }
        
    /**
     * Get the atmospheric refraction angle.
     * @return refraction angle in degrees [°]
     */
    public double getRefraction() {
        return mBaseRefrDeg * mPressureCorrection * mTemperatureCorrection;
    }
    
    ////////////////////////////////
    // Corrections for refraction //
    ////////////////////////////////
    
    public  static final double BASE_TEMP_C   = 10;                                // 10 degree Celsius in Celsius
    private static final double BASE_TEMP_K   = Temperature.toKelvin(BASE_TEMP_C); // Base temperature in Kelvin
    public  static final double BASE_PRESS_PA = 101000;                            // 1010 hPa
    
    /**
     * Pressure correction, because refraction
     * <ul>
     * <li>Refraction increases approximately 1% for every 0.9 kPa increase in pressure</li>
     * <li>Refraction decreases approximately 1% for every 0.9 kPa decrease in pressure</li>
     * </ul>
     * @param p_Pa pressure in Pascal [Pa]
     * @return this (for Builder-like usage)
     */
    public AtmosphericRefraction setPressure(double p_Pa) {
        mPressureCorrection = p_Pa / BASE_PRESS_PA;
        return this;
    }
            
    /**
     * Temperature correction, because refraction
     * <ul>
     * <li>increases approximately 1% for every 3 °C decrease in temperature</li>
     * <li>decreases approximately 1% for every 3 °C increase in temperature</li>
     * </ul>
     * @param T_C temperature in Celsius degrees [°C]
     * @return this (for Builder-like usage)
     */    
    public AtmosphericRefraction setTemperature(double T_C) {
        mTemperatureCorrection = Temperature.toKelvin(T_C) / BASE_TEMP_K;
        return this;
    } 
    
}
