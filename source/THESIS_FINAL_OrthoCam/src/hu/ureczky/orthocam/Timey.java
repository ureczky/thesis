package hu.ureczky.orthocam;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.SystemClock;

//TODO rename
public class Timey {
    
    private final static boolean NANO_CAPABLE = (android.os.Build.VERSION.SDK_INT >= 17);
    private final static long MILLISECOND = NANO_CAPABLE ? 1000*1000 : 1;
    final static long SECOND = 1000 * MILLISECOND;
    private boolean animationEnabled = Settings.DEFAULT_ANIMATION_ENABLED;
    private long transformationStarted = 0;
    private float transformationPhase = 0;
    private float transformationPhase0 = 0;
    private long transformationLength = 500 * MILLISECOND;
    private int transformationDirection = -1;
    
    public static int DIRECTION_FORWARD = 1;
    public static int DIRECTION_BACKWARD = -1;
    
    public Timey(Context context){
        
    }
    
    public void init(SharedPreferences mPreferences){
        animationEnabled = mPreferences.getBoolean(Settings.KEY_ANIMATION_ENABLED, Settings.DEFAULT_ANIMATION_ENABLED);
    }
    
    public void reverse(){
        transformationDirection *= -1;
        transformationStarted = getTime();
        transformationPhase0 = transformationPhase;
    }
    
    /**
     * Get the current time as precisely as it can.
     * @return current time in milliseconds or nanoseconds, depend on the system's capability.
     */
    @SuppressLint("NewApi")
    static long getTime(){
        return NANO_CAPABLE 
            ? SystemClock.elapsedRealtimeNanos() // Nanoseconds
            : SystemClock.elapsedRealtime();     // Milliseconds
    }

    public float getTransformationPhase() {
        if(animationEnabled && transformationPhase != (transformationDirection+1)/2 ){
            //  With animation
            long currTime = getTime(); 
            long elapsedTime = currTime - transformationStarted;
            float deltaPhase = transformationDirection * elapsedTime / (float) transformationLength;
            transformationPhase = Math.min(1, Math.max(0, transformationPhase0 + deltaPhase));
            
        } else {
            // Without animation
            transformationPhase = (transformationDirection==1) ? 1f : 0f;
        }
        return transformationPhase;
    }
}
