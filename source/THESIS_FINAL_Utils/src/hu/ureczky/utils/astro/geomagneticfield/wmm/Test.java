package hu.ureczky.utils.astro.geomagneticfield.wmm;

import android.util.Log;

import hu.ureczky.utils.TimeUtils;
import hu.ureczky.utils.astro.Astronomy;
import hu.ureczky.utils.astro.geomagneticfield.GeomagneticField;

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.SimpleTimeZone;

public class Test {
    /** Run tests from http://www.ngdc.noaa.gov/geomag/WMM/data/WMM2015/WMM2015testvalues.pdf */
    public static boolean test2015() {

        String TAG = "WMM_2015_TEST";

        GregorianCalendar gc = new GregorianCalendar(new SimpleTimeZone(0, "UTC"));
        
        WMM wmm = new WMM_2015();
        for(float[] testcase : WMM_2015.TESTCASES) {
            
            // input
            float year = testcase[0];
            float alt  = testcase[1] * 1000;
            float lat  = testcase[2];
            float lon  = testcase[3];
            
            // expected output
            float X    = testcase[4];
            float Y    = testcase[5];
            float Z    = testcase[6];
            float H    = testcase[7];
            float F    = testcase[8];
            float I    = testcase[9];
            float D    = testcase[10];
            
            // calculate
            long t = getTimeMillisFromYear(year);
            GeomagneticField gmf = new GeomagneticField_WMM(wmm, t);
            gmf.setParameters(lat, lon, alt);
            
            // calculated output
            float X_ = gmf.getX();
            float Y_ = gmf.getY();
            float Z_ = gmf.getZ();
            float H_ = gmf.getHorizontalStrength();
            float F_ = gmf.getFieldStrength();
            float I_ = gmf.getInclination();
            float D_ = gmf.getDeclination();
            
            // compare
            gc.setTimeInMillis(t);
            Log.v(TAG, "t: " + gc.get(Calendar.YEAR) + "." + (gc.get(Calendar.MONTH)+1) + "." + gc.get(Calendar.DAY_OF_MONTH));
            Log.v(TAG, "X: " + X_ + " (diff: " + (X_-X) + ")");
            Log.v(TAG, "Y: " + Y_ + " (diff: " + (Y_-Y) + ")");
            Log.v(TAG, "Z: " + Z_ + " (diff: " + (Z_-Z) + ")");
            Log.v(TAG, "H: " + H_ + " (diff: " + (H_-H) + ")");
            Log.v(TAG, "F: " + F_ + " (diff: " + (F_-F) + ")");
            Log.v(TAG, "I: " + I_ + " (diff: " + (I_-I) + ")");
            Log.v(TAG, "D: " + D_ + " (diff: " + (D_-D) + ")");
            if(!(equals(X,X_,10) && equals(Y,Y_,10) && equals(Z,Z_,10) && equals(H,H_,10) && equals(F,F_,10) && equals(I,I_,0.1f) && equals(D,D_,0.1f))) {
                Log.e(TAG, "failed");
                return false;
            }   
        }
        
        // calculate
        long t = getTimeMillisFromYear(2015);
        GeomagneticField gmf = new GeomagneticField_WMM(wmm, t);
        gmf.setParameters((float)Astronomy.LAT_BUD_DEG, (float)Astronomy.LON_BUD_DEG, 0);
        Log.d(TAG, "BUD:" + gmf.getDeclination());
        
        Log.d(TAG, "passed");
        return true;
    }
    
    /** Run tests from http://www.ngdc.noaa.gov/geomag/WMM/data/WMM2010/WMM2010testvalues.pdf */
    public static boolean test2010() {

        String TAG = "WMM_2010_TEST";

        GregorianCalendar gc = new GregorianCalendar(new SimpleTimeZone(0, "UTC"));
        
        WMM wmm = new WMM_2010();
        for(float[] testcase : WMM_2010.TESTCASES) {
            
            // input
            float year = testcase[0];
            float alt  = testcase[1] * 1000;
            float lat  = testcase[2];
            float lon  = testcase[3];
            
            // expected output
            float X    = testcase[4];
            float Y    = testcase[5];
            float Z    = testcase[6];
            float H    = testcase[7];
            float F    = testcase[8];
            float I    = testcase[9];
            float D    = testcase[10];
            
            // calculate
            long t = getTimeMillisFromYear(year);
            GeomagneticField gmf = new GeomagneticField_WMM(wmm, t);
            gmf.setParameters(lat, lon, alt);
            
            // calculated output
            float X_ = gmf.getX();
            float Y_ = gmf.getY();
            float Z_ = gmf.getZ();
            float H_ = gmf.getHorizontalStrength();
            float F_ = gmf.getFieldStrength();
            float I_ = gmf.getInclination();
            float D_ = gmf.getDeclination();
            
            // compare
            gc.setTimeInMillis(t);
            Log.v(TAG, "t: " + gc.get(Calendar.YEAR) + "." + (gc.get(Calendar.MONTH)+1) + "." + gc.get(Calendar.DAY_OF_MONTH));
            Log.v(TAG, "X: " + X_ + " (diff: " + (float)((X_-X)/1) + ")");
            Log.v(TAG, "Y: " + Y_ + " (diff: " + (float)((Y_-Y)/1) + ")");
            Log.v(TAG, "Z: " + Z_ + " (diff: " + (float)((Z_-Z)/1) + ")");
            Log.v(TAG, "H: " + H_ + " (diff: " + (float)((H_-H)/1) + ")");
            Log.v(TAG, "F: " + F_ + " (diff: " + (float)((F_-F)/1) + ")");
            Log.v(TAG, "I: " + I_ + " (diff: " + (float)((I_-I)/1) + ")");
            Log.v(TAG, "D: " + D_ + " (diff: " + (float)((D_-D)/1) + ")");
            if(!(equals(X,X_,10) && equals(Y,Y_,10) && equals(Z,Z_,10) && equals(H,H_,10) && equals(F,F_,10) && equals(I,I_,0.1f) && equals(D,D_,0.1f))) {
                Log.e(TAG, "failed");
                return false;
            }   
        }
        
        // calculate
        long t = getTimeMillisFromYear(2015);
        GeomagneticField gmf = new GeomagneticField_WMM(wmm, t);
        gmf.setParameters((float)Astronomy.LAT_BUD_DEG, (float)Astronomy.LON_BUD_DEG, 0);
        Log.d(TAG, "BUD:" + gmf.getDeclination());
        
        Log.d(TAG, "passed");
        return true;
    }
    
    /** Compare width built-in solution */
    public static boolean testAndroid2010() {

        String TAG = "WMM_2010_ANDROID";

        GregorianCalendar gc = new GregorianCalendar(new SimpleTimeZone(0, "UTC"));
        
        WMM wmm = new WMM_2010();
        for(float[] testcase : WMM_2010.TESTCASES) {
            
            // input
            float year = testcase[0];
            float alt  = testcase[1] * 1000;
            float lat  = testcase[2];
            float lon  = testcase[3];
            
            long t = getTimeMillisFromYear(year);
            
            // expected output
            android.hardware.GeomagneticField gmf_ref = new android.hardware.GeomagneticField(lat, lon, alt, t);
            float X = gmf_ref.getX();
            float Y = gmf_ref.getY();
            float Z = gmf_ref.getZ();
            float H = gmf_ref.getHorizontalStrength();
            float F = gmf_ref.getFieldStrength();
            float I = gmf_ref.getInclination();
            float D = gmf_ref.getDeclination();            
            
            // calculate
            
            GeomagneticField gmf = new GeomagneticField_WMM(wmm, t);
            gmf.setParameters(lat, lon, alt);
            
            // calculated output
            float X_ = gmf.getX();
            float Y_ = gmf.getY();
            float Z_ = gmf.getZ();
            float H_ = gmf.getHorizontalStrength();
            float F_ = gmf.getFieldStrength();
            float I_ = gmf.getInclination();
            float D_ = gmf.getDeclination();
            
            // compare
            gc.setTimeInMillis(t);
            Log.v(TAG, "t: " + gc.get(Calendar.YEAR) + "." + (gc.get(Calendar.MONTH)+1) + "." + gc.get(Calendar.DAY_OF_MONTH));
            Log.v(TAG, "X: " + X_ + " (diff: " + (float)((X_-X)/X) + "%)");
            Log.v(TAG, "Y: " + Y_ + " (diff: " + (float)((Y_-Y)/Y) + "%)");
            Log.v(TAG, "Z: " + Z_ + " (diff: " + (float)((Z_-Z)/Z) + "%)");
            Log.v(TAG, "H: " + H_ + " (diff: " + (float)((H_-H)/H) + "%)");
            Log.v(TAG, "F: " + F_ + " (diff: " + (float)((F_-F)/F) + "%)");
            Log.v(TAG, "I: " + I_ + " (diff: " + (float)((I_-I)/I) + "%)");
            Log.v(TAG, "D: " + D_ + " (diff: " + (float)((D_-D)/D) + "%)");
            if(!(equals(X,X_,10) && equals(Y,Y_,10) && equals(Z,Z_,10) && equals(H,H_,10) && equals(F,F_,10) && equals(I,I_,0.1f) && equals(D,D_,0.1f))) {
                Log.e(TAG, "failed");
                return false;
            }   
        }
        
        // calculate
        long t = getTimeMillisFromYear(2015);
        GeomagneticField gmf = new GeomagneticField_WMM(wmm, t);
        gmf.setParameters((float)Astronomy.LAT_BUD_DEG, (float)Astronomy.LON_BUD_DEG, 0);
        Log.d(TAG, "BUD:" + gmf.getDeclination());
        

        Log.d(TAG, "passed");
        return true;
    }
    
    private static long getTimeMillisFromYear(float year) {
        GregorianCalendar gc = new GregorianCalendar(new SimpleTimeZone(0, "UTC"));
        gc.set((int)year, Calendar.JANUARY, 1, 0, 0, 0);        
        return gc.getTimeInMillis()
                + (long)((year - (int)year) * TimeUtils.DAYS_PER_JULIAN_YEAR * TimeUtils.MILLISECONDS_PER_DAY);
    }
    
    private static boolean equals(float a, float b, float err) {
        double diff = Math.abs(a-b); 
        return diff < err;
    }
}
