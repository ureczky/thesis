package hu.ureczky.celebrations;

import android.util.JsonWriter;
import android.util.Log;

import hu.ureczky.utils.TimeUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.List;

// Requires API 11 (JSON-writer/reader)
public class State {
    
    private static final String TAG = "State";
    
    /////////////// 
    // SINGLETON //
    ///////////////
    
    private static State mInstance;
    
    public static State getInstance() {
        if(mInstance == null) {
            mInstance = new State();
        }
        return mInstance;
    }
    
    public DebugEnabler mDebugEnabler = new DebugEnabler();
    
    // Do not instantiate (private)
    private State() {
        mOrientation    = new float[3];
        mRotationVector = new float[4];
        mZoomPercent    = 100;
        mTarget         = Target.SUN;
        mTaskType       = TaskType.POSITION;
    }
    
    // RESULTS
    public class Result {
        public TaskType mTaskType;
        public Target mTargetType;
        public long mTimeStamp;
    }
    public class PositionResult extends Result {
        public double mAzimuth;
        public double mElevation;
        public float mMagnIntesity;
        public float mMagnInclination;
        public float mPressurePa;
        public float mTemperatureC;
    }
    public class TimeResult extends Result {
        public double mLon, mLat;
        public float mPressurePa;
        public float mTemperatureC;
    }
    public List<Result> mResults;
    
    public void setSensorValues(SensorWatcher sensorWatcher) {
        mTimeStamp       = System.currentTimeMillis();
        mOrientation     = sensorWatcher.getOrientation();
        mPressure_Pa     = sensorWatcher.mPressure_Pa;
        mTemperature_C   = sensorWatcher.mTemperature_C;
        mMagnIntensity   = sensorWatcher.mMagnIntensity;
        mMagnInclination = sensorWatcher.mMagnInclination;
    }
    
    public Result getResult(SensorWatcher sensorWatcher) {
        setSensorValues(sensorWatcher);
        switch(mTaskType) {
            case POSITION: {
                PositionResult result = new PositionResult();
                result.mTaskType        = mTaskType;
                result.mTargetType      = mTarget;
                result.mTimeStamp       = mTimeStamp;
                result.mAzimuth         = mOrientation[0];
                result.mElevation       = mOrientation[1];
                result.mPressurePa      = mPressure_Pa;
                result.mTemperatureC    = mTemperature_C;
                result.mMagnIntesity    = mMagnIntensity;
                result.mMagnInclination = mMagnInclination;
                return result;
            }
            case TIME: {
                TimeResult result = new TimeResult();
                result.mTaskType     = mTaskType;
                result.mTargetType   = mTarget;
                result.mTimeStamp    = mTimeStamp;
                result.mLon          = mLongitude;
                result.mLat          = mLatitude;
                result.mPressurePa   = mPressure_Pa;
                result.mTemperatureC = mTemperature_C;
                return result;
            }
            default:
                Log.e(TAG, "Unknown TaskType");
                return null;
        }
    }
    
    public void snapshot(SensorWatcher sensorWatcher) {
        Result result = getResult(sensorWatcher);
        mResults.add(result);
    }
    
    public Result getLastResult() {
        int lastIdx = mResults.size() - 1;
        return (lastIdx > 0) ? mResults.get(lastIdx) : null;
    }
    
    // JSON keys
    private static String ID_TIMESTAMP      = "timestamp";
    private static String ID_DATETIME       = "datetime";
    private static String ID_TARGET         = "target";
    private static String ID_ORIENTATION    = "orientation";
    private static String ID_AZIMUTH        = "azimuth";
    private static String ID_ELEVATION      = "elevation";
    private static String ID_MAGNETIC_FIELD = "magnetic_field";
    private static String ID_MAGN_INTEN     = "magnetic_intensity";
    private static String ID_MAGN_INCLIN    = "magnetic_inclination";
    private static String ID_ROLL           = "roll";
    private static String ID_PRESSURE       = "pressure";
    private static String ID_TEMPERAURE     = "temperature";
    private static String ID_CAMERA         = "camera";
    private static String ID_EXPOSURE       = "exposure";
    private static String ID_ZOOMPERCENT    = "zoompercent";
    
    // Device parameters
    public String mPhone;   // Device's name
    public String mVersion; // Android version
    
    public long mTimeStamp; // UTC timestamp
    public Target mTarget;
    public TaskType mTaskType;
    
    // Sensors
    public float[] mOrientation; // Azimuth, elevation, roll
    public float[] mRotationVector;
    public float mPressure_Pa;
    public float mTemperature_C;
    private float mMagnIntensity;
    private float mMagnInclination;
    // Result
    public float mLongitude;
    public float mLatitude;
    
    // Camera parameters
    public int mZoomPercent;
    public int mZoomIdx;
    public int mExposure;
    public float mFovXDeg;
    public float mFovYDeg;
    public float mFovXRad;
    public float mFovYRad;
    public float mFocalLength_mm; // Original focal length of the camera's sensor in millimeters
    public float mFocalLength_px; // Focal length in pixels according to current preview size and zoom percent
    public float mSensorAspectRatio;
    public int mPreviewWidth;
    public int mPreviewHeight;
    public int mResX; // X resolution (width) of the image
    public int mResY; // Y resolution (height) of the image
        
    // Only for debug
    public String mDescr;    // User description
    public String mDateTime; // User readable time (YYYY.MM.DD,HH:MM(UTC))
    public double mLong;     // Longitude
    public double mLat;      // Latitude
    // public String mPlace="Budapest/Home/Work"
        
    public boolean switchTarget() {
        switch(mTarget) {
            case SUN:  mTarget = Target.MOON; break;
            case MOON: mTarget = Target.SUN;  break;
            default: Log.e(TAG, "Unknown Target Type");
        }
        return mDebugEnabler.click();
    }
            
    public void write(File outFile) throws IOException {
        FileOutputStream metadataStream = new FileOutputStream(outFile);
            JsonWriter writer = new JsonWriter(new OutputStreamWriter(metadataStream, "UTF-8"));
                writer.setIndent("  ");
                writer.beginObject();
                    writer.name(ID_TIMESTAMP  ).value(mTimeStamp);
                    writer.name(ID_DATETIME   ).value(TimeUtils.formatTimeStamp(mTimeStamp));
                    writer.name(ID_ORIENTATION);
                    writer.beginObject();
                        writer.name(ID_AZIMUTH  ).value(mOrientation[0]);
                        writer.name(ID_ELEVATION).value(mOrientation[1]);
                        writer.name(ID_ROLL     ).value(mOrientation[2]);
                    writer.endObject();
                    writer.name(ID_MAGNETIC_FIELD);
                    writer.beginObject();
                        writer.name(ID_MAGN_INTEN).value(mMagnIntensity);
                        writer.name(ID_MAGN_INCLIN).value(mMagnInclination);
                    writer.endObject();
                    writer.name(ID_PRESSURE).value(mPressure_Pa);
                    writer.name(ID_TEMPERAURE).value(mTemperature_C);
                    writer.name(ID_CAMERA);
                    writer.beginObject();
                        writer.name(ID_EXPOSURE).value(mExposure);
                        writer.name(ID_ZOOMPERCENT).value(mZoomPercent);
                    writer.endObject();
                    writer.name(ID_TARGET).value(mTarget.toString());
                writer.endObject();
            writer.close();
        metadataStream.close();
    }   

    public void writeXXXArray(JsonWriter writer) throws IOException {
        writer.beginArray();
        for (int i=0; i<1; i++) {
            writer.name("x"+i).value(mOrientation[i]);
        }
        writer.endArray();
    }
      
}
