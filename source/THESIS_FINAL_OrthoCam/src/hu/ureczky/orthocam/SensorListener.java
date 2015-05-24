package hu.ureczky.orthocam;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Matrix;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.preference.PreferenceManager;
import android.view.Display;
import android.view.WindowManager;

import java.util.List;

import hu.ureczky.utils.SensorUtils;
import hu.ureczky.utils.TransformUtils;

public class SensorListener implements SensorEventListener {

    private SensorManager mSensorManager;
    private SharedPreferences mPreferences;
    private int mRotation;
    private float gx=1, gy=1, gz=1;
    private float[] mRotationVector;
    private float[] mSensorValues;
    
    public SensorListener(Context context){
        mSensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        mPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        WindowManager windowManager = ((WindowManager) context.getSystemService(Context.WINDOW_SERVICE));
        Display display = windowManager.getDefaultDisplay();
        mRotation = display.getRotation();
        
    }
    
    public void pause(){
        if(mSensorManager!=null){
            mSensorManager.unregisterListener(this);
        }
    }
    
    public void listen(){
        // Get selected sensor from preferences
        String choosenSensorString = mPreferences.getString(Settings.KEY_SENSOR_MODE, "");
        String[] sensorDescriptor = choosenSensorString.split("#");
        int sensorType = Integer.valueOf(sensorDescriptor[0]);
        String sensorName = sensorDescriptor[1];
        String sensorVendor = sensorDescriptor[2];
        int sensorVersion = Integer.valueOf(sensorDescriptor[3]);
        
        // Find the selected sensor from the available sensors
        
        Sensor choosenSensor = null;
        List<Sensor> sensorsByType = mSensorManager.getSensorList(sensorType);
        for(Sensor sensor: sensorsByType){
            if(sensor.getName().equals(sensorName)
            && sensor.getVendor().equals(sensorVendor)
            && sensor.getVersion()==sensorVersion){
                choosenSensor = sensor;
            }    
        }
        
        // Register the Listener
        mSensorManager.registerListener(this, choosenSensor, SensorManager.SENSOR_DELAY_UI);
    }
    
    public float[] getGravity()
    {
        return new float[]{gx, gy, gz};
    }
    
    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        
        // Get the values according to Screen's coordinate system
        mSensorValues = SensorUtils.getValuesRemapedForScreen(mRotation, sensorEvent);
        
        int sensorType = sensorEvent.sensor.getType();
        switch(sensorType) {
            case Sensor.TYPE_GRAVITY:
            case Sensor.TYPE_ACCELEROMETER: {
                gx = mSensorValues[0];
                gy = mSensorValues[1];
                gz = mSensorValues[2];
                double g = Math.sqrt(gx*gx + gy*gy + gz*gz);
                gx = (float)(gx / g);
                gy = (float)(gy / g);
                gz = (float)(gz / g);
                break;
            }
            case Sensor.TYPE_ROTATION_VECTOR:
            case Sensor.TYPE_GAME_ROTATION_VECTOR:
            case Sensor.TYPE_GEOMAGNETIC_ROTATION_VECTOR: {

                mRotationVector = mSensorValues;
                float[] g = SensorUtils.getGravityFromRotationVector(mSensorValues);
                gx = g[0];
                gy = g[1];
                gz = g[2];
                
                //float[] Rv = sensorEvent.values;
//                float[] rot = new float[9];
//                SensorManager.getRotationMatrixFromVector(rot, Rv);
//                                   
//                float[] values = new float[3];
//
//                SensorManager.getOrientation(rot, values);
//                float azimuth = values[0];
//                float pitch = values[1];
//                float roll = values[2];
//                
//                //float azimuthDeg = (float) Math.toDegrees(azimuth);
//                float pitchDeg = (float) Math.toDegrees(pitch);
//                float rollDeg = (float) Math.toDegrees(roll);
                
                break;
            }
            case Sensor.TYPE_ORIENTATION:
                //float azimuth = mSensorValues[0];
                float pitch = mSensorValues[1];
                float roll  = mSensorValues[2];
                
                double pitchRad = Math.toRadians(pitch);
                double rollRad  = Math.toRadians(roll);
                float s2 = (float)Math.sin(pitchRad);
                float c2 = (float)Math.cos(pitchRad);
                float s3 = (float)Math.sin(rollRad);
                float c3 = (float)Math.cos(rollRad);
                Matrix M = TransformUtils.createMatrix3(
                        c3, 0, s3,
                        -s2*c3, c2, -c3*s2,
                        -c2*s3, -s2, c2*c3
                );
                
                // z-> (s3, -c3*s2, c2*c3)
                gx = s3;
                gy = -c3*s2;
                gz = c2*c3;
                
                break;
            case Sensor.TYPE_MAGNETIC_FIELD:
            case Sensor.TYPE_MAGNETIC_FIELD_UNCALIBRATED:
                //TODO implement some of these
            default:
                break;
        }
    }

    @Override
    public void onAccuracyChanged (Sensor senor, int accuracy) {
        //TODO uncalibrated magnetometer?
    }
    
    /**
     * Calculate the orthogonal view's transformation given by the the plane's normalvector: <b>n = (u,v,w)</b>.
     * @param f focal length of the camera in the same unit as the picture to be transformed is. (e.g. in pixel)
     * @return matrix of the 2D-transformation, which transforms the normalvector to the z-axis.
     */
    public Matrix getOrthoView(float f){
        //return SensorUtils.getRotatedView(mRotationVector, f);
        return SensorUtils.getOrthoView(gx, gy, gz, f);
    }
    
    /**
     * @return elevation in radians
     */
    public double getElevation() {
        return Math.atan2(Math.sqrt(gx*gx+gy*gy),gz);
    }
    
    /**
     * @return elevation in degrees
     */
    public double getElevationDeg() {
        return Math.toDegrees(getElevation());
    }
    
    public float getZoomCorrection() {
        return SensorUtils.getRelativeDistance(gx,gy,gz);
    }
    
    
    
//    private float[] multiply(float[] rv1, float[] rv2) {
//        
//        float w1 = rv1[3];
//        float x1 = rv1[0];
//        float y1 = rv1[1];
//        float z1 = rv1[2];
//        
//        float w2 = rv2[3];
//        float x2 = rv2[0];
//        float y2 = rv2[1];
//        float z2 = rv2[2];
//        
//        float xx = + x1 * w2 + y1 * z2 - z1 * y2 + w1 * x2;
//        float yy = - x1 * z2 + y1 * w2 + z1 * x2 + w1 * y2;
//        float zz = + x1 * y2 - y1 * x2 + z1 * w2 + w1 * z2;
//        float ww = - x1 * x2 - y1 * y2 - z1 * z2 + w1 * w2;
//
//        return new float[]{ww, xx, yy, zz}; //attention: size:4 (may loose heading accuracy)
//    }

}
