package hu.ureczky.celebrations;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.util.Log;
import android.util.Pair;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import hu.ureczky.celebrations.State.PositionResult;
import hu.ureczky.celebrations.State.Result;
import hu.ureczky.celebrations.astronomy.CelestialPosition;
import hu.ureczky.utils.SensorUtils;
import hu.ureczky.utils.TimeUtils;
import hu.ureczky.utils.astro.Astronomy;
import hu.ureczky.utils.astro.AtmosphericRefraction;
import hu.ureczky.utils.astro.Barometry;
import hu.ureczky.utils.astro.Temperature;
import hu.ureczky.utils.astro.geomagneticfield.GeomagneticField;
import hu.ureczky.utils.astro.geomagneticfield.GeomagneticFieldFactory;

public class SensorWatcher {

    private static final String TAG = "SensorWatcher"; 
    private static final String DEG = "°";
    
    private TextView mInfoText;
    private ARView mArView;
    
    private SensorManager mSensorManager;
    private int mRotation; // Device's rotation
    
    // Orientation
    private float mAzimuthDegMagn;
    private float mElevationDeg; // apparent, where we see
    private float mRollDeg;
    private float mRollRad;
    
    private float[] mRotationMatrix = new float[9];
    private float[] mOrientationValues = new float[3];
    
    // Gravity [m/s^2]
    //private float[] mGravity0    = new float[3]; // x, y, z, original gravity
    private float[] mGravity     = new float[3]; // x, y, z, calculated from rotation vector
    private float[] mGravityUnit = new float[3];
    private float mGravityIntensity;
    
    // Magnetic [uT]
    private float[] mMagn = new float[3]; // x, y, z
    private float[] mMagnUnit = new float[3];
    public float mMagnIntensity;
    public float mMagnInclination;
    
    // Pressure, temperature
    public float mPressure_Pa;
    public float mTemperature_C;
    private static final float DEFAULT_TEMPERATURE_C = (float) AtmosphericRefraction.BASE_TEMP_C;
    private static final float DEFAULT_PRESSURE_PA   = (float) AtmosphericRefraction.BASE_PRESS_PA;
    
    private double azimuthAngle0;
    private double elevationAngle0;
    
    long mLastCalcTime = 0;
    double mLat;
    double mLon;
    
    
    public SensorWatcher(TextView infoText, int rotation) {
        
        mSensorManager = (SensorManager) infoText.getContext().getSystemService(Context.SENSOR_SERVICE);
                
        mInfoText = infoText;
        
        // Initialize the rotation matrix to identity
        mRotationMatrix[0] = 1;
        mRotationMatrix[3] = 1;
        mRotationMatrix[6] = 1;
        
        mTemperature_C = DEFAULT_TEMPERATURE_C;
        mPressure_Pa   = DEFAULT_PRESSURE_PA;
        
        mRotation = rotation;
        onResume();
    }
    
    public void onResume() {
        registerListeners();
    }
    
    public void onPause() {    
        mSensorManager.unregisterListener(sensorEventListener);
    }
    
    private void registerListeners() {
        TaskType t = State.getInstance().mTaskType;
        
        List<Pair<Integer,Integer>> registeredSensors = new ArrayList<Pair<Integer,Integer>>();
        
        if(t == TaskType.COMPASS) {
            registeredSensors.add(new Pair<Integer,Integer>(Sensor.TYPE_GAME_ROTATION_VECTOR , SensorManager.SENSOR_DELAY_UI));
        } else {
            registeredSensors.add(new Pair<Integer,Integer>(Sensor.TYPE_ROTATION_VECTOR      , SensorManager.SENSOR_DELAY_UI));//TODO FASTEST?
            registeredSensors.add(new Pair<Integer,Integer>(Sensor.TYPE_MAGNETIC_FIELD       , SensorManager.SENSOR_DELAY_UI));
            registeredSensors.add(new Pair<Integer,Integer>(Sensor.TYPE_PRESSURE             , SensorManager.SENSOR_DELAY_NORMAL));
            registeredSensors.add(new Pair<Integer,Integer>(Sensor.TYPE_TEMPERATURE          , SensorManager.SENSOR_DELAY_NORMAL));//TODO deprecated from 14
            registeredSensors.add(new Pair<Integer,Integer>(Sensor.TYPE_AMBIENT_TEMPERATURE  , SensorManager.SENSOR_DELAY_NORMAL));
        }
        registeredSensors.add(new Pair<Integer,Integer>(Sensor.TYPE_GRAVITY              , SensorManager.SENSOR_DELAY_UI));
        
        
        for(Pair<Integer,Integer> s: registeredSensors) {
            int sensorType  = s.first;
            int sensorDelay = s.second;
            
            Sensor sensor = mSensorManager.getDefaultSensor(sensorType);
            if(sensor == null) {
                Log.e(TAG, "There is no " + sensorType + " sensor");
            } else {
                mSensorManager.registerListener(sensorEventListener, sensor , sensorDelay);
            }
        }
    }
    
    public void addListener(ARView arView) {
        mArView = arView;
    }    
    
    public float[] getOrientation() {
        return new float[] {mAzimuthDegMagn, mElevationDeg, mRollDeg};
    }
    
    private final SensorEventListener sensorEventListener = new SensorEventListener() {
        
        @Override
        public void onSensorChanged(SensorEvent sensorEvent) {
            
            // Get the values according to Screen's coordinate system
            float[] sensorValues = SensorUtils.getValuesRemapedForScreen(mRotation, sensorEvent);
            
            switch(sensorEvent.sensor.getType()) {
                
                case Sensor.TYPE_ROTATION_VECTOR: {                  
                
                    // Convert the rotation-vector to a 4x4 matrix.
                    SensorManager.getRotationMatrixFromVector(mRotationMatrix, sensorValues);
                    
                    mGravity = SensorUtils.getGravityFromRotationVector(sensorValues);
                    
                    //TODO ezeket törölni
                    calculate();
                    if(mArView!=null) mArView.invalidate();
                    
                    break;
                }
                case Sensor.TYPE_GAME_ROTATION_VECTOR: {
                    // Convert the rotation-vector to a 4x4 matrix.
                    SensorManager.getRotationMatrixFromVector(mRotationMatrix, sensorValues);
                    
                    mGravity = SensorUtils.getGravityFromRotationVector(sensorValues);
                    
                    //TODO ezeket törölni
                    calculate();
                    if(mArView!=null) mArView.invalidate();
                    
                    break;
                }
                case Sensor.TYPE_GRAVITY: {    
                    
                    //mGravity0 = sensorValues;
                    
                    //mGravity[0] = sensorValues[0];
                    //mGravity[1] = sensorValues[1];
                    //mGravity[2] = sensorValues[2];
                    //mRollRad = (float) Math.atan2(mGravity[0], mGravity[1]);
                    //mRollDeg = (float) Math.toDegrees(mRollRad);
                    
                    //double g12 = Math.sqrt(mGravity[0]*mGravity[0]+mGravity[1]*mGravity[1]); 
                    //mElevationDeg = (float) Math.toDegrees(Math.atan2(g12,mGravity[2]))-90f;
                                        
                    break;
                }
                case Sensor.TYPE_MAGNETIC_FIELD: {
                    mMagn = sensorValues;
                    break;
                }
                /*
                case Sensor.TYPE_ACCELEROMETER: {
                    break;
                }
                */
                case Sensor.TYPE_PRESSURE: {
                    float pressure_Pa = 100 * sensorValues[0];
                    if(Barometry.isValid(mPressure_Pa)) {
                        mPressure_Pa = pressure_Pa;
                    }
                    break;
                }
                case Sensor.TYPE_TEMPERATURE:
                case Sensor.TYPE_AMBIENT_TEMPERATURE: {
                    float temp_C = sensorValues[0];
                    if(Temperature.isValid(temp_C)) {
                        mTemperature_C = sensorValues[0];
                    }
                    break;
                }
            }
        }

        public void onAccuracyChanged (Sensor senor, int accuracy) {
            Log.i(TAG, "onAccuracyChanged:" + accuracy);
        }

    };
    
    private static long t_lastrender = 0;
    public String calculate() {
        
        long t_now = System.currentTimeMillis();
        if(t_now < t_lastrender + 30) return ""; // TODO render loop
        t_lastrender = t_now;
        
        /*
        //float[] rotationMatrix2 = new float[9];
        //SensorManager.getRotationMatrixFromVector(rotationMatrix2, sensorValues);
        float[] rotationMatrix2 = getRotationMatrixFromVector(sensorValues);
        //remapForView(rotationMatrix2);
        float[] orientationValues2 = new float[3];
        SensorManager.getOrientation(rotationMatrix2, orientationValues2);
        float azimuthAngle2 = (float)Math.toDegrees(orientationValues2[0]);
        float pitchAngle2   = (float)Math.toDegrees(orientationValues2[1]);
        float rollAngle2    = (float)Math.toDegrees(orientationValues2[2]);
       
        azimuthAngle2 = (azimuthAngle2 + 360) % 360; //Convert from -180..180 to 0..360
        pitchAngle2   *= -1; //Up is positive
    
      */  
        
        float _1 = SensorUtils.toUnit(mGravity, mGravityUnit);
        //mGravityIntensity = (float) SensorUtils.vectorLen(mGravity0); //would return constant 9.81
        
        mRollRad = (float) Math.atan2(mGravity[0], mGravity[1]);
        mRollDeg = (float) Math.toDegrees(mRollRad);
            
        SensorUtils.remapForView(mRotationMatrix);
        
        SensorManager.getOrientation(mRotationMatrix, mOrientationValues);
        
        mAzimuthDegMagn = (float)Math.toDegrees(mOrientationValues[0]);
        mElevationDeg   = (float)Math.toDegrees(mOrientationValues[1]);
        //mRollDeg      = (float)Math.toDegrees(mOrientationValues[2]); //TODO miert nem jo
        
        mAzimuthDegMagn = (mAzimuthDegMagn + 360) % 360; //Convert from -180..180 to 0..360
        mElevationDeg *=-1; //Up is positive
        
        // Desired orientation
        float bpLon = (float) Astronomy.LON_BUD_DEG; 
        float bpLat = (float) Astronomy.LAT_BUD_DEG;
        float lon0 = bpLon;
        float lat0 = bpLat;
            
        long timeStamp = System.currentTimeMillis();
        CelestialPosition cp = new CelestialPosition(State.getInstance().mTarget, timeStamp, lat0, lon0);
        azimuthAngle0   = cp.mAzimuthDegMagn;
        elevationAngle0 = cp.mElevationDeg;
        GeomagneticField gmf = GeomagneticFieldFactory.create(timeStamp);
        gmf.setParameters(lat0, lon0, 0);
        float magnIntensity0   = gmf.getFieldStrength() / 1000;
        float magnInclination0 = gmf.getInclination();
        float magnDeclination0 = gmf.getDeclination();
        
        //Pressure, altitude, temperature
        String pressureStr = "N/A";
        String altitudeStr = "N/A";
        
        if(mPressure_Pa!= DEFAULT_PRESSURE_PA) {
            double altitude_m = Barometry.getAltitudeFromPressure(mPressure_Pa); //TODO 2nd parameter: sea level pressure
            pressureStr = String.format("%+8.2f", Math.round(mPressure_Pa)/100f);
            altitudeStr = String.format("%+8.2f", Math.round(100 * altitude_m)/100f);
        }
        
        //Refraction
        double refraction_deg = 0;
        if(mElevationDeg > 0) {
            AtmosphericRefraction AtmRefr = new AtmosphericRefraction().setApparentElevation(mElevationDeg);
            if(mPressure_Pa   != DEFAULT_PRESSURE_PA)   AtmRefr.setPressure(mPressure_Pa);
            if(mTemperature_C != DEFAULT_TEMPERATURE_C) AtmRefr.setTemperature(mTemperature_C);
            refraction_deg = AtmRefr.getRefraction();
        }
        
        mMagnIntensity = SensorUtils.toUnit(mMagn, mMagnUnit);
        
        mMagnInclination = (float) Math.toDegrees(SensorUtils.angleRad(mMagn, mGravity)) - 90; // gravity is up
        
        String infos_DEBUG = String.format(
                "Azimuth    : %+8.2f ° [%+3.2f]\n"+
                "Elevation  : %+8.2f ° [%+3.2f]\n"+
                "Roll       : %+8.2f °\n"+
                "Pressure   : %s hPa\n"+
                "->Altitude : %s m\n"+
                "Temperature: %+8.2f °C\n"+
                "Refraction : %+8.2f °\n"+
                "Magn. Inten: %+8.2f uT [%+3.2f]\n"+
                "Magn. Incli: %+8.2f °  [%+3.2f]\n"+
                "Magn. Decli:             [%+3.2f]\n"+
              //"Grav. Int  : %+8.2f m/s^2\n"+
                "Zoom: %d%%", //TODO kulon
                Math.round(100 * mAzimuthDegMagn  )/100f,   azimuthAngle0,
                Math.round(100 * mElevationDeg    )/100f, elevationAngle0,
                Math.round(100 * mRollDeg         )/100f,
                pressureStr,
                altitudeStr,
                Math.round(100 * mTemperature_C   )/100f,
                Math.round(100 * refraction_deg   )/100f,
                Math.round(100 * mMagnIntensity   )/100f, magnIntensity0,
                Math.round(100 * mMagnInclination )/100f, magnInclination0,
                magnDeclination0,
              //Math.round(100 * mGravityIntensity)/100f,
                State.getInstance().mZoomPercent
        );
        
        String infos = String.format(
                "Azimuth    : %+8.2f °\n"+
                "Elevation  : %+8.2f °\n"+
                "Roll       : %+8.2f °\n"+
                "-----------------------\n"+
                "Pressure   : %s hPa\n"+
                "->Altitude : %s m\n"+
                "Temperature: %+8.2f °C\n"+
                "Refraction : %+8.2f °\n"+
                "Magn. Inten: %+8.2f uT\n"+
                "Magn. Incli: %+8.2f ° \n"+
                "Zoom: %d%%", //TODO kulon
                Math.round(100 * mAzimuthDegMagn  )/100f,
                Math.round(100 * mElevationDeg    )/100f,
                Math.round(100 * mRollDeg         )/100f,
                pressureStr,
                altitudeStr,
                Math.round(100 * mTemperature_C   )/100f,
                Math.round(100 * refraction_deg   )/100f,
                Math.round(100 * mMagnIntensity   )/100f,
                Math.round(100 * mMagnInclination )/100f,
                State.getInstance().mZoomPercent
        );
        
        // DEBUG calculation
//        long t = System.currentTimeMillis();
//        if(t - mLastCalcTime > 1 * TimeUtils.MILLISECONDS_PER_SECOND) {
//            State state = State.getInstance();
//            //state.setSensorValues(this);
//            Result r = state.getResult(this);
//            //state.saveResult(sensorWatcher);
//            Location optLoc = Algorithm.calculate(r); //TODO csak a last..
//            mLat = optLoc.getLatitude();
//            mLon = optLoc.getLongitude();
//            mLastCalcTime = System.currentTimeMillis();
//            infos += String.format(
//                "-----------------------\n"+
//                "Latitude   : %+8.2f °\n"+
//                "Longitude  : %+8.2f °\n"+,
//                Math.round(100 * mLat )/100f,
//                Math.round(100 * mLon )/100f
//                );
//        }
        
        //Refresh results
        State state = State.getInstance();
        Result r = state.getResult(this);

        String infoStr = state.mDebugEnabler.mDebugMode ? infos_DEBUG : infos;
        mInfoText.setText(infoStr);
        return infoStr;
    }
}
