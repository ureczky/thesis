package hu.ureczky.celebrations;

import android.location.Location;
import android.util.Log;

import hu.ureczky.celebrations.State.PositionResult;
import hu.ureczky.celebrations.State.Result;
import hu.ureczky.celebrations.State.TimeResult;
import hu.ureczky.celebrations.astronomy.CelestialPosition;
import hu.ureczky.utils.astro.geomagneticfield.GeomagneticField;
import hu.ureczky.utils.astro.geomagneticfield.GeomagneticFieldFactory;

public class Algorithm {
    
    private static final String TAG = "Algorithm";
    
    private static float mAzimuthDegMagn;
    private static float mElevationDeg;
    private static float mMagnInclination;
    private static float mMagnIntensity;
    private static Target mTarget;
    private static long mTimeStamp;
    private static GeomagneticField mGMF;
    
    // Maximum differences in sensor values
    private static final float DIFF_MAGNETIC_INTENSITY   = 75-15;
    private static final float DIFF_MAGNETIC_INCLINATION = 180;
    private static final float DIFF_ELEVATION = 180;
    private static final float DIFF_AZIMUTH = 180;
    
    public static Location calculate(Result input_raw) {
        
        //TODO
        switch(input_raw.mTaskType) {
            case POSITION: {
                PositionResult input = (PositionResult) input_raw;
                return calculatePosition(input);
            }
            case TIME: {
                TimeResult input = (TimeResult) input_raw;
                //TODO return calculateTime();
                break;
            }
            case COMPASS: {
                //CompassResult input = (CompassResult) input_raw;
              //TODO return calculateCompass();
                break;
            }
            default:
                Log.e(TAG, "Unknown Result Type");
        }
        return null;
    }
    
    private static Location calculatePosition(PositionResult input) {
        mTimeStamp = input.mTimeStamp;
        mTarget = input.mTargetType;
        mGMF = GeomagneticFieldFactory.create(mTimeStamp);
        
        // Measured angles
        mAzimuthDegMagn  = (float) input.mAzimuth;
        mElevationDeg    = (float) input.mElevation;
        mMagnInclination = input.mMagnInclination;
        mMagnIntensity   = input.mMagnIntesity;
        
        double finalLat = 0, finalLon = 0;     // currently found optimal position
        double optLat = 0, optLon = 0;         // optimal position of the previous iteration
        double lat, lon;                       // currently examined position
        //double azimuthDegMagn, elevationDeg;   // currently examined position's calculated azimuth, elevation
        //double errAzimuthDeg, errElevationDeg; // difference of the azimuth and elevation to the currently examined positions calculated azimuth and elevation
        double err;                            // error points calculated from errAzimuth, errElevation
        double errMin = Double.MAX_VALUE;
        
        long t0 = System.currentTimeMillis();
        
        for(double d = 20; d > 0.1; d /= 4) { // d: distance of the points (resolution)
            for(int i = -9; i < 9; i++) {
                lon = optLon + i * d;
                for(int j = -4; j <= 4; j++) {
                    lat = optLat + j * d;
                    if(lat > 80 || lat < -80)  // exclude poles and the near area
                        continue;
                    
                    // Calculate
                    err = errorCalc(lat, lon);
                    
                    if(err < errMin) {
                        errMin  = err;
                        finalLat = lat;
                        finalLon = lon;
                    }
                }
            }
            optLat = finalLat;
            optLon = finalLon;
        }
        
        // Calculation time
        long t1 = System.currentTimeMillis();
        Log.d(TAG, "Calculation time:" + (t1-t0) + "ms");
        
        // Return location
        Location loc = new Location("");
        loc.setLatitude(optLat);
        loc.setLongitude(optLon);
        // float e = errorCalc(optLat, optLon); //DEGUG
        return loc;
    }
    
    private static float errorCalc(double lat, double lon) {
        CelestialPosition cp = new CelestialPosition(mTarget, mTimeStamp, lat, lon);
        float azimuthDegMagn = (float) cp.mAzimuthDegMagn;
        float elevationDeg   = (float) cp.mElevationDeg;
        
        mGMF.setParameters((float)lat, (float)lon, 0 /*TODO*/);
        float magneticIntensity = mGMF.getFieldStrength() / 1000f;
        float magneticInclination = mGMF.getInclination();
        
        float errMagnInt   = getSqError(mMagnIntensity,   magneticIntensity,   DIFF_MAGNETIC_INTENSITY);
        float errMagnInc   = getSqError(mMagnInclination, magneticInclination, DIFF_MAGNETIC_INCLINATION);
        float errAzimuth   = getSqError(mAzimuthDegMagn,  azimuthDegMagn,      DIFF_AZIMUTH);
        float errElevation = getSqError(mElevationDeg,    elevationDeg,        DIFF_ELEVATION);
        
        //TODO miket szamoljon
        //float err          = errMagnInt + errMagnInc;
        //float err          = errAzimuth + errElevation;
        float err          = errMagnInt + errMagnInc + errAzimuth + errElevation;
        return err;
    }
    
    private static float getSqError(float a, float b, float maxDiff) {
        float relDiff = (a - b) / maxDiff;
        return relDiff * relDiff;
        
    }
}
