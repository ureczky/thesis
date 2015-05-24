package hu.ureczky.utils.astro;

public class Barometry {

    /** Sea level standard atmospheric pressure [Pa] */
    private static final double p0 = 101325;

    /** Minimum observed pressure in Pascal [Pa] */
    private static final double p_MIN = 87000;
    
    /** Maximum observed pressure in Pascal [Pa] */
    private static final double p_MAX = 108600;
    
    /** Sea level standard temperature [K] */
    private static final double T0 = 288.15;
    
    /** Earth-surface gravitational acceleration [m/s2] */
    private static final double g = 9.80665;
    
    /** Molar mass of dry air [kg/mol] */
    private static final double M = 0.0289644;
    
    /** Universal gas constant [J/(mol*K)] */
    private static final double R = 8.31447;
    
    /** Temperature lapse rate [K/m] */
    private static final double L = 0.0065;
    
    // Helper constants: (no need to calculate every time)
    private static final double gM_RL = (g*M)/(R*L);
    private static final double RL_gM = (R*L)/(g*M);
    private static final double L_T0 = L/T0;
    private static final double T0_L = T0/L;
    
    /**
     * Calculate the barometric pressure from altitude.
     * @param altitude sea level height in meter [m]
     * @return barometric pressure in Pascal [Pa]
     */
    public static double getPressureFromAltitude(double altitude){
        return Math.pow(p0 * (1 - L_T0 * altitude), gM_RL);
    }
    
    /**
     * Calculate the altitude from barometric pressure.
     * @param p barometric pressure in Pascal [Pa]
     * @return sea level height (altitude) in meter [m]
     */
    public static double getAltitudeFromPressure(double p){
        return getAltitudeFromPressure(p, p0);
    }
    
    /**
     * Calculate the altitude from barometric pressure.
     * @param p barometric pressure in Pascal [Pa] (or [hPa], but same unit as p0)
     * @param p0 sea level barometric pressure in Pascal [Pa] (or [hPa], but same unit as p)
     * @return sea level height in meter [m]
     */
    public static double getAltitudeFromPressure(double p, double p0){
        return T0_L * (1.0 - Math.pow((p / p0), RL_gM));
    }
    
    /**
     * Is a pressure a valid value? (Compare with minimum and maximum observed values) 
     * @param p_hPa pressure in Pascal [Pa]
     * @return validity
     */
    public static boolean isValid(double p_Pa) {
        return (p_MIN <= p_Pa) && (p_Pa <= p_MAX);
    }
    
}
