package hu.ureczky.utils;

import android.graphics.Matrix;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorManager;
import android.util.Log;
import android.view.Surface;

public class SensorUtils {
    
    private static final String TAG = "SensorUtils";
    
    /**
     * Transform and return with the values from a sensor's event.
     * 
     * <p>
     * The axes are re-defined according to the screen's rotation
     * (in the sense how the UI-layout is built, not the actual rotation).
     * After this the device's x, y axes mean the (rotated) screens x, y axes.
     * </p>
     * 
     * This transfromation is applied to:
     * <ul>
     * <li> accelerometer </li>
     * <li> gravity sensor </li>
     * <li> linear accelerometer </li>
     * <li> gyroscope </li>
     * <li> uncalibrated gyroscope </li>
     * <li> magnetic field sensor </li>
     * <li> uncalibrated magnetic field sensor </li>
     * <li> orientation sensor </li>
     * <li> rotation vector </li>
     * <li> game rotation vector </li>
     * <li> geomagnetic rotation vector </li>
     * </ul>
     * 
     * The rotation vectors' parameters will be extended to 5 (with cos(fi/2) and accuracy:-1) according to the new API.
     * 
     * <br/>
     * No transformation is applied to:
     * <ul>
     * <li> ambient temperature </li>
     * <li> light </li>
     * <li> pressure </li>
     * <li> proximity </li>
     * <li> relative humidity </li>
     * <li> significant motion </li>
     * <li> step counter </li>
     * <li> step detector </li>
     * </ul>
     * 
     * @param rotation screen's rotation: Surface.ROTATION_XXX (0, 90, 180, 270)
     * @param sensorEvent
     */
    public static float[] getValuesRemapedForScreen(int rotation, SensorEvent sensorEvent) {
        
        float[] sensorValues = sensorEvent.values.clone();
        int sensorType = sensorEvent.sensor.getType();
        
        switch(sensorType){
            case Sensor.TYPE_ACCELEROMETER:
            case Sensor.TYPE_GRAVITY:
            case Sensor.TYPE_LINEAR_ACCELERATION:
                // sensorValues: [a_x, a_y, a_z] (acceleration values [m/s^2])

                rotateAxes(rotation, sensorValues, 0, 1);
                
                break;
            case Sensor.TYPE_GYROSCOPE:
                // sensorValues: [w_x, w_y, w_z] (Angular speeds [rad/s])
                
                rotateAxes(rotation, sensorValues, 0, 1);
                
                break;
            case Sensor.TYPE_GYROSCOPE_UNCALIBRATED:
                // sensorValues: [w_x, w_y, w_z, b_x, b_y, b_z] (Uncalibrated angular speeds [rad/s], biases [rad/s])
                
                // Swap angular speeds
                rotateAxes(rotation, sensorValues, 0, 1);
                
                // Swap biases
                rotateAxes(rotation, sensorValues, 3, 4);
                
                break;
            case Sensor.TYPE_MAGNETIC_FIELD:
                // sensorValues: [B_x, B_y, B_z] (Magnetic field components [uT])
                
                rotateAxes(rotation, sensorValues, 0, 1);
                
                break;
            case Sensor.TYPE_MAGNETIC_FIELD_UNCALIBRATED:
                // sensorValues: [B_x, B_y, B_z, b_x, b_y, b_z] (Magnetic field components [uT], biases in micro-Tesla [uT])
                
                // Swap magnetic fields
                rotateAxes(rotation, sensorValues, 0, 1);
                
                // Swap biases
                rotateAxes(rotation, sensorValues, 3, 4);
                
                break;
            case Sensor.TYPE_ROTATION_VECTOR:
            case Sensor.TYPE_GAME_ROTATION_VECTOR:
            case Sensor.TYPE_GEOMAGNETIC_ROTATION_VECTOR:
                // sensorValues:
                // [0]: x*sin(phi/2)
                // [1]: y*sin(phi/2)
                // [2]: z*sin(phi/2)
                // [3]: cos(phi/2) (optional, but always present from API 18)
                // [4]: estimated heading Accuracy (in radians) (added in API 18, but -1 if unavailable)
                                
                // STEP 1
                // Copy the parameters
                float x = sensorValues[0];
                float y = sensorValues[1];
                float z = sensorValues[2];
                float w;
                
                // STEP 2
                // Extend the parameters if needed
                int length = sensorValues.length;
                if(length < 5) {
                    
                    // Calculate the real value (cos(phi/2)) if missing
                    if(length < 4){
                        // Calculate the real value (cos(phi/2)) if missing
                        
                        // cos^2(phi/2) = 1 - sin^2(phi/2)
                        double cos2fi2 = 1 - x * x + y * y + z * z;
                        // cos(phi/2)
                        w = (cos2fi2 > 0) ? (float) Math.sqrt(cos2fi2) : 0;
                    } else {
                        w = sensorValues[3];
                    }
                    
                    sensorValues = new float[5];
                    
                    // Indicate the accuracy is not available
                    sensorValues[4] = -1;
                } else {
                    // All values are available
                    w = sensorValues[3];
                }
                
                // STEP 3
                // Remap according to screen's rotation
                
                // BUGFIX for Issue#63268
                // Make the cosine coefficient positive in order to
                // keep the angle between [-180..180] degree
                int sign;
                
                double size = Math.sqrt(2);
                switch(rotation) {
                    case Surface.ROTATION_0:
                        // No need to transform
                        break;
                    case Surface.ROTATION_90:
                        sign = (z+w < 0) ? -1 : +1;
                        sensorValues[0] = (float)(sign * (x-y) / size);
                        sensorValues[1] = (float)(sign * (x+y) / size);
                        sensorValues[2] = (float)(sign * (z-w) / size);
                        sensorValues[3] = (float)(sign * (z+w) / size);
                        break;
                    case Surface.ROTATION_180:
                        sign = (z > 0) ? -1 : +1;
                        sensorValues[0] = (float)(sign *  y );
                        sensorValues[1] = (float)(sign * -x );
                        sensorValues[2] = (float)(sign *  w );
                        sensorValues[3] = (float)(sign * -z );
                        break;
                    case Surface.ROTATION_270:
                        sign = (w < z) ? -1 : +1;
                        sensorValues[0] = (float)(sign * (y+x) / size);
                        sensorValues[1] = (float)(sign * (y-x) / size);
                        sensorValues[2] = (float)(sign * (w+z) / size);
                        sensorValues[3] = (float)(sign * (w-z) / size);
                        break;
                    default:
                        Log.e("SensorUtils", "Invalid rotation(" + rotation + ")");
                }
                
                break;
                
            case Sensor.TYPE_ORIENTATION:
                // sensorValues: [azimuth, pitch, roll] in degrees
                // Deprecated
                Log.w("SensorUtils", "Deprecated orientation sensor type. Use rotation vector instead with getOrientation() method.");
                break;
            case Sensor.TYPE_TEMPERATURE:
                Log.w("SensorUtils", "Deprecated orientation sensor type. Use rotation vector instead with getOrientation() method.");
                break;
            case Sensor.TYPE_AMBIENT_TEMPERATURE:
                // sensorValues[0]: ambient (room) temperature in degree Celsius.
            case Sensor.TYPE_LIGHT:
                // sensorValues[0]: Ambient light level in SI lux units
            case Sensor.TYPE_PRESSURE:
                // sensorValues[0]: Atmospheric pressure in hPa (millibar)
            case Sensor.TYPE_PROXIMITY:
                // sensorValues[0]: Proximity sensor distance measured in centimeters
            case Sensor.TYPE_RELATIVE_HUMIDITY:
                // sensorValues[0]: Relative ambient air humidity in percent
            //case Sensor.TYPE_SIGNIFICANT_MOTION:
                // Its a trigger sensor, not captured here
            case Sensor.TYPE_STEP_COUNTER:
                // TODO not documented yet
            case Sensor.TYPE_STEP_DETECTOR:
                // TODO not documented yet
            // The common part for the above:
                // No need to change anything
                break;
            default:
                Log.w("SensorUtils", "Unknown sensor type("+sensorType+")");
        }       
        return sensorValues;
    }
    
    private static float[] mTempMatrix_3x3 = new float[9];
    private static float[] mTempMatrix_4x4 = new float[16];
    
    /**
     * The same as SensorManager#remapCoordinateSystem,
     * but width a fix for the
     * <a href="https://code.google.com/p/android/issues/detail?id=58858">issue#58858</a>:
     * It can in-place (when outR=inR) remap a 3x3 matrix as well.
     * @see{android.hardware.SensorManager#CoordinateSystem}
     */
    public static boolean remapCoordinateSystem(float[] inR, int X, int Y, float[] outR)
    {
        if (inR == outR) {
            boolean homo = (outR.length == 16); 
            synchronized(homo ? mTempMatrix_4x4 : mTempMatrix_3x3) {
                // we don't expect to have a lot of contention
                if (remapCoordinateSystemImpl(inR, X, Y, homo ? mTempMatrix_4x4 : mTempMatrix_3x3)) {
                    final int size = outR.length;
                    for (int i=0 ; i<size ; i++)
                        outR[i] = homo ? mTempMatrix_4x4[i] : mTempMatrix_3x3[i];
                    return true;
                }
                return false;
            }
        }
        return remapCoordinateSystemImpl(inR, X, Y, outR);
    }
    
    // Unchanged copy from android source (but it was private)
    private static boolean remapCoordinateSystemImpl(float[] inR, int X, int Y, float[] outR)
    {
        /*
         * X and Y define a rotation matrix 'r':
         *
         *  (X==1)?((X&0x80)?-1:1):0    (X==2)?((X&0x80)?-1:1):0    (X==3)?((X&0x80)?-1:1):0
         *  (Y==1)?((Y&0x80)?-1:1):0    (Y==2)?((Y&0x80)?-1:1):0    (Y==3)?((X&0x80)?-1:1):0
         *                              r[0] ^ r[1]
         *
         * where the 3rd line is the vector product of the first 2 lines
         *
         */

        final int length = outR.length;
        if (inR.length != length)
            return false;   // invalid parameter
        if ((X & 0x7C)!=0 || (Y & 0x7C)!=0)
            return false;   // invalid parameter
        if (((X & 0x3)==0) || ((Y & 0x3)==0))
            return false;   // no axis specified
        if ((X & 0x3) == (Y & 0x3))
            return false;   // same axis specified

        // Z is "the other" axis, its sign is either +/- sign(X)*sign(Y)
        // this can be calculated by exclusive-or'ing X and Y; except for
        // the sign inversion (+/-) which is calculated below.
        int Z = X ^ Y;

        // extract the axis (remove the sign), offset in the range 0 to 2.
        final int x = (X & 0x3)-1;
        final int y = (Y & 0x3)-1;
        final int z = (Z & 0x3)-1;

        // compute the sign of Z (whether it needs to be inverted)
        final int axis_y = (z+1)%3;
        final int axis_z = (z+2)%3;
        if (((x^axis_y)|(y^axis_z)) != 0)
            Z ^= 0x80;

        final boolean sx = (X>=0x80);
        final boolean sy = (Y>=0x80);
        final boolean sz = (Z>=0x80);

        // Perform R * r, in avoiding actual muls and adds.
        final int rowLength = ((length==16)?4:3);
        for (int j=0 ; j<3 ; j++) {
            final int offset = j*rowLength;
            for (int i=0 ; i<3 ; i++) {
                if (x==i) outR[offset+i] = sx ? -inR[offset+0] : inR[offset+0];
                if (y==i) outR[offset+i] = sy ? -inR[offset+1] : inR[offset+1];
                if (z==i) outR[offset+i] = sz ? -inR[offset+2] : inR[offset+2];
            }
        }
        if (length == 16) {
            outR[3] = outR[7] = outR[11] = outR[12] = outR[13] = outR[14] = 0;
            outR[15] = 1;
        }
        return true;
    }
    
    /**
     * Rotate axes components in an array, containing vector components
     * 
     * @param rotation Surface.ROTATION_XXX (0, 90, 180, 270): rotation constant to express rotation around the third axis (first axis x second axis)
     * @param values an array containing vector components
     * @param idx1 index of the first axis
     * @param idx2 index of the second axis
     */
    private static void rotateAxes(int rotation, float[] values, int idx1, int idx2){
        switch(rotation) {
            case Surface.ROTATION_0:
                // no need to rotate
                // x := x
                // y := y
                break;
            case Surface.ROTATION_90: {
                // x := -y
                // y := x
                float x_old = values[idx1];
                values[idx1] = -values[idx2];
                values[idx2] = x_old;
                break;
            }
            case Surface.ROTATION_180: {
                // x := -x
                // y := -y
                values[idx1] *= -1;
                values[idx2] *= -1;
                break;
            }
            case Surface.ROTATION_270:{    
                // x := y
                // y := -x
                float x_old = values[idx1];
                values[idx1] = values[idx2];
                values[idx2] = -x_old;
                break;
            }
            default:
                Log.d("SensorUtils", "Wrong rotation code(" + rotation + "");
        }
        
    }
    

//    /**
//     * Remap the coordinate system for AR according to the screen's orientation.
//     * @param rotationMatrix float[16]
//     * @param orientation Surface.Rotation_DEGREE
//     */
//    public static void remapForCameraView(float[] rotationMatrix, int orientation) {
//        //Needed if its not the default orientation
//        
//          switch(orientation){
//              case Surface.ROTATION_0:
//                  SensorManager.remapCoordinateSystem(
//                      rotationMatrix,
//                      SensorManager.AXIS_X,
//                      SensorManager.AXIS_Z,
//                      rotationMatrix
//                  );
//                  break;
//              case Surface.ROTATION_90:
//                  SensorManager.remapCoordinateSystem(
//                      rotationMatrix,
//                      SensorManager.AXIS_Z,
//                      SensorManager.AXIS_MINUS_X,
//                      rotationMatrix
//                  );
//                  break;
//              case Surface.ROTATION_180:
//                  SensorManager.remapCoordinateSystem(
//                      rotationMatrix,
//                      SensorManager.AXIS_MINUS_X,
//                      SensorManager.AXIS_MINUS_Z,
//                      rotationMatrix
//                  );
//                  break;
//              case Surface.ROTATION_270:
//                  SensorManager.remapCoordinateSystem(
//                      rotationMatrix,
//                      SensorManager.AXIS_MINUS_Z,
//                      SensorManager.AXIS_X,
//                      rotationMatrix
//                  );
//                  break;
//          }
//      }
    
    /**
     * Get the relative <b>d/d0</b> distance from a horizontal plane.
     * <ul>
     * <li><b>O</b> is the origo, the center of the view, where the device is.</li>
     * <li><b>M</b> is the orthopoint of the origo.</li>
     * <li><b>P</b> is where the device's <b>-z</b> axis intersect the plane.</li>
     * <li><b>d0</b> measures the <b>OM</b> distance, the altitude of the device.</li>
     * <li><b>d</b> measures the <b>OP</b> distance</li>
     * </ul>
     * <pre>   
     *            O (device)
     *           /|
     *          / |
     *      d  /  | d0 (altitude)
     *        /   |
     *       /   _| 
     *  ____/___|_|___________ground
     *     P      M
     * </pre>
     * @param x x coordinate of the gravitational acceleration's vector
     * @param y y coordinate of the gravitational acceleration's vector
     * @param z z coordinate of the gravitational acceleration's vector
     * @return relative distance: <b>d/d0</b>, where d0 is the distance from the 
     */
    public static float getRelativeDistance(float x, float y, float z) {
        return (float) (Math.sqrt(x*x+y*y+z*z) / Math.abs(z));
    }
    
    /**
     * Transform the rotation vector to a normalized gravity vector('s opposite).
     * 
     * @param rotationVector in [x,y,z,w] format, e.g. from the
     *        {@link android.hardware.Sensor#TYPE_ROTATION_VECTOR TYPE_ROTATION_VECTOR}
     *        sensor type.
     * @return gravity vector('s opposite) (the same as the
     *         {@link android.hardware.Sensor#TYPE_GRAVITY TYPE_GRAVITY}
     *         sensor type would return, but with length 1)
     */
    public static float[] getGravityFromRotationVector(float[] rotationVector) {
        // Quaternion reprezentation: r = w+xi+yj+zk
        float w = rotationVector[3]; // cos(phi/2)
        float x = rotationVector[0]; // x*sin(phi/2)
        float y = rotationVector[1]; // y*sin(phi/2)
        float z = rotationVector[2]; // z*sin(phi/2)
        
        //     r^(-1)    *         z       *       r
        //  (w-xi-yj-zk) * (0+0*i+0*j+1*k) * (w+xi+yj+zk) = 0 + 2 * (x*z - w*y) * i + 2 * (y*z + w*x) * j + (w*w - x*x - y*y + z*z) * k
        return new float[] {
            2 * (x*z - w*y),       // gx
            2 * (y*z + w*x),       // gy
            w*w - x*x - y*y + z*z  // gz
        };
    }
    
    public static float[] getGravityFromOrientation(float[] orientation) {
        //Special case of getRotationMatrixFromOrientation (azimuth=0) and only need the transformed z axis
        
        float pitchDegree = orientation[1];
        float rollDegree  = orientation[2];
        
        double pitch = Math.toRadians(pitchDegree);
        double roll  = Math.toRadians(rollDegree);
        
        float s2 = (float)Math.sin(pitch);
        float c2 = (float)Math.cos(pitch);
        
        float s3 = (float)Math.sin(roll);
        float c3 = (float)Math.cos(roll);
        
        // z -> (s3, -c3*s2, c2*c3)
        float u = s3;
        float v = -c3*s2;
        float w = c2*c3;
        
        return new float[]{u,v,w};
    }
    
    public static float[] getRotationMatrixFromOrientation(float[] orientation) {
        
        float azimuthDegree = orientation[0];
        float pitchDegree   = orientation[1];
        float rollDegree    = orientation[2];
        
        double azimuth = Math.toRadians(azimuthDegree);
        double pitch   = Math.toRadians(pitchDegree);
        double roll    = Math.toRadians(rollDegree);
        
        // z and x rotations defined in opposite direction 
        azimuth *= -1;
        pitch   *= -1;
        
        float s1 = (float) Math.sin(azimuth);
        float c1 = (float) Math.cos(azimuth);
        float s2 = (float) Math.sin(pitch);
        float c2 = (float) Math.cos(pitch);
        float s3 = (float) Math.sin(roll);
        float c3 = (float) Math.cos(roll);

        // Rotation matrix for z-x'-y'' axes
        return new float[] {
                c1*c3 - s1*s2*s3 , -c2*s1 , c1*s3 + c3*s1*s2,
                c3*s1 + c1*s2*s3 ,  c1*c2 , s1*s3 - c1*c3*s2,
                   -c2*s3        ,   s2   ,     c2*c3
        };
    }
    
    /**
     * Calculate the orthogonal view's transformation given by the the plane's normalvector: <b>n = (u,v,w)</b>.
     * @param u normal vectors 1st component, the projection of the normalvector to the device's x-axis.
     * @param v normal vectors 2nd component, the projection of the normalvector to the device's y-axis.
     * @param w normal vectors 3rd component, the projection of the normalvector to the device's z-axis. 
     * @param f focal length of the camera in the same unit as the picture to be transformed is. (e.g. in pixel)
     * @return matrix of the 2D-transformation, which transforms the normalvector to the z-axis.
     */
    public static Matrix getOrthoView(float u, float v, float w, float f) {
    
        // above the horizon, flip the vector in order to transform the ceiling instead of the floor 
        if(w < 0) {
            u *= -1;
            v *= -1;
            w *= -1;
        }
    
        float u2 = u * u;
        float v2 = v * v;
        float w2 = w * w;

        float m2 = u2 + v2;
        float m  = (float) Math.sqrt(m2);
        float n2 = u2 + v2 + w2;
        float n  = (float) Math.sqrt(n2);
                
        return TransformUtils.createMatrix3(
            n*u2+w*v2 , u*v*(n-w) , 0    ,
            u*v*(n-w) , n*v2+w*u2 , 0    ,
            -m2*u/f   , -m2*v/f   , m2*w
        );
    }
    
    public static Matrix getRotatedView(float[] rV, float f) {
        
        float[] gravity = getGravityFromRotationVector(rV);
        float u = gravity[0];
        float v = gravity[1];
        float w = gravity[2];
        
        // above the horizon, flip the vector in order to transform the ceiling instead of the floor 
        if(w < 0) {
            u *= -1;
            v *= -1;
            w *= -1;
        }
        
        float[] pitchRollRotationVector = eliminateYaw(rV);
        
        //TODO flip above horizon
        float ww = pitchRollRotationVector[3]; //cos(phi/2)
        if(2*ww*ww < 1) { //|phi| > 90 deg
            float cos = ww;
            float sin = (float) Math.sqrt(1-cos*cos);
            float phi = (float) Math.acos(cos)*2;
            
            pitchRollRotationVector[0] *= -cos/sin;
            pitchRollRotationVector[1] *= -cos/sin;
            pitchRollRotationVector[2] *= -cos/sin;
            pitchRollRotationVector[3] *= sin;
            
            // +180 degree
//            float coss = (float) Math.cos((Math.PI + phi)/2);
//            float sinn = (float) Math.sin((Math.PI + phi)/2);
//            
//            pitchRollRotationVector[0] *= sinn/sin;
//            pitchRollRotationVector[1] *= sinn/sin;
//            pitchRollRotationVector[2] *= sinn/sin;
//            pitchRollRotationVector[3] *= coss/cos;
            
        }
        
        float [] R = new float[9];
        SensorManager.getRotationMatrixFromVector(R, pitchRollRotationVector);

        float r11 = R[0];
        float r12 = R[1];
        float r13 = R[2];
        float r21 = R[3];
        float r22 = R[4];
        float r23 = R[5];
        float r31 = R[6];
        float r32 = R[7];
        float r33 = R[8];
        
        return TransformUtils.createMatrix3(
                f*(w*r11 - u*r13), f*(w*r12 - v*r13), 0,
                f*(w*r21 - u*r23), f*(w*r22 - v*r23), 0,
                -w*r31+u*r33-u, -w*r32+v*r33-v, f*w
        );
    }
    
    public static float[] getRotationMatrixFromVector(float[] rotationVector) {

        float w = rotationVector[3];
        float x = rotationVector[0];
        float y = rotationVector[1];
        float z = rotationVector[2];

        float xx = 2 * x * x;
        float yy = 2 * y * y;
        float zz = 2 * z * z;
        float xy = 2 * x * y;
        float zw = 2 * z * w;
        float xz = 2 * x * z;
        float yw = 2 * y * w;
        float yz = 2 * y * z;
        float xw = 2 * x * w;
        
        float[] R = new float[] {
                1 - yy - zz, xy - zw    , xz + yw,
                xy + zw    , 1 - xx - zz, yz - xw,
                xz - yw    , yz + xw    , 1 - xx - yy
        };
        
        return R;
    }
    
    /**
     * Remap the coordinate system
     * @param rotationMatrix float[16]
     * @param orientation Surface.Rotation_DEGREE
     */
    public static void remapForScreen(float[] rotationMatrix, int orientation) {
        //Needed if its not the default orientation
        
          float[] copy = rotationMatrix.clone();
          switch(orientation){
              case Surface.ROTATION_0:
                 // No need to change
//                  SensorManager.remapCoordinateSystem(
//                      copy,
//                      SensorManager.AXIS_X,
//                      SensorManager.AXIS_Y,
//                      rotationMatrix
//                  );
                  break;
              case Surface.ROTATION_90:
                  SensorManager.remapCoordinateSystem(
                      copy,
                      SensorManager.AXIS_Y,
                      SensorManager.AXIS_MINUS_X,
                      rotationMatrix
                  );
                  break;
              case Surface.ROTATION_180:
                  SensorManager.remapCoordinateSystem(
                      copy,
                      SensorManager.AXIS_MINUS_X,
                      SensorManager.AXIS_MINUS_Y,
                      rotationMatrix
                  );
                  break;
              case Surface.ROTATION_270:
                  SensorManager.remapCoordinateSystem(
                      copy,
                      SensorManager.AXIS_MINUS_Y,
                      SensorManager.AXIS_X,
                      rotationMatrix
                  );
                  break;
          }
      }
        
    /**
     * Remap the coordinate system
     * @param rotationMatrix float[16]
     * @param orientation Surface.Rotation_DEGREE
     */
    public static void remapForView(float[] rotationMatrix) {
        SensorManager.remapCoordinateSystem(
            rotationMatrix,
            SensorManager.AXIS_X,
            SensorManager.AXIS_Z,
            rotationMatrix
        );
      }
    
    /**
     * Remap the coordinate system
     * @param rotationMatrix float[16]
     * @param orientation Surface.Rotation_DEGREE
     */
    public static void remapForScreenView(float[] rotationMatrix, int orientation) {
        //Needed if its not the default orientation
        
          switch(orientation){
              case Surface.ROTATION_0:
                  SensorManager.remapCoordinateSystem(
                      rotationMatrix,
                      SensorManager.AXIS_X,
                      SensorManager.AXIS_Z,
                      rotationMatrix
                  );
                  break;
              case Surface.ROTATION_90:
                  SensorManager.remapCoordinateSystem(
                      rotationMatrix,
                      SensorManager.AXIS_Z,
                      SensorManager.AXIS_MINUS_X,
                      rotationMatrix
                  );
                  break;
              case Surface.ROTATION_180:
                  SensorManager.remapCoordinateSystem(
                      rotationMatrix,
                      SensorManager.AXIS_MINUS_X,
                      SensorManager.AXIS_MINUS_Z,
                      rotationMatrix
                  );
                  break;
              case Surface.ROTATION_270:
                  SensorManager.remapCoordinateSystem(
                      rotationMatrix,
                      SensorManager.AXIS_MINUS_Z,
                      SensorManager.AXIS_X,
                      rotationMatrix
                  );
                  break;
          }
      }
    
    /**
     * Eliminate the Yaw component (the compass) from a rotation vector
     * @param rotation vector float[4]
     * @return rotation vector without the yaw
     */
    public static float[] eliminateYaw(float[] rV){
        float ww = rV[3];
        float xx = rV[0];
        float yy = rV[1];
        float zz = rV[2];
        
        float zw = (float)Math.sqrt(zz * zz + ww * ww);
        float q0 = zw;
        float q1 = (xx * ww + yy * zz) / zw;
        float q2 = (yy * ww - xx * zz) / zw;
        float q3 = 0;
        
        return new float[]{q1, q2, q3, q0};
    }
    
    public static float vectorLen(float[] v) {
        float sumSq = 0;
        for(float vi: v) {
            sumSq += vi * vi; 
        }
        return (float) Math.sqrt(sumSq);
    }
    
    /**
     * @param vector IN
     * @param unitVector OUT - same direction, 1 size
     * @return vector's original length
     */
    public static float toUnit(float[] vector, float[] unitVector) {
        float length = vectorLen(vector);
        if(length > 0) {
            for(int i = 0; i < vector.length; i++) {
                unitVector[i] = vector[0] / length;
            }
        }
        return length;
    }
    
    public static float angleRad(float[] v1, float[] v2) {
        return (float) Math.acos(scalarProduct(v1,v2) / ( vectorLen(v1) * vectorLen(v2) ) );
    }
    
    public static float scalarProduct(float[] v1, float[] v2) {
        float sum = 0;
        for(int i = 0; i < v1.length; i++) {
            sum += v1[i]*v2[i];
        }
        return sum;
    }
    
    public static float normalizeAzimuthDeg(float azimuthDeg) {
        return (azimuthDeg + 360) % 360;
    }
    
    public static float azimToSignedDeg(float azimDeg) {
        float angleDeg = normalizeAzimuthDeg(azimDeg);
        if(angleDeg>180) angleDeg = angleDeg - 360;
        return angleDeg;
    }
    
    
}
