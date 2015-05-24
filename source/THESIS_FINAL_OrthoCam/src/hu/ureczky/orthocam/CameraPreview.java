package hu.ureczky.orthocam;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.hardware.Camera.Area;
import android.hardware.Camera.AutoFocusCallback;
import android.hardware.Camera.AutoFocusMoveCallback;
import android.hardware.Camera.Parameters;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.TextureView;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import hu.ureczky.utils.AndroidUtils;
import hu.ureczky.utils.CameraUtils;
import hu.ureczky.utils.TransformUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedList;
import java.util.List;

@SuppressLint("NewApi")
public class CameraPreview extends Activity implements TextureView.SurfaceTextureListener {    
    private Camera mCamera;
    
    
    private TextureView mTextureView;
    private TextView mLabelFPS;
    private TextView mLabelElevation;
    private LinearLayout mLayout;
    private View myCanvas;
    
    private SharedPreferences mPreferences;
    SensorListener mSensorListener;
    
    private float fovX;
    float fovY;
    
    // FPS
    private boolean fpsEnabled = Settings.DEFAULT_FPS_ENABLED;
    private Deque<Long> times = new LinkedList<Long>();
    private final int TIMES_SIZE = 10;
    private float fps = 0;
    private long currTime;
    
    // Elevation
    private boolean elevationEnabled = Settings.DEFAULT_ELEVATION_ENABLED;
    
    private Timey mTimey;
    
    private boolean autoZoomCorrection = false;
    
    float f_px;
    
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d("Lifecycles", "CameraPreview-onCreate");
        
        AndroidUtils.setFullScreen(this);
        
        setContentView(R.layout.main);
        mTextureView = (TextureView) findViewById(R.id.textureView);
        mLayout = (LinearLayout) findViewById(R.id.myLayout);
        myCanvas = new ARView(this);
        mLayout.addView(myCanvas);
        mLabelFPS = (TextView) findViewById(R.id.fps);
        mLabelElevation = (TextView) findViewById(R.id.elevation);
        
        mSensorListener = new SensorListener(this);
        mTimey = new Timey(this);
    }
    
    @Override
    protected void onPause() {
        super.onPause();
        Log.d("Lifecycles", "CameraPreview-onPause");
        
        if(mTextureView != null) {
            mTextureView.setSurfaceTextureListener(null);
        }
        releaseCamera();
        
        if(mSensorListener != null)
            mSensorListener.pause();
    }
    
    private void releaseCamera() {
        if (mCamera != null) {
            mCamera.stopPreview();
            mCamera.release();
            mCamera = null;
        }
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        Log.d("Lifecycles", "CameraPreview-onResume");
        
        // Check settings
        if(Settings.isOutDated(this)) {
            Log.d("Lifecycles", "Call: settings");
            startActivity(new Intent(this, Settings.class));
            return;
        }
        
        mPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        
        // FPS visibility
        fpsEnabled = mPreferences.getBoolean(Settings.KEY_FPS_ENABLED, Settings.DEFAULT_FPS_ENABLED);
        mLabelFPS.setVisibility(fpsEnabled ? View.VISIBLE : View.GONE);
        
        // Elevation visibility
        elevationEnabled = mPreferences.getBoolean(Settings.KEY_ELEVATION_ENABLED, Settings.DEFAULT_ELEVATION_ENABLED);
        mLabelElevation.setVisibility(elevationEnabled ? View.VISIBLE : View.GONE);
        
        // Animation time
        mTimey.init(mPreferences);
        
        // SENSOR
        mSensorListener.listen();
        
        // TextureView
        mTextureView.setSurfaceTextureListener(this);
        
        // Bugfix for standby and recover (onSurfaceTextureAvailable won't be called otherwise)
        if (mTextureView.isAvailable()) {
            onSurfaceTextureAvailable(mTextureView.getSurfaceTexture(), mTextureView.getWidth(), mTextureView.getHeight());
        }
    }
    
    // Listener set from XML
    public void settingsClick(View v){
        Log.d("Lifecycles", "Click: settings");
        startActivity(new Intent(this, Settings.class));
    }
    
    // Listener set from XML
    public void orthoClick(View v){
        Log.d("Lifecycles", "Click: ortho");
        mTimey.reverse();
        
        //TODO icon change?
    }
    
    // Listener set from XML
    public void zoomClick(View v){
        Log.d("Lifecycles", "Click: Zoom");
        autoZoomCorrection ^= true;
    }
    
    // Listener set from XML
    public void infoClick(View v){
        Log.d("Lifecycles", "Click: Info");
        startActivity(new Intent(this, Infos.class));
    }
       
    // Listener set from XML
    public void shootClick(View v){
        Log.d("Lifecycles", "Click: Shoot");
        print(mCamera);
        
        new SavePhotoTask(CameraPreview.this, mCamera, mSensorListener).execute();
        //DEBUG_focus();
        
    }
    
    //dontneed, just debug
    private void DEBUG_focus() {
        Parameters p = mCamera.getParameters();
        int areas = p.getMaxNumFocusAreas();
        Log.d("Camera", "Areas: " + areas);
        String fm = p.getFocusMode();
        Log.d("Camera", "Focus Mode: " + fm);
        List<String> fms = p.getSupportedFocusModes();
        Log.d("Camera", "Supported focus Modes: " + fms);
        if(areas>0){
            List<Area> focusAreas = new ArrayList<Area>();
            Area center = new Area(new Rect(-10,-10,10,10),1000); //TOPLEFT=(-1000,-1000), RIGHTBOTTOM=(1000,1000), Weight=[1..1000]
            focusAreas.add(center);
            p.setFocusAreas(focusAreas);
            p.setFocusMode(Parameters.FOCUS_MODE_AUTO);
            mCamera.setParameters(p);
        }
        mCamera.autoFocus(mAutoFocusCallback);
    }
    
    private final AutoFocusMoveCallback mAutoFocusMoveCallback = new AutoFocusMoveCallback() {
        @Override
        public void onAutoFocusMoving(boolean start, Camera camera) {
            Log.d("Camera life", "Focus moving:"+start);            
            print(camera);
        }
    };
    
    private AutoFocusCallback mAutoFocusCallback = new AutoFocusCallback(){
        @Override
        public void onAutoFocus(boolean success, Camera camera) {
            Log.d("Camera life", "Focus callback, success:" + success);
            print(camera);
        }
    };
    
    private void print(Camera camera){
        Parameters p = camera.getParameters();
        String focusmode = p.getFocusMode();
        Log.d("focusmode:", focusmode);
        float fovx = p.getHorizontalViewAngle();
        float f = p.getFocalLength();
        Log.d("f:", ""+f);
        Log.d("fovx:", ""+fovx);
        float[] dists = new float[3];
        p.getFocusDistances(dists);
        Log.d("d[NEAR]    : ", ""+dists[Parameters.FOCUS_DISTANCE_NEAR_INDEX]);
        Log.d("d[OPTIMAL] : ", ""+dists[Parameters.FOCUS_DISTANCE_OPTIMAL_INDEX]);
        Log.d("d[FAR]     : ", ""+dists[Parameters.FOCUS_DISTANCE_FAR_INDEX]);
    }
    
    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
        Log.d("Lifecycles", "onSurfaceTextureAvailable");
        
        // Get the appropriate camera
        int camId = Integer.valueOf(mPreferences.getString(Settings.KEY_CAMERA_ID, "0"));
        
        mCamera = Camera.open(camId);
        CameraUtils.setCameraDisplayOrientation(this, camId, mCamera);

        // Get parameters
        Parameters parameters = mCamera.getParameters();
        
        // Set Resolution
        String prevResolution = mPreferences.getString(Settings.KEY_PREVIEW_RESOLUTION, "0x0 (0:0)");
        String[] prevDimensions = prevResolution.split("[ x]");
        int prevWidth = Integer.valueOf(prevDimensions[0]);
        int prevHeight = Integer.valueOf(prevDimensions[1]);
        parameters.setPreviewSize(prevWidth, prevHeight);
          
        // Set Focus Mode
        String focusMode = mPreferences.getString(Settings.KEY_FOCUS_MODE, Settings.DEFAULT_FOCUS_MODE);
        parameters.setFocusMode(focusMode);
        if(focusMode == Parameters.FOCUS_MODE_CONTINUOUS_PICTURE || focusMode == Parameters.FOCUS_MODE_CONTINUOUS_VIDEO){
            mCamera.setAutoFocusMoveCallback(mAutoFocusMoveCallback);
        }
          
        // Set FPS
        String fpsMode = mPreferences.getString(Settings.KEY_FPS_MODE, "0");
        String[] fpsInterval = fpsMode.split("#");
        int minFps = Integer.valueOf(fpsInterval[0]);
        int maxFps = Integer.valueOf(fpsInterval[1]);
        parameters.setPreviewFpsRange(minFps, maxFps);
          
        mCamera.setParameters(parameters);
        parameters = mCamera.getParameters();
        
        float f_mm = parameters.getFocalLength();
        float fovx = (float) Math.toRadians(parameters.getHorizontalViewAngle());
        float fovy = (float) Math.toRadians(parameters.getVerticalViewAngle());
        f_px = CameraUtils.getFocalLengthInPx(f_mm, fovx, fovy, prevWidth, prevHeight, 100);
          
        RelativeLayout.LayoutParams lp = (RelativeLayout.LayoutParams) mTextureView.getLayoutParams();
        lp.height = prevHeight;
        lp.width  = prevWidth;
        mTextureView.setLayoutParams(lp);
        
        //surface.setDefaultBufferSize(prevWidth, prevHeight); //TODO kell-e

        try {
            mCamera.setPreviewTexture(surface);
            mCamera.startPreview();
        } catch (IOException e) {
            e.printStackTrace();
        }
            
    }
    
    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
        Log.d("Lifecycles", "onSurfaceTextureSizeChanged");
        // Ignored, Camera does all the work for us
    }

    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
        Log.d("Lifecycles", "onSurfaceTextureDestroyed");
        releaseCamera();
        return true;
    }

    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surface) {
        //Log.v("Lifecycles", "onSurfaceTextureUpdated");
        
        //Bitmap b = mTextureView.getBitmap();
        
        // Invoked every time there's a new Camera preview frame
        
        // Notes on calculation speed
        // If the calculation is slow, FPS is simply decreasing, so the method seems to be synchronised (not called repeatedly until it hasn't finished)
        // However its blocking the UI until the next refresh
        // Although its only observed if calculation took more than 0.1 s, but currently the matrix calculation is way too fast.
        // Test1: Wait for 0.1 sec, FPS decrease to 8.5 fps from the set (15, 24, 30) value.
        // Test2: Wait for 1 sec, UI is blocking for 1 sec.
        /*
        long t0 = SystemClock.elapsedRealtime();
        long t_curr;
        do{
            t_curr = SystemClock.elapsedRealtime();
        } while(t_curr - t0 < 100);
        */

        // FPS
        if(fpsEnabled) {
            currTime = Timey.getTime();
            times.push(currTime);
            if(times.size() > TIMES_SIZE){
                times.removeLast();
            }
            fps = (times.size() < 2) ? 0
                : Timey.SECOND * (times.size() - 1) / (float) (times.getFirst() - times.getLast());
            mLabelFPS.setText(String.format("FPS: %.1f", fps));
        }
        if(elevationEnabled) {
            double elevationDeg = mSensorListener.getElevationDeg();
            mLabelElevation.setText(String.format("Elevation: %.2f", elevationDeg));
        }
        
        int w = mTextureView.getMeasuredWidth();
        int h = mTextureView.getMeasuredHeight();
        //Size size = mCamera.getParameters().getPreviewSize(); //TODO jo méretek? nem keveredik a két méret? (preview vs UI element)
        //int w0 = size.width;
        //int h0 = size.height;
        Matrix transform = getMagicTransformation(w, h, f_px, false);
        
        // Set the transformations origo to the picture's midpoint //TODO ez így jó-e, nem keveredik-e a kétféle középpont fogalom??(preview vs. textureview)
        remap(transform, w, h);
        
        // Transformation without tearing effect
        //setTransformWithoutInvalidate(mTextureView, transform);
        mTextureView.setTransform(transform);
        
        //RectF movie = new RectF(0, 0, 1, 1);
        
        //  zoom to upper left corner
         //RectF zoom_region = new RectF(0, 0, .5f, .5f);
    
         //Matrix transformm = new Matrix();
         //transformm.setRectToRect( zoom_region, movie, ScaleToFit.CENTER);
         //transformm.setRectToRect( zoom_region, movie, ScaleToFit.START);
         //transformm.setRectToRect( zoom_region, movie, ScaleToFit.END);
         //transformm.setRectToRect( zoom_region, movie, ScaleToFit.FILL);
         //mTextureView.setX(200);
         //Rect r = mTextureView.getClipBounds();
         //mTextureView.setClipBounds(new Rect(0,0,10,10));
         //mTextureView.setTransform(transformm);
        
        myCanvas.invalidate();
        
    }
    
    public Matrix getMagicTransformation(int width, int height, float f, boolean reverse) {
        
        //Magic transform
        Matrix magicTransform;        
        float transformationPhase = mTimey.getTransformationPhase();
        if(reverse) transformationPhase = 1 - transformationPhase;
        if(transformationPhase==0) {
            Matrix unit = new Matrix();
            magicTransform = unit;
        }else{
            Matrix orthoTransform = mSensorListener.getOrthoView(f);
            if(transformationPhase==1) {
                magicTransform = orthoTransform;
            }else{
                Matrix unit = new Matrix();
                magicTransform = TransformUtils.interpolateLinear(unit, orthoTransform, transformationPhase);
            }
        }

        // Auto Zoom correction
        if(autoZoomCorrection) {
            float scaleCorrection = mSensorListener.getZoomCorrection();
            magicTransform.postScale(scaleCorrection, scaleCorrection);
        }
               
        return magicTransform;
    }
    
    void remap(Matrix M, int w, int h) {
        Matrix remapToDevice   = TransformUtils.remapToDevice(w, h);
        Matrix remapFromDevice = TransformUtils.remapFromDevice(w, h);
        
        M.preConcat(remapToDevice);
        M.postConcat(remapFromDevice);
    }
    
}