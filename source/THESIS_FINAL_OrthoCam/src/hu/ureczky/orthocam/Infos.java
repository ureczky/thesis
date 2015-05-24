package hu.ureczky.orthocam;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.hardware.Camera;
import android.hardware.Camera.CameraInfo;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.util.Log;
import android.widget.ScrollView;
import android.widget.TextView;

import java.lang.reflect.Method;
import java.util.List;

public class Infos extends Activity {

    private StringBuilder details;
    
    @SuppressLint("NewApi")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d("Lifecycles", "Infos-onCreate");
        
        setContentView(R.layout.infos);
        ScrollView scrollView = (ScrollView) findViewById(R.id.scrollView);
        TextView detailsText = new TextView(this);
        scrollView.addView(detailsText);
        
        //TODO JSON?
        
        details = new StringBuilder();
        
        //TODO tabs
        //TODO fps/=1000 ?
        //TODO zoom: 0
        //TODO share?
        
        // OS
        int apiLevel = android.os.Build.VERSION.SDK_INT;
        String osVersionName = translateApiLevel(apiLevel);
        details.append("API Level: " + apiLevel + ": Android " + osVersionName + "\n");
        
        // Hardware
        details.append("CPU_ABI: "   + android.os.Build.CPU_ABI + "\n");
        details.append("CPU_ABI2: "  + android.os.Build.CPU_ABI2 + "\n");
        
        details.append("Brand: "     + android.os.Build.BRAND + "\n");
        details.append("Device: "    + android.os.Build.DEVICE + "\n");
        details.append("Display: "   + android.os.Build.DISPLAY + "\n");
        details.append("Hardware: "  + android.os.Build.HARDWARE + "\n");
        details.append("Product: "   + android.os.Build.PRODUCT + "\n");
        
        // Device
        details.append("Device: \n");
        details.append("  Manufacturer: " + android.os.Build.MANUFACTURER + "\n");
        details.append("  Model: " + android.os.Build.MODEL + "\n");

        // OpenCV Manager
        String openCV;
        try {
            PackageInfo openCVPackage = getPackageManager().getPackageInfo("org.opencv.engine", 0 );
            openCV = openCVPackage.versionName;
        } catch( PackageManager.NameNotFoundException e ){
            openCV = "not available";
        }
        details.append("OpenCV Manager: " + openCV + "\n");
        
        details.append("\n");
        details.append("Camera Details\n");
        
        int numberOfCameras = (apiLevel >= 9) ? Camera.getNumberOfCameras() : 1; // API9
        details.append("  Number of Cameras: "+ numberOfCameras +"\n");
        for(int cameraId = 0; cameraId < numberOfCameras; cameraId++) {
            // ID
            details.append("    - " + ((cameraId==0) ? "Default" : "Additional" ) + " Camera (ID: " + cameraId + ")\n");
            CameraInfo cameraInfo = new CameraInfo();
            Camera.getCameraInfo(cameraId, cameraInfo);
            
            // FACING
            int facing = cameraInfo.facing;
            String facingStr;
            switch(facing){
                case CameraInfo.CAMERA_FACING_BACK:  facingStr = "Back";    break;
                case CameraInfo.CAMERA_FACING_FRONT: facingStr = "Front";   break;
                default:                             facingStr = "Unknown"; break;
            }
            details.append("        Facing: " + facingStr + "\n");
            
            // ORIENTATION
            String orientation = (apiLevel >= 9) ? String.valueOf(cameraInfo.orientation) : "N/A";
            details.append("        Orientation: " + orientation + " [degree]\n");
            
            // SHUTTER
            boolean canDisableShutterSound = (apiLevel >= 17) ? cameraInfo.canDisableShutterSound : false;
            details.append("        Can disable shutter sound: " + canDisableShutterSound + "\n");
            
            Camera camera = null;
            try {
                camera = (apiLevel >= 9)
                    ? Camera.open(cameraId)
                    : Camera.open();
                Camera.Parameters cameraParams = camera.getParameters();
                
                boolean isAutoExposureLockSupported       = (apiLevel < 14) ? false : cameraParams.isAutoExposureLockSupported();     // API 14
                boolean isAutoWhiteBalanceLockSupported   = (apiLevel < 14) ? false : cameraParams.isAutoWhiteBalanceLockSupported(); // API 14
                boolean isZoomSupported                   = (apiLevel <  8) ? false : cameraParams.isZoomSupported();                 // API 8
                boolean isSmoothZoomSupported             = (apiLevel <  8) ? false : cameraParams.isSmoothZoomSupported();           // API 8
                boolean isVideoSnapshotSupported          = (apiLevel < 14) ? false : cameraParams.isVideoSnapshotSupported();        // API 14
                boolean isVideoStabilizationSupported     = (apiLevel < 15) ? false : cameraParams.isVideoStabilizationSupported();   // API 15
                           
                cameraParams.getVerticalViewAngle();   // API 8
                cameraParams.getHorizontalViewAngle(); // API 8
                cameraParams.getFocalLength(); // API 8

                //TODO replace param order
                //TODO integrate API level
                printList(cameraParams, "Antibanding",         "getSupportedAntibanding",        "getAntibanding");       // API 5/5
                printList(cameraParams, "Picture format",      "getSupportedPictureFormats",     "getPictureFormat");     // API 1/5 //TODO conversion
                printList(cameraParams, "Preview format",      "getSupportedPreviewFormats",     "getPreviewFormat");     // API 1/5 //TODO conversion
                printList(cameraParams, "Picture size",        "getSupportedPictureSizes",       "getPictureSize");       // API 1/5
                printList(cameraParams, "Preview size",        "getSupportedPreviewSizes",       "getPreviewSize");       // API 1/5
                printList(cameraParams, "Jpeg thumbnail size", "getSupportedJpegThumbnailSizes", "getJpegThumbnailSize"); // API 5/8
                printList(cameraParams, "Video size",          "getSupportedVideoSizes",         "");                     // API 11  //TODO 1?
                printList(cameraParams, "Color effects",       "getSupportedColorEffects",       "getColorEffect");       // API 5/5
                printList(cameraParams, "White balance",       "getSupportedWhiteBalance",       "getWhiteBalance");      // API 5/5
                printList(cameraParams, "Scene mode",          "getSupportedSceneModes",         "getSceneMode");         // API 5/5
                printList(cameraParams, "Flash mode",          "getSupportedFlashModes",         "getFlashMode");         // API 5/5
                printList(cameraParams, "Focus mode",          "getSupportedFocusModes",         "getFocusMode");         // API 5/5
                printList(cameraParams, "Preview FPS",         "getSupportedPreviewFpsRange",    "getPreviewFrameRate");  // API 1/9 //TODO /1000, fixed frame rate/auto frame rate: fluctuates
                
                //TODO list all possiblity even if not supported
                //Parameters.FOCUS_MODE_INFINITY
                //Parameters.FOCUS_MODE_AUTO
                //Parameters.FOCUS_MODE_EDOF
                //Parameters.FOCUS_MODE_FIXED
                //Parameters.FOCUS_MODE_MACRO
                //Parameters.FOCUS_MODE_CONTINUOUS_PICTURE
                //Parameters.FOCUS_MODE_CONTINUOUS_VIDEO
                
                try{
                    if(apiLevel > 5){
                        details.append("            Supported preview frame rates (deprecated in API 9):\n");
                        List<Integer> fpss = cameraParams.getSupportedPreviewFrameRates(); // API 5
                        for(Integer fps: fpss){
                            details.append("              - " + fps + "\n");
                        }
                    }
                }catch(Exception e){
                    e.printStackTrace();
                }
                
                details.append("          Zoom: " + (isZoomSupported ? cameraParams.getZoom() : "not supported") + "\n");
                if(isZoomSupported) {
                    details.append("            Supported zoom values:\n");
                    List<Integer> zooms = cameraParams.getZoomRatios(); // API 8
                    for(Integer zoom: zooms){
                        details.append("            - " + zoom/(float)100 + "\n");
                    }
                    details.append("            Max zoom: " + cameraParams.getMaxZoom() + "\n"); // API 8
                    details.append("            Smooth zoom: " + (isSmoothZoomSupported ? "supported" : "not supported") + "\n");
                }
                
                details.append("        Supported features ( + / - )\n");
                details.append("          " + (isAutoExposureLockSupported ? "+" : "-")     + " Auto Exposure Lock"      + "\n");
                details.append("          " + (isAutoWhiteBalanceLockSupported ? "+" : "-") + " Auto White Balance Lock" + "\n");
                details.append("          " + (isVideoSnapshotSupported ? "+" : "-")        + " Video Snapshot "         + "\n");
                details.append("          " + (isVideoStabilizationSupported ? "+" : "-")   + " Video Stabilization "    + "\n");
                
                
                cameraParams.getExposureCompensation(); //API 8
                cameraParams.getExposureCompensationStep(); //API 8
                
            } finally{
                if(camera != null){
                    camera.stopPreview();
                    camera.release();
                    camera = null;
                }
            }
        }
        
        // SENSORS
        
        details.append("\nSENSORS\n");
        SensorManager manager = (SensorManager)getSystemService(SENSOR_SERVICE);
        
        //TODO what is this? count?
        //int i = manager.getSensors();
        
        List<Sensor> sensors = manager.getSensorList(Sensor.TYPE_ALL);
        for(Sensor sensor: sensors){
            details.append("  - "                             + sensor.getName()                      +         "\n"); //API 3
            details.append("    Type: "                       + translateSensorType(sensor.getType()) +         "\n"); //API 3
            details.append("    Vendor: "                     + sensor.getVendor()                    +         "\n"); //API 3
            details.append("    Version: "                    + sensor.getVersion()                   +         "\n"); //API 3
            details.append("    Power: "                      + sensor.getPower()                     + " mA" + "\n"); //API 3
            details.append("    Resolution: "                 + sensor.getResolution()                +         "\n"); //API 3
            details.append("    Min delay: "                  + sensor.getMinDelay()                  + " ms" + "\n"); //API 9
            details.append("    Max range: "                  + sensor.getMaximumRange()              +         "\n"); //API 3
            if(apiLevel>=19){
                details.append("    Max fifo event count: "       + sensor.getFifoMaxEventCount()             + "\n"); //API 19
                details.append("    Fifo reserved envent count: " + sensor.getFifoReservedEventCount()        + "\n"); //API 19
            }else{
                details.append("    Fifo events: not supported\n");
            }
        }
        
        detailsText.setText(details.toString());
        
    }
    
    
    private void printList(Camera.Parameters cameraParams, String feature, String supportedMethod, String currentMethod){
        
        details.append("          " + feature + ": ");
        Method method_current;
        Object current;
        try {
            method_current = cameraParams.getClass().getDeclaredMethod(currentMethod);
            current = method_current.invoke(cameraParams);
            String currentStr = toString(current);
            
            details.append(currentStr + "\n");
            try {
                details.append("            Supported alternatives: \n");
                Method method_supported = cameraParams.getClass().getDeclaredMethod(supportedMethod);
                List list = (List) method_supported.invoke(cameraParams);
                if(list==null || list.isEmpty() || list.size() == 1 && list.get(0).equals(currentStr)) {
                    details.append("              No alternatives supported\n");
                } else{
                    for(Object alternative : list){
                        String alternativeStr = toString(alternative);
                        boolean actual = alternativeStr.equals(currentStr);
                        if(actual) continue;
                        details.append("            - " + alternativeStr + "\n");
                    }
                }
            } catch (Exception e) { // NoSuchMethodException, IllegalAccessException, IllegalArgumentException, InvocationTargetException 
                e.printStackTrace();
                details.append("              N/A\n");
            }
            
        } catch (Exception e) { // NoSuchMethodException, IllegalAccessException, IllegalArgumentException, InvocationTargetException 
            e.printStackTrace();
            details.append("N/A\n");
        }
        
    }
    
    private String toString(Object obj){
        if(obj instanceof Camera.Size){
            Camera.Size size = (Camera.Size) obj;
            int w = size.width;
            int h = size.height;
            int gcd = MathUtil.gcd(w, h);
            int ratio_w = w/gcd;
            int ratio_h = h/gcd;
            return w + " x " + h + " (" + ratio_w + ":" + ratio_h + ")";
        } else if (obj instanceof int[]){
            int[] interval = (int[]) obj;
            return interval[0]/1000 + "-" + interval[1]/1000;
        }
            
        return String.valueOf(obj);
    }
    
    //private boolean notEmpty(List list){
    //    return (list != null && !list.isEmpty());
    //}
    
    private String translateApiLevel(int apiLevel){
        switch(apiLevel){
            case android.os.Build.VERSION_CODES.BASE:                   return "1.0";                        // 01
            case android.os.Build.VERSION_CODES.BASE_1_1:               return "1.1";                        // 02
            case android.os.Build.VERSION_CODES.CUPCAKE:                return "1.5 (Cupcake)";              // 03
            case android.os.Build.VERSION_CODES.DONUT:                  return "1.6 (Donut)";                // 04
            case android.os.Build.VERSION_CODES.ECLAIR_0_1:             return "2.0.1 (Eclair)";             // 05
            case android.os.Build.VERSION_CODES.ECLAIR:                 return "2.0 (Eclair)";               // 06
            case android.os.Build.VERSION_CODES.ECLAIR_MR1:             return "2.1 (Eclair)";               // 07
            case android.os.Build.VERSION_CODES.FROYO:                  return "2.2 (Froyo)";                // 08
            case android.os.Build.VERSION_CODES.GINGERBREAD:            return "2.3 (Gingerbread)";          // 09
            case android.os.Build.VERSION_CODES.GINGERBREAD_MR1:        return "2.3.3 (Gingerbread)";        // 10
            case android.os.Build.VERSION_CODES.HONEYCOMB:              return "3.0 (Honeycomb)";            // 11
            case android.os.Build.VERSION_CODES.HONEYCOMB_MR1:          return "3.1 (Honeycomb)";            // 12
            case android.os.Build.VERSION_CODES.HONEYCOMB_MR2:          return "3.2 (Honeycomb)";            // 13
            case android.os.Build.VERSION_CODES.ICE_CREAM_SANDWICH:     return "4.0 (Ice Cream Sandwich)";   // 14
            case android.os.Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1: return "4.0.3 (Ice Cream Sandwich)"; // 15
            case android.os.Build.VERSION_CODES.JELLY_BEAN:             return "4.1 (Jelly Bean)";           // 16
            case android.os.Build.VERSION_CODES.JELLY_BEAN_MR1:         return "4.2 (Jelly Bean)";           // 17
            case android.os.Build.VERSION_CODES.JELLY_BEAN_MR2:         return "4.3 (Jelly Bean)";           // 18
            case android.os.Build.VERSION_CODES.KITKAT:                 return "4.4 (Kitkat)";               // 19
            case android.os.Build.VERSION_CODES.CUR_DEVELOPMENT:        return "Current Development";        // 10.000
            default:                                                    return ">4.4";
        }
    }
    
    private String translateSensorType(int sensorType){
        switch(sensorType){
            case Sensor.TYPE_ALL:                         return "All";                           // -1, API 3+
            case Sensor.TYPE_ACCELEROMETER:               return "Accelerometer";                 // 01, API 3+
            case Sensor.TYPE_MAGNETIC_FIELD:              return "Magnetic field";                // 02, API 3+
            case Sensor.TYPE_ORIENTATION:                 return "Orientation";                   // 03, API 3-7 (Deprecated)
            case Sensor.TYPE_GYROSCOPE:                   return "Gyroscope";                     // 04, API 3+
            case Sensor.TYPE_LIGHT:                       return "Light";                         // 05, API 3+
            case Sensor.TYPE_PRESSURE:                    return "Pressure";                      // 06, API 3+
            case Sensor.TYPE_TEMPERATURE:                 return "Temperature";                   // 07, API 3-14 (Deprecated)
            case Sensor.TYPE_PROXIMITY:                   return "Proximity";                     // 08, API 3+
            case Sensor.TYPE_GRAVITY:                     return "Gravity";                       // 09, API 9+
            case Sensor.TYPE_LINEAR_ACCELERATION:         return "Linear acceleration";           // 10, API 9+
            case Sensor.TYPE_ROTATION_VECTOR:             return "Rotation vector";               // 11, API 9+
            case Sensor.TYPE_RELATIVE_HUMIDITY:           return "Relative humidity";             // 12, API 14+
            case Sensor.TYPE_AMBIENT_TEMPERATURE:         return "Ambient temperature";           // 13, API 14+
            case Sensor.TYPE_MAGNETIC_FIELD_UNCALIBRATED: return "Magnetic field (uncalibrated)"; // 14, API 18+
            case Sensor.TYPE_GAME_ROTATION_VECTOR:        return "Game rotation vector";          // 15, API 18+
            case Sensor.TYPE_GYROSCOPE_UNCALIBRATED:      return "Gyroscope (uncalibrated)";      // 16, API 18+
            case Sensor.TYPE_SIGNIFICANT_MOTION:          return "Significant motion";            // 17, API 18+
            case Sensor.TYPE_STEP_DETECTOR:               return "Step detector";                 // 18, API 19+
            case Sensor.TYPE_STEP_COUNTER:                return "Step counter";                  // 19, API 19+
            case Sensor.TYPE_GEOMAGNETIC_ROTATION_VECTOR: return "Geomagnetic rotation vector";   // 20, API 19+
            default:                                      return "Unknown";
        }
    }

}
