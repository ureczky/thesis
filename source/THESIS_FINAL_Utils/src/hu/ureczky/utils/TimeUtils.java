package hu.ureczky.utils;

import hu.ureczky.utils.tests.Tests;

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.SimpleTimeZone;


public class TimeUtils {
    
    private static final String TAG = "TimeUtils";

    // Julian Days: @see: http://aa.usno.navy.mil/data/docs/JulianDate.php
    
    /** Julian date of 1900.01.01, 00:00:00.000 UTC */
    private static final double JD_1900_0 = 2415020.5;
    
    /** Julian date of 1970.01.01, 00:00:00.000 UTC */
    private static final double JD_1970_0 = 2440587.5;
    
    /** Julian date of 2000.01.01, 12:00:00.000 UTC */
    private static final double JD_2000 = 2451545.0;
    
    /** Julian date of 1858.11.17,00:00:00.000 UTC */
    private static final double JD_1858_11_17_0 = 2400000.5;
    
    /** Julian date of 0001.01.01,00:00:00.000 UTC */
    private static final double JD_1_0 = 1721423.5;
        
    /** Julian days per century */
    public static final int DAYS_PER_JULIAN_CENTURY = 36525;
    
    /** Julian days per year. */
    public static final double DAYS_PER_JULIAN_YEAR = DAYS_PER_JULIAN_CENTURY / 100.0;
    
    // Convert units
    public static final int MILLISECONDS_PER_SECOND = 1000;
    public static final int SECONDS_PER_MINUTE      = 60;
    public static final int MINUTES_PER_HOUR        = 60;
    public static final int HOURS_PER_DAY           = 24;
    public static final int MILLISECONDS_PER_DAY    = HOURS_PER_DAY * MINUTES_PER_HOUR * SECONDS_PER_MINUTE * MILLISECONDS_PER_SECOND;
    public static final double DAY_PER_MILLISECOND  = 1.0 / MILLISECONDS_PER_DAY;
    
    /**
     * Convert UTC time to Julian Day (JD)
     * <br/> Days since -4712.01.01, 12:00:00.000 UTC
     * <br/> which is B.C. 4713 (because there is no 0-th year, 1 = A.C. 1, 0 = B.C. 1, -1 = B.C. 2)
     * @param timestamp - milliseconds since 1970.01.01,00:00:00.000 UTC
     * @return Julian Day (JD)
     */
    public static double getJulianDay(long timestamp) {
        double daysSince1970 = timestamp * DAY_PER_MILLISECOND;
        return JD_1970_0 + daysSince1970;
    }
        
    /**
     * Convert UTC time to Modified Julian Day (MJD)
     * <br/> Days since 1858.11.17,00:00:00.000 (UT)
     * <br/> which is B.C. 4713 (because there is no 0-th year, 1 = A.C. 1, 0 = B.C. 1, -1 = B.C. 2)
     * @param timestamp - milliseconds since 1970.01.01,00:00:00.000 UTC
     * @return Modified Julian Day (MJD)
     */
    public static double getJulianDayModified(long timestamp) {
        return getJulianDay(timestamp) - JD_1858_11_17_0;
    }
    
    /**
     * Get the days since 2000.01.01 noon
     * <br/> Days since 2000.01.01, 12:00:00.000 UTC
     * <br/> which is B.C. 4713 (because there is no 0-th year, 1 = A.C. 1, 0 = B.C. 1, -1 = B.C. 2)
     * @param timestamp - milliseconds since 1970.01.01,00:00:00.000 UTC
     * @return days
     */
    public static double getJulianDay2000(long timestamp) {
        return getJulianDay(timestamp) - JD_2000;
    }
    
    /**
     * Calculate the Julian Day (JD) of the beginning of an year (year.01.01,00:00:00.000 UTC)
     * @see Meeus, page 62
     * @param year
     * @return Julian Day (JD)
     */
    public static double getJulianDayAtYearBeginning(int year) {
        int Y = year - 1; // previous year
        if(Y < 1582) { // before Gregorian reform
            return JD_1_0 + Math.floor(DAYS_PER_JULIAN_YEAR * Y); // Floor is needed, year can be negative, where (int) cast not sufficient.
        } else {
            int leaps_corr = - Y / 100 + Y / 400;
            int days = (int)(DAYS_PER_JULIAN_YEAR * Y) + leaps_corr + - 10 + 12;
            // -10: because 1582.10.04 was followed by 1582.10.15 (so 10 days was left).
            // +12: because before 1582, leap years were only the 4*k numbers.
            //      so dont need to add the (-1582/100 + 1582/400) = -12 correction to those years.
            // Also not needed the floor() function here, because in this case year is positive.
            return JD_1_0 + days;
        }
    }
    
    public static String formatTimeStamp(long timeStamp) {
        GregorianCalendar calendar = new GregorianCalendar(new SimpleTimeZone(0, "UTC"));
        calendar.setTimeInMillis(timeStamp);
        int year   = calendar.get(Calendar.YEAR);
        int month  = calendar.get(Calendar.MONTH) + 1;
        int day    = calendar.get(Calendar.DAY_OF_MONTH);
        int hour   = calendar.get(Calendar.HOUR);
        int min    = calendar.get(Calendar.MINUTE);
        int sec    = calendar.get(Calendar.SECOND);
        int ms     = calendar.get(Calendar.MILLISECOND);
        return year + "." + month + "." + day + "," + hour + ":" + min + ":" + sec + "." + ms + "(UTC)";
    }
    
    ///////////
    // TESTS //
    ///////////
    
    public static void test() {
        
        // Unix-epoch test
        Tests.assertTrue(JD_1970_0 == getJulianDay(0));
        
        GregorianCalendar gc = new GregorianCalendar(new SimpleTimeZone(0, "UTC"));
        gc.set(Calendar.SECOND, 0);
        gc.set(Calendar.MILLISECOND, 0);
        
        // Meeus examples, page 62.
        //      year           month    day  hr  min
        gc.set(+2000, Calendar.JANUARY,   1, 12,  0); Tests.assertTrue(getJulianDay(gc.getTimeInMillis()) == 2451545.0);
        gc.set(+1999, Calendar.JANUARY,   1,  0,  0); Tests.assertTrue(getJulianDay(gc.getTimeInMillis()) == 2451179.5);
        gc.set(+1987, Calendar.JANUARY,  27,  0,  0); Tests.assertTrue(getJulianDay(gc.getTimeInMillis()) == 2446822.5);
        gc.set(+1987, Calendar.JUNE,     19, 12,  0); Tests.assertTrue(getJulianDay(gc.getTimeInMillis()) == 2446966.0);
        gc.set(+1988, Calendar.JANUARY,  27,  0,  0); Tests.assertTrue(getJulianDay(gc.getTimeInMillis()) == 2447187.5);
        gc.set(+1988, Calendar.JUNE,     19, 12,  0); Tests.assertTrue(getJulianDay(gc.getTimeInMillis()) == 2447332.0);
        gc.set(+1900, Calendar.JANUARY,   1,  0,  0); Tests.assertTrue(getJulianDay(gc.getTimeInMillis()) == 2415020.5);
        gc.set(+1600, Calendar.JANUARY,   1,  0,  0); Tests.assertTrue(getJulianDay(gc.getTimeInMillis()) == 2305447.5);
        gc.set(+1600, Calendar.DECEMBER, 31,  0,  0); Tests.assertTrue(getJulianDay(gc.getTimeInMillis()) == 2305812.5);
        gc.set( +837, Calendar.APRIL,    10,  8,  0); Tests.assertTrue(getJulianDay(gc.getTimeInMillis()) == 2026871.5 + 1.0/3);
        gc.set( -123, Calendar.DECEMBER, 31,  0,  0); Tests.assertTrue(getJulianDay(gc.getTimeInMillis()) == 1676496.5);
        gc.set( -122, Calendar.JANUARY,   1,  0,  0); Tests.assertTrue(getJulianDay(gc.getTimeInMillis()) == 1676497.5);
        gc.set(-1000, Calendar.JULY,     12, 12,  0); Tests.assertTrue(getJulianDay(gc.getTimeInMillis()) == 1356001.0);
        gc.set(-1000, Calendar.FEBRUARY, 29,  0,  0); Tests.assertTrue(getJulianDay(gc.getTimeInMillis()) == 1355866.5);
        gc.set(-1001, Calendar.AUGUST,   17, 21, 36); Tests.assertTrue(getJulianDay(gc.getTimeInMillis()) == 1355671.4);
        gc.set(-4712, Calendar.JANUARY,   1, 12,  0); Tests.assertTrue(getJulianDay(gc.getTimeInMillis()) ==       0.0);
        
        // Check constants
        gc.set(    1, Calendar.JANUARY,   1,  0,  0);
        gc.set(    1, Calendar.JANUARY,   1,  0,  0); Tests.assertTrue(getJulianDay(gc.getTimeInMillis()) == JD_1_0);
        gc.set( 1858, Calendar.NOVEMBER, 17,  0,  0); Tests.assertTrue(getJulianDay(gc.getTimeInMillis()) == JD_1858_11_17_0);
        gc.set( 1900, Calendar.JANUARY,   1,  0,  0); Tests.assertTrue(getJulianDay(gc.getTimeInMillis()) == JD_1900_0);
        gc.set( 1970, Calendar.JANUARY,   1,  0,  0); Tests.assertTrue(getJulianDay(gc.getTimeInMillis()) == JD_1970_0);
        gc.set( 2000, Calendar.JANUARY,   1, 12,  0); Tests.assertTrue(getJulianDay(gc.getTimeInMillis()) == JD_2000);
        
        
        
        // Test getJulianDay2000()
        gc.set(2000, Calendar.JANUARY, 1, 12, 0);
        double jd2000 = getJulianDay2000(gc.getTimeInMillis());
        Tests.assertTrue(jd2000 == 0);
        
        // Test getJulianDayFromYear()
        for(int year = -401; year < 2100; year++) {
            gc.set(year, Calendar.JANUARY, 1, 0, 0);
            double jdYear1 = getJulianDay(gc.getTimeInMillis());
            double jdYear2 = getJulianDayAtYearBeginning(year);
            Tests.assertTrue(jdYear1 == jdYear2);
        }
        
    }
    
}
