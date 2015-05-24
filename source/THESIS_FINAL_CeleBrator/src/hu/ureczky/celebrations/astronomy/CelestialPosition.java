package hu.ureczky.celebrations.astronomy;

import android.util.Log;

import hu.ureczky.celebrations.Target;
import hu.ureczky.celebrations.astronomy.implementations.SunMoonCalculator;
import hu.ureczky.celebrations.astronomy.implementations.SunRelativePosition;
import hu.ureczky.utils.astro.geomagneticfield.GeomagneticField;
import hu.ureczky.utils.astro.geomagneticfield.GeomagneticFieldFactory;

public class CelestialPosition {

    private static final String TAG = "CelestialPosition";
    private long mTimeStamp;
    private double mLatitude;
    private double mLongitude;
    
    public double mAzimuthDegTrue;
    public double mAzimuthDegMagn;
    public double mElevationDeg;
    
    public CelestialPosition(Target target, long timeStamp, double latitudeDeg, double longitudeDeg)
    {
        mTimeStamp = timeStamp;
        mLatitude  = latitudeDeg;
        mLongitude = longitudeDeg;
        
        switch(target) {
            case SUN:  calculateSun();  break;
            case MOON: calculateMoon(); break;
            default: Log.e(TAG, "Unknown target type");
        }
        
        magneticCorrection(); 
    }
    
    private void calculateSun() {
//        SunMoonCalculator smc = calculateSunMoon();
//        if(smc != null) {
//            mAzimuthDegTrue = Math.toDegrees(smc.sunAz);
//            mElevationDeg   = Math.toDegrees(smc.sunEl);
//        }
        
        SunRelativePosition srp = new SunRelativePosition();
        srp.setDate(mTimeStamp);
        srp.setCoordinate(mLongitude, mLatitude);
        mAzimuthDegTrue = srp.getAzimuth();
        mElevationDeg   = srp.getElevation();
        
    }
    
    private void calculateMoon() {
        SunMoonCalculator smc = calculateSunMoon();
        if(smc != null) {
            mAzimuthDegTrue = Math.toDegrees(smc.moonAz);
            mElevationDeg   = Math.toDegrees(smc.moonEl);
        }
    }
    
    private SunMoonCalculator calculateSunMoon() {
        try {
            SunMoonCalculator smc = new SunMoonCalculator(
                    mTimeStamp,
                    Math.toRadians(mLongitude),
                    Math.toRadians(mLatitude));
            smc.calcSunAndMoon();
            return smc;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
    
    private void magneticCorrection() {
        // Magnetic declination correction
        GeomagneticField gmf = GeomagneticFieldFactory.create(mTimeStamp);
        gmf.setParameters(
                (float) mLatitude,
                (float) mLongitude,
                0 // altitude
        );                                
        float declinationDeg = gmf.getDeclination();
        
        mAzimuthDegMagn = mAzimuthDegTrue - declinationDeg;
        mAzimuthDegMagn = (mAzimuthDegMagn +360) % 360; //Convert to 0..360 
    }
}
