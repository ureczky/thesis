package hu.ureczky.utils.tests;

import android.util.Log;

import hu.ureczky.utils.BuildConfig;
import hu.ureczky.utils.TimeUtils;

public class Tests {
    
    private static final String TAG = "Tests";
    
    public static void test() {
        if(BuildConfig.DEBUG) {
            Log.i(TAG, "Launching tests...");
            
            long t0 = System.currentTimeMillis();
            
//            TimeUtils.test();
            
//            boolean success = true;
//            success &= hu.ureczky.utils.astro.geomagneticfield.wmm.Test.testAndroid2010();
//            success &= hu.ureczky.utils.astro.geomagneticfield.wmm.Test.test2010();
//            success &= hu.ureczky.utils.astro.geomagneticfield.wmm.Test.test2015();
//            Log.e(TAG, "WMM:" + success);
            
            long t1 = System.currentTimeMillis();
            long dt_sec = (t1-t0) / 1000; 
            
            Log.e(TAG, "Finished tests in " + dt_sec + " seconds");
        }
    }
    
    public static void assertTrue(boolean bool) {
        if(!bool) {
            try {
                throw new Exception("assertTrue");
            } catch(Exception e) {
                e.printStackTrace();
            }
        }
    }
    
}
