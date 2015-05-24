package hu.ureczky.orthocam;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.hardware.Camera;
import android.hardware.Camera.CameraInfo;
import android.hardware.Camera.Parameters;
import android.hardware.Camera.Size;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceActivity;
import android.preference.PreferenceCategory;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

public class Settings extends PreferenceActivity implements OnPreferenceChangeListener {
    
    public static long VERSION = 12; 
    
    public static String KEY_VERSION            = "settings_version";
    public static String KEY_CAMERA_ID          = "camera_id";
    public static String KEY_FPS_ENABLED        = "show_fps";
    public static String KEY_ELEVATION_ENABLED  = "show_elevation";
    public static String KEY_ANIMATION_ENABLED  = "enable_animate";
    public static String KEY_FPS_MODE           = "fps_value";
    public static String KEY_FOCUS_MODE         = "focus_mode";
    public static String KEY_PREVIEW_RESOLUTION = "list_preview_resolution";
    public static String KEY_SENSOR_MODE        = "sensor_mode";
    
    public static boolean DEFAULT_FPS_ENABLED       = false;
    public static boolean DEFAULT_ELEVATION_ENABLED = false;
    public static boolean DEFAULT_ANIMATION_ENABLED = true;
    public static String  DEFAULT_FOCUS_MODE        = Parameters.FOCUS_MODE_FIXED;

    public static boolean isOutDated(Context context) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        long actVersion = preferences.getLong(KEY_VERSION, 0);
        boolean outDated = (actVersion < VERSION);
        if(outDated){
            Editor editor = preferences.edit();
            editor.clear();
            editor.commit();
            //TODO innen hívni intent-et??
        }
        return outDated;
    }
    
    SharedPreferences mPreferences;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d("Lifecycles", "Settings-onCreate");

        mPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        
        setPreferenceScreen(createPreferenceHierarchy());
        
        SharedPreferences.Editor editor = mPreferences.edit();
        editor.putLong(KEY_VERSION, VERSION);
        editor.commit();
    }

    private PreferenceScreen createPreferenceHierarchy() {
        // Root
        PreferenceScreen root = getPreferenceManager().createPreferenceScreen(this);
        
        // GENERAL Settings
        PreferenceCategory generalSettings = new PreferenceCategory(this);
        generalSettings.setTitle(R.string.general_settings);
        root.addPreference(generalSettings);      
        
        // FPS
        CheckBoxPreference checkboxFPS = new CheckBoxPreference(this);
        checkboxFPS.setKey(KEY_FPS_ENABLED);
        checkboxFPS.setTitle(R.string.title_fps);
        checkboxFPS.setSummary(R.string.summary_fps);
        checkboxFPS.setDefaultValue(DEFAULT_FPS_ENABLED);
        generalSettings.addPreference(checkboxFPS);
        
        // Elevation
        CheckBoxPreference checkboxElevation = new CheckBoxPreference(this);
        checkboxElevation.setKey(KEY_ELEVATION_ENABLED);
        checkboxElevation.setTitle(R.string.title_elevation);
        checkboxElevation.setSummary(R.string.summary_elevation);
        checkboxElevation.setDefaultValue(DEFAULT_ELEVATION_ENABLED);
        generalSettings.addPreference(checkboxElevation);
        
        // ANIMATION
        CheckBoxPreference checkboxAnimation = new CheckBoxPreference(this);
        checkboxAnimation.setKey(KEY_ANIMATION_ENABLED);
        checkboxAnimation.setTitle(R.string.title_animation);
        checkboxAnimation.setSummary(R.string.summary_animation);
        checkboxAnimation.setDefaultValue(DEFAULT_ANIMATION_ENABLED);
        generalSettings.addPreference(checkboxAnimation);
        
        // SENSORS Selector
        
        SensorManager manager = (SensorManager)getSystemService(SENSOR_SERVICE);       
        List<Sensor> sensors = manager.getSensorList(Sensor.TYPE_ALL);
        List<Sensor> eligibleSensors = new ArrayList<Sensor>();
        for(Sensor sensor: sensors){
            int type = sensor.getType();
            switch(type){
                case Sensor.TYPE_ACCELEROMETER:
                case Sensor.TYPE_GRAVITY:
                case Sensor.TYPE_ROTATION_VECTOR:
                case Sensor.TYPE_GAME_ROTATION_VECTOR:
                case Sensor.TYPE_GEOMAGNETIC_ROTATION_VECTOR:
                case Sensor.TYPE_ORIENTATION:
                    eligibleSensors.add(sensor);
                    break;
                case Sensor.TYPE_MAGNETIC_FIELD:
                case Sensor.TYPE_MAGNETIC_FIELD_UNCALIBRATED:
                default:
                    break;
            }
        }
        
        String[] sensorModesArray = new String[eligibleSensors.size()];
        String[] sensorValuesArray = new String[eligibleSensors.size()];
        int defaultSensorIdx = 0;
        for(int i = 0; i < eligibleSensors.size(); i++){
            Sensor sensor = eligibleSensors.get(i);
            int type = sensor.getType();
            String name = sensor.getName();
            String vendor = sensor.getVendor();
            int version = sensor.getVersion();
            if(type==Sensor.TYPE_GRAVITY) defaultSensorIdx = i;
            sensorModesArray[i] = name + " (" + vendor + ", v." + version + ")";
            sensorValuesArray[i] = type + "#" + name + "#" + vendor + "#" + version;
        }
        
        ListPreference sensorModePref = new ListPreference(this);
        sensorModePref.setEntries(sensorModesArray);
        sensorModePref.setEntryValues(sensorValuesArray);
        sensorModePref.setDialogTitle(R.string.title_select_sensor);
        sensorModePref.setKey(KEY_SENSOR_MODE);
        sensorModePref.setTitle(R.string.title_sensor_mode);
        sensorModePref.setSummary(R.string.summary_sensor_mode);
        sensorModePref.setDefaultValue(sensorValuesArray[defaultSensorIdx]);
        generalSettings.addPreference(sensorModePref);

        // CAMERA Settings
        PreferenceCategory cameraSettings = new PreferenceCategory(this);
        cameraSettings.setTitle(R.string.camera_settings);
        root.addPreference(cameraSettings);
        
        // CAMERA Selector
        ListPreference cameraPref = new ListPreference(this);

        int numberOfCameras = Camera.getNumberOfCameras(); // API 9
        String[] cameraTypes = new String[numberOfCameras];
        String[] cameraIds = new String[numberOfCameras];
        for(int cameraId = 0; cameraId < numberOfCameras; cameraId++){
            CameraInfo cameraInfo = new CameraInfo();
            Camera.getCameraInfo(cameraId, cameraInfo);
            
            switch(cameraInfo.facing){
                case CameraInfo.CAMERA_FACING_BACK:
                    cameraTypes[cameraId] = "Back"; //TODO res-be
                    break;
                case CameraInfo.CAMERA_FACING_FRONT:
                    cameraTypes[cameraId] = "Front"; //TODO res-be
                    break;
                default:
                    cameraTypes[cameraId] = "Unknown facing";
                    break;
            }
            cameraIds[cameraId] = String.valueOf(cameraId);
        }
        
        cameraPref.setEntries(cameraTypes);
        cameraPref.setEntryValues(cameraIds);
        cameraPref.setDefaultValue("0");
        cameraPref.setDialogTitle(R.string.title_select_camera);
        cameraPref.setKey(KEY_CAMERA_ID);
        cameraPref.setTitle(R.string.title_camera);
        cameraPref.setSummary(R.string.summary_camera);
        cameraPref.setOnPreferenceChangeListener(this);
        cameraSettings.addPreference(cameraPref);       
                
        // PREVIEW RESOLUTION Selector
        
        int selectedCamera = Integer.valueOf(mPreferences.getString(KEY_CAMERA_ID, "0"));
        Camera camera = Camera.open(selectedCamera);
        List<Size> previewResolutions = camera.getParameters().getSupportedPreviewSizes();
        camera.release();
        
        Camera.Size biggestSize = getBiggestPreviewSize(previewResolutions);
        String [] previewResolutionsArray = new String[previewResolutions.size()];
        for(int i = 0; i < previewResolutions.size(); i++){
            previewResolutionsArray[i] = toString(previewResolutions.get(i));
        }
                
        ListPreference prevResolutionPref = new ListPreference(this);
        prevResolutionPref.setEntries(previewResolutionsArray);
        prevResolutionPref.setEntryValues(previewResolutionsArray);
        prevResolutionPref.setDialogTitle(R.string.title_select_preview_resolution);
        prevResolutionPref.setKey(KEY_PREVIEW_RESOLUTION);
        prevResolutionPref.setTitle(R.string.title_preview_resolution);
        prevResolutionPref.setSummary(R.string.summary_preview_resolution);
        prevResolutionPref.setDefaultValue(toString(biggestSize));
        cameraSettings.addPreference(prevResolutionPref);

        // FOCUS MODE Selector
        
        selectedCamera = Integer.valueOf(mPreferences.getString(KEY_CAMERA_ID, "0"));
        camera = Camera.open(selectedCamera);
        List<String> focusModes = camera.getParameters().getSupportedFocusModes();
        camera.release();
        String [] focusModesArray = focusModes.toArray(new String[focusModes.size()]);
        
        ListPreference focusModesPref = new ListPreference(this);
        focusModesPref.setEntries(focusModesArray);
        focusModesPref.setEntryValues(focusModesArray);
        focusModesPref.setDialogTitle(R.string.title_select_focus_mode);
        focusModesPref.setKey(KEY_FOCUS_MODE);
        focusModesPref.setTitle(R.string.title_focus_mode);
        focusModesPref.setSummary(R.string.summary_focus_mode);
        focusModesPref.setDefaultValue(focusModesArray[0]);
        cameraSettings.addPreference(focusModesPref);
        
        // FPS Selector
        
        selectedCamera = Integer.valueOf(mPreferences.getString(KEY_CAMERA_ID, "0"));
        camera = Camera.open(selectedCamera);
        List<int[]> fpsModes = camera.getParameters().getSupportedPreviewFpsRange();
        camera.release();
        String [] fpsModesArray = new String[fpsModes.size()];
        String [] fpsValuesArray = new String[fpsModes.size()];
        for(int i=0; i<fpsModes.size(); i++){
            int[] fpsMode = fpsModes.get(i);
            int minFps = fpsMode[0];
            int maxFps = fpsMode[1];
            if(minFps == maxFps){
                fpsModesArray[i] = String.format("%.1f [fps]", minFps/(float)1000);
            }else{
                fpsModesArray[i] = String.format("%.1f - %.1f [fps]", minFps/(float)1000, maxFps/(float)1000);
            }
            
            fpsValuesArray[i] = minFps + "#" + maxFps;
        }
        
        ListPreference fpsModesPref = new ListPreference(this);
        fpsModesPref.setEntries(fpsModesArray);
        fpsModesPref.setEntryValues(fpsValuesArray);
        fpsModesPref.setDialogTitle(R.string.title_select_fps_mode);
        fpsModesPref.setKey(KEY_FPS_MODE);
        fpsModesPref.setTitle(R.string.title_fps_mode);
        fpsModesPref.setSummary(R.string.summary_fps_mode);
        fpsModesPref.setDefaultValue(fpsValuesArray[0]);
        cameraSettings.addPreference(fpsModesPref);
        
        // TODO check integrity
        
        return root;
    }
    
    private Camera.Size getBiggestPreviewSize(List<Size> supportedPreviewSizes) {
        Camera.Size result = null;
        int resultArea = 0;
        
        for (Camera.Size size : supportedPreviewSizes) {
            int newArea = size.width * size.height;
            if (newArea > resultArea) {
                result = size;
                resultArea = newArea;
            }
        }

        return(result);
    }

    //TODO egyedi legyen
    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        Log.d("debug", "Camera changed");
        Editor editor = mPreferences.edit();
        editor.remove(KEY_FOCUS_MODE);
        editor.remove(KEY_PREVIEW_RESOLUTION);
        editor.putString(KEY_CAMERA_ID, newValue.toString());
        editor.commit();
        setPreferenceScreen(createPreferenceHierarchy());
        return false;
    }
    
    private String toString(Camera.Size size){
        int w = size.width;
        int h = size.height;
        
        int gcd = MathUtil.gcd(w,h);
        int ratio_width = w / gcd;
        int ratio_height = h / gcd;
        
        return w + "x" + h + " (" + ratio_width + ":" + ratio_height + ")";
    }
}
