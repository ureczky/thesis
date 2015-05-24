package hu.ureczky.utils.astro;

public class Temperature {
    
    /** Absolute zero: 0 Kelvin in Celsius degrees. The lower limit of the thermodynamic temperature scale. */
    private static final double ABSOLUTE_ZERO = -273.15;
    
    /** The highest temperature ever recorded on Earth was 136 Fahrenheit (58 Celsius) in the Libyan desert. */
    private static final double T_MAX_C = 58;
    
    /** The coldest temperature ever measured was -126 Fahrenheit (-88 Celsius) at Vostok Station in Antarctica. */
    private static final double T_MIN_C = -88;
    
    /**
     * Convert Celsius to Kelvin
     * @param T_C temperature in Celsius [°C]
     * @return temperature in Kelvin [°K]
     */
    public static double toKelvin(double T_C) {
        return T_C - ABSOLUTE_ZERO;
    }
    
    /**
     * Convert Kelvin to Celsius
     * @param T_K temperature in Kelvin [°K]
     * @return temperature in Celsius [°C]
     */
    public static double toCelsius(double T_K) {
        return T_K + ABSOLUTE_ZERO;
    }
    
    /**
     * Is the temperature a valid value? (Compare with minimum and maximum observed values) 
     * @param T_C temperature in Celsius degrees [°C]
     * @return validity
     */
    public static boolean isValid(double T_C) {
        return (T_MIN_C <= T_C) && (T_C <= T_MAX_C);
    } 
    
}
