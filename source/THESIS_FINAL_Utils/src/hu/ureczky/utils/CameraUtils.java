package hu.ureczky.utils;

import android.annotation.TargetApi;
import android.app.Activity;
import android.hardware.Camera;
import android.hardware.Camera.CameraInfo;
import android.os.Build;
import android.util.Log;

public class CameraUtils {
    
    private static String TAG = "CameraUtils";
    
    @TargetApi(Build.VERSION_CODES.GINGERBREAD)
    public static Camera getCamera()
    {
        // Camera object to access the first back-facing camera on the device.
        Camera camera = null;
        try {
            if(android.os.Build.VERSION.SDK_INT >= 9) {
                camera = Camera.open(0); // Open the first camera
            } else {
                camera = Camera.open();
            }
            if(camera == null) {
                Log.e(TAG, "There is no camera");
            }
        } catch (Exception e) {
            Log.e(TAG, "Cannot open camera: " + e.toString());
        }
        return camera;
    }
    
    public static void setCameraDisplayOrientation(Activity activity, int cameraId, Camera camera) {
        CameraInfo info = new CameraInfo();
        Camera.getCameraInfo(cameraId, info);
        int rotation = activity.getWindowManager().getDefaultDisplay().getRotation();
        int degrees = AndroidUtils.getDegreesFromSurfaceRotation(rotation);

        int result;
        if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
            result = (info.orientation + degrees) % 360;
            result = (360 - result) % 360;  // compensate the mirror
        } else {  // back-facing
            result = (info.orientation - degrees + 360) % 360;
        }
        camera.setDisplayOrientation(result);
    }
    
    /** 
     * Get the focal length in pixels
     * @param f_mm focal length in millimeters (@see Camera.Parameters.getFocalLength())
     * @param sensorFovX_deg fov(X) of the camera sensor in radians (@see Camera.Parameters.getHorizontalViewAngle())
     * @param sensorFovY_deg fov(Y) of the camera sensor in radians (@see Camera.Parameters.getVerticalViewAngle())
     * @param zoomPercent zoom in percents [100..maxZoom]
     * @record f_px focal length in pixels, according to the given picture widht,height and take the zoom into consideration
     */
    public static float getFocalLengthInPx(float f_mm, float sensorFovX_rad, float sensorFovY_rad, int previewWidth_px, int previewHeight_px, double zoomPercent) {
        
        double tanSensorFovX2 = Math.tan(sensorFovX_rad / 2);
        double tanSensorFovY2 = Math.tan(sensorFovY_rad / 2);
        
        double sensorRatio = tanSensorFovX2 / tanSensorFovY2;
        // 16:9 = 1.7777
        // 4:3  = 1.3333
        
        double sensorWidth_mm  = 2 * f_mm * tanSensorFovX2;
        double sensorHeight_mm = 2 * f_mm * tanSensorFovY2;
        
//        Log.d("Focal length", f_mm + " mm");
//        Log.d("Sensor fovX",  Math.toDegrees(sensorFovX_rad) + " degree");
//        Log.d("Sensor fovY",  Math.toDegrees(sensorFovY_rad) + " degree");
//        Log.d("Sensor Size",  sensorWidth_mm + " mm x " + sensorHeight_mm + " mm");
//        Log.d("Sensor ratio", ""+sensorRatio);
        
        double previewRatio = previewWidth_px / (double)previewHeight_px;
        if(previewRatio < sensorRatio) {
            // recalc fovX
            tanSensorFovX2 = tanSensorFovY2 * previewRatio;
            sensorFovX_rad = (float)(2*Math.atan(tanSensorFovX2));
        } else {
            // recalc fovY
            tanSensorFovY2 = tanSensorFovX2 / previewRatio;
            sensorFovY_rad = (float)(2 * Math.atan(tanSensorFovY2));
        }
        
        sensorWidth_mm  = 2 * f_mm * Math.tan(sensorFovX_rad/2);
        sensorHeight_mm = 2 * f_mm * Math.tan(sensorFovY_rad/2);
        
//        Log.d("Sensor crop",  "CROP");
//        Log.d("Sensor fovX",  Math.toDegrees(sensorFovX_rad) + " degree");
//        Log.d("Sensor fovY",  Math.toDegrees(sensorFovY_rad) + " degree");
//        Log.d("Sensor Size",  sensorWidth_mm + " mm x " + sensorHeight_mm + " mm");
        
        // ZOOM
        
        double zoomRatio = zoomPercent / 100.0;
        
        sensorWidth_mm  /= zoomRatio;
        sensorHeight_mm /= zoomRatio;
        
//        Log.d("Zoom", zoomRatio + "x");
//        Log.d("Sensor fovX",  Math.toDegrees(sensorFovX_rad) + " degree");
//        Log.d("Sensor fovY",  Math.toDegrees(sensorFovY_rad) + " degree");
//        Log.d("Sensor Size",  sensorWidth_mm + " mm x " + sensorHeight_mm + " mm");
        
        double mm_in_pixel_x = previewWidth_px  / sensorWidth_mm;
        double mm_in_pixel_y = previewHeight_px / sensorHeight_mm;
        
        // Ideally x-y is the same, but get the average
        float f_px = (float)(f_mm * (mm_in_pixel_x + mm_in_pixel_y)/2);
        
        return f_px;
    }
    
    public static Camera.Size getPreviewSize_MaxFittingArea(int width, int height, Camera.Parameters parameters) {
        Camera.Size result = null;
        
        for (Camera.Size size : parameters.getSupportedPreviewSizes()) {
            if (size.width <= width && size.height <= height) {
                if (result == null) {
                    result = size;
                }
                else {
                    int resultArea = result.width * result.height;
                    int newArea    = size.width * size.height;

                    if (newArea > resultArea) {
                        result = size;
                    }
                }
            }
        }
        if(result == null) {
            Log.e(TAG, "Couldn't find preview size which fits the criterias");
        }

        return result;
    }
    
    public static Camera.Size getPreviewSize_MaxArea(Camera.Parameters parameters) {
        Camera.Size result = null;
        
        for (Camera.Size size : parameters.getSupportedPreviewSizes()) {
            if (result == null) {
                result = size;
            }
            else {
                int resultArea = result.width * result.height;
                int newArea    = size.width * size.height;

                if (newArea > resultArea) {
                    result = size;
                }
            }
        }
        if(result == null) {
            Log.e(TAG, "Couldn't find preview size which fits the criterias");
        }

        return result;
    }
    
    /**
     * Get the maximum area with the given aspect ratio: width / height
     * @param parameters
     * @param aspectRatio - camera's sensor's original aspect ratio (width/height)
     * @return valid camera preview size
     */
    public static Camera.Size getPreviewSize_MaxArea_Aspect(Camera.Parameters parameters, float aspectRatio) {
        Camera.Size result = null;
        Log.d(TAG, "getPreviewSize_MaxArea_Aspect: " + aspectRatio);
        
        float bestRatio = 0;
        for (Camera.Size size : parameters.getSupportedPreviewSizes()) {
            int w = size.width;
            int h = size.height;
            float r = w / (float) h;
            boolean isBestRatio = Math.abs(aspectRatio - r) <= Math.abs(aspectRatio - bestRatio) - 0.01;
            boolean isSameRatio = Math.abs(aspectRatio - r) <= Math.abs(aspectRatio - bestRatio) + 0.01;
            Log.d(TAG, "- "+ w + " x " + h + " (" + r + ")" + (isBestRatio ? "!" : isSameRatio ? "*" : ""));
            if (isBestRatio || isSameRatio) {
                bestRatio = r;
                if (result == null) {
                    result = size;
                }
                else {
                    int resultArea = result.width * result.height;
                    int newArea = size.width * size.height;
    
                    if (isBestRatio || newArea > resultArea) {
                        result = size;
                    }
                }
            }
        }
        if(result == null) {
            Log.e(TAG, "Couldn't find preview size which fits the criterias");
        } else {
            Log.d(TAG, "best: " + result.width + " x " + result.height);
        }

        return result;
    }
}
