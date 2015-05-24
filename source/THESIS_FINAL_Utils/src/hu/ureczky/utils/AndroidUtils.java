package hu.ureczky.utils;

import android.app.Activity;
import android.view.Surface;
import android.view.Window;
import android.view.WindowManager;

public class AndroidUtils {
    
    // Remove title bar and notification bar 
    public static void setFullScreen(Activity activity) {
        
        //Remove title bar
        activity.requestWindowFeature(Window.FEATURE_NO_TITLE);

        //Remove notification bar
        activity.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
    }
    
    public static int getDegreesFromSurfaceRotation(int rotation){
        int degrees = 0;
        switch (rotation) {
            case Surface.ROTATION_0:   degrees = 0;   break;
            case Surface.ROTATION_90:  degrees = 90;  break;
            case Surface.ROTATION_180: degrees = 180; break;
            case Surface.ROTATION_270: degrees = 270; break;
        }
        return degrees;
    }
}
