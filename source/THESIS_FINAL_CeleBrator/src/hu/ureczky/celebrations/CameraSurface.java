package hu.ureczky.celebrations;

import android.content.Context;
import android.hardware.Camera;
import android.hardware.Camera.Parameters;
import android.hardware.Camera.PictureCallback;
import android.hardware.Camera.PreviewCallback;
import android.hardware.Camera.ShutterCallback;
import android.hardware.Camera.Size;
import android.location.Location;
import android.util.AttributeSet;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Toast;

import hu.ureczky.celebrations.State.Result;
import hu.ureczky.utils.CameraUtils;
import hu.ureczky.utils.MediaScannerHacker;
import hu.ureczky.utils.astro.Astronomy;
import hu.ureczky.utils.astro.distancecalculator.DistanceCalculator;
import hu.ureczky.utils.astro.distancecalculator.DistanceCalculator.ANGLE_TYPE;
import hu.ureczky.utils.astro.distancecalculator.DistanceCalculator.MODEL_TYPE;
import hu.ureczky.utils.astro.geomagneticfield.GeomagneticField;
import hu.ureczky.utils.astro.geomagneticfield.GeomagneticFieldFactory;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;

public class CameraSurface extends SurfaceView implements SurfaceHolder.Callback {
    
    private final static String TAG = "CameraSurface";
    
    private final static String DEG = "°";
    
    private Camera mCamera;
    private SurfaceHolder mPreviewHolder;
    private boolean inPreview;
    
    private SensorWatcher mSensorWatcher;
    private State mState;
        
    // = Environment.getExternalStorageDirectory() + "Android/data"+ getContext().getPackageName() + "files"
    private final File baseDir = CameraSurface.this.getContext().getExternalFilesDir(null); 
    
    public CameraSurface(Context context, State state) {
        super(context);
        init();
    }

    public CameraSurface(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public CameraSurface(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init();
    }

    public void init() {
        Log.v(TAG, "init");
        mState = State.getInstance();
        inPreview = false;
        mPreviewHolder = getHolder();
        mPreviewHolder.addCallback(this);
        mPreviewHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
    }
    
    public void onResume() {
        Log.v(TAG, "onResume");
        Log.v(TAG, "- inPreview: " + inPreview);
        if (!inPreview) {
            mCamera = CameraUtils.getCamera();
            Log.v(TAG, "- camera: " + ((mCamera==null) ? "null" : "opened"));
            init();
            inPreview = true;
        }
        
        // Bugfix for standby and recover (surfaceChanged() won't be called otherwise)
        dispatchWindowVisibilityChanged(View.VISIBLE);
    }
    
    public void onPause() {
        Log.v(TAG, "onPause");
        inPreview = false;
        if(mCamera != null) {
            mCamera.stopPreview();
            mCamera.setPreviewCallback(null);
            mCamera.release();  
            mCamera = null;
            mPreviewHolder.removeCallback(this);
            mPreviewHolder = null;
            dispatchWindowVisibilityChanged(View.GONE); // Bugfix, see above
        }
    }

    public void surfaceCreated(SurfaceHolder holder) {
        Log.v(TAG, "surfaceCreated");
        if(!inPreview) {
            Log.v(TAG, "- not in preview");
            return;
        }
        try {
            mCamera.setPreviewDisplay(mPreviewHolder);    
        } catch (Throwable t) {
            Log.e(TAG, "Exception in setPreviewDisplay()", t);
        }
    }
    
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        Log.v(TAG, "surfaceChanged: " + width + "x" + height);
        Log.v(TAG, "- camera: " + ((mCamera == null) ? "null" : "exists"));
        if(mCamera != null) {
            Camera.Parameters parameters = mCamera.getParameters();
            
            getCameraSensorParameters(parameters);
            
            // Camera and preview size
            setCameraAndPreviewSize(parameters, holder, width, height);
            
            // Exposure: minimum
            setExposureToMinimum(parameters);
            
            // Focus: infinity
            setFocusModeToInfinity(parameters);
            
            // TODO Restore saved zoom
            mState.mZoomIdx = 0;
            mState.mZoomPercent = 100;
                    
            // TODO Small Pic
            //List<Camera.Size> picSizes = parameters.getSupportedPictureSizes();
            //parameters.setPictureSize(800, 600);
            
            mCamera.setParameters(parameters);
            
            //TODO del
            Log.i(TAG, "Focus mode:" + mCamera.getParameters().getFocusMode());
            
            //mCamera.setPreviewCallback(previewCallBack);
            mCamera.startPreview();
            inPreview = true;
        }
    }
    
    public void surfaceDestroyed(SurfaceHolder holder) {
        Log.v(TAG, "surfaceDestroyed");
        onPause();
    }
    
    public void shoot() {
        mCamera.takePicture(shutterCallback, null, null, jpegCallback);
    }
    
    ShutterCallback shutterCallback = new ShutterCallback(){
        @Override
        public void onShutter() {
            Log.d("shoot", "onShutter");
            
            // Refresh time and sensor values
            mState.snapshot(mSensorWatcher);
            
            double azimuthDegMagn = mState.mOrientation[0];
            double elevationDeg   = mState.mOrientation[1];
            
            // Calculate the estimated position
            Location optLoc = Algorithm.calculate(mState.getLastResult());
            double optLatD = optLoc.getLatitude();
            double optLonD = optLoc.getLongitude();
                        
            boolean DEBUG = true; //TODO
            if(DEBUG)
            {                
                double currLatD = Astronomy.LAT_BUD_DEG;
                double currLonD = Astronomy.LON_BUD_DEG;
                
                DistanceCalculator DC_Ellipsoid = DistanceCalculator.create(MODEL_TYPE.ELLIPSOID, ANGLE_TYPE.DEGREE);
                double diffKm = (float) DC_Ellipsoid.getDistance(currLatD, currLonD, optLatD, optLonD) / 1000;
                
                Toast.makeText(CameraSurface.this.getContext(),
                        "Hiba:"  + diffKm               + "km\n" +
                        "DLat: " + (optLatD - currLatD) + DEG + "\n" +
                        "DLon: " + (optLonD - currLonD) + DEG + "\n",
                        Toast.LENGTH_LONG).show();
                                
                GeomagneticField gmf = GeomagneticFieldFactory.create(mState.mTimeStamp);
                gmf.setParameters((float)currLatD, (float)currLonD, 0 /*TODO altitude*/);                                
                float declinationD = gmf.getDeclination();
                
                Log.d("azimuth",      azimuthDegMagn + DEG );
                Log.d("elevation",    elevationDeg + DEG);
                Log.d("longitude",    "" + optLonD);
                Log.d("latitude",     "" + optLatD);
                Log.d("declination",  "" + declinationD);
            }            
        }
        
    };
    
//    PictureCallback rawCallback = new PictureCallback(){
//        @Override
//        public void onPictureTaken(byte[] data, Camera camera) {
//            Log.d("shoot", "onPictureTaken-raw:" + ((data==null)?"null":""+data.length));
//        }  
//    };
//    
//    PictureCallback postviewCallback = new PictureCallback(){
//        @Override
//        public void onPictureTaken(byte[] data, Camera camera) {
//            Log.d("shoot", "onPictureTaken-postview:" + ((data==null)?"null":""+data.length));
//        }  
//    };
    
    PictureCallback jpegCallback = new PictureCallback(){
        @Override
        public void onPictureTaken(byte[] data, Camera camera) {
            Log.d("shoot", "onPictureTaken-jpeg:" + ((data==null)?"null":""+data.length));
            
            // Save the image JPEG data to the SD card
            try {
                //Create directory
                File picDir = getSubDir();
                
                //Create files
                long timestamp = mState.mTimeStamp;
                File pictureFile = new File(picDir, timestamp + ".jpg");
                pictureFile.createNewFile();
                File metadataFile = new File(picDir, timestamp + ".json");
                metadataFile.createNewFile();
                
                FileOutputStream pictureStream = new FileOutputStream(pictureFile);
                pictureStream.write(data);
                pictureStream.close();
                                
                // Write
                mState.write(metadataFile);
                
                // Hack
                MediaScannerHacker.scan(CameraSurface.this.getContext(), pictureFile, metadataFile);
                
            } catch (FileNotFoundException e) {
                Log.e(TAG, "File Not Found", e);
            } catch (IOException e) {
                Log.e(TAG, "IO Exception", e);
            }
            
            mCamera.startPreview();
        }  

    };
        
    PreviewCallback previewCallBack = new PreviewCallback() {
        @Override
        public void onPreviewFrame(byte[] data, Camera camera) {
            Log.d(TAG, "onPreviewFrame:" + ((data==null) ? "null" : ""+data.length));
        }
    };

    public void setsensorWatcher(SensorWatcher sensorWatcher) {
        mSensorWatcher = sensorWatcher;
    }
       
    // @param percent: [0..100]
    public void changeZoomPercent(int percent) {
        if(mCamera != null) {
            Camera.Parameters parameters = mCamera.getParameters();
            if(parameters.isZoomSupported())
            {
                int zoomMaxIdx = parameters.getMaxZoom();
                int zoomIdx = (int) Math.round(zoomMaxIdx * percent / 100.0);
                int newZoomPercent = parameters.getZoomRatios().get(zoomIdx);
                parameters.setZoom(zoomIdx);
                mState.mZoomIdx = zoomIdx;
                mState.mZoomPercent = newZoomPercent;
                mState.mFocalLength_px = CameraUtils.getFocalLengthInPx(mState.mFocalLength_mm, mState.mFovXRad, mState.mFovYRad, mState.mPreviewWidth, mState.mPreviewHeight, mState.mZoomPercent);
                mCamera.setParameters(parameters);
            }
        }
    }
    
    private File getSubDir() {
        File subDir = new File(baseDir, mState.mTaskType.subDir);
        
        if(!subDir.exists()) {
            subDir.mkdirs();
        }
        return subDir;
    }
    
    private void setCameraAndPreviewSize(Parameters parameters, SurfaceHolder holder, int maxWidth, int maxHeight) {
        
        // Preview size: best fit
        //Camera.Size size = CameraUtils.getPreviewSize_MaxFittingArea(maxWidth, maxHeight, parameters);
        //Camera.Size size = CameraUtils.getPreviewSize_MaxArea(parameters);
        Camera.Size size = CameraUtils.getPreviewSize_MaxArea_Aspect(parameters, mState.mSensorAspectRatio);        
        if (size == null) {
            Log.e(TAG, "Unable to get the desired preview size");
            return;
        }
        
        parameters.setPreviewSize(size.width, size.height);
        mState.mPreviewWidth  = size.width;
        mState.mPreviewHeight = size.height;
        mState.mFocalLength_px = CameraUtils.getFocalLengthInPx(mState.mFocalLength_mm, mState.mFovXRad, mState.mFovYRad, mState.mPreviewWidth, mState.mPreviewHeight, 100);
        Log.d(TAG, "- Preview size set to " + size.width + "x" + size.height);
        
        // Resize surface according to that (to maintain the aspect ratio)
        double resizeX = maxWidth  / (double) size.width;
        double resizeY = maxHeight / (double) size.height;
        double resize = Math.min(resizeX, resizeY);
                    
        int newWidth  = (int) (resize * size.width);
        int newHeight = (int) (resize * size.height);
        
        mPreviewHolder.setFixedSize(newWidth, newHeight);
    }
    
    private void setExposureToMinimum(Parameters parameters) {
        int minExp = parameters.getMinExposureCompensation();
        int maxExp = parameters.getMaxExposureCompensation();
        float stepExp = parameters.getExposureCompensationStep();
        Log.d(TAG, "- Supported exposures: " + minExp + ".." + maxExp + "(step " + stepExp + ")");
        if(minExp != 0) {
            parameters.setExposureCompensation(minExp);
            mState.mExposure = minExp;
            Log.d(TAG, "- Exposure set to " + minExp);
        }
    }
    
    private void setFocusModeToInfinity(Parameters parameters) {
        List<String> focusModes = parameters.getSupportedFocusModes();
        Log.d(TAG, "Supported focus modes:" + focusModes);
        for(String focusMode: focusModes)
        {
            Log.d(TAG, focusMode);
            if(focusMode.equals(Parameters.FOCUS_MODE_INFINITY)) {
                parameters.setFocusMode(Parameters.FOCUS_MODE_INFINITY);
            }
        }
    }
    
    private void getCameraSensorParameters(Parameters parameters) {
        
        // Focal length
        float f_mm = parameters.getFocalLength();
        mState.mFocalLength_mm = f_mm;
        
        // View Angles
        mState.mFovXDeg = parameters.getHorizontalViewAngle();
        mState.mFovYDeg = parameters.getVerticalViewAngle();
        mState.mFovXRad = (float) Math.toRadians(mState.mFovXDeg);
        mState.mFovYRad = (float) Math.toRadians(mState.mFovYDeg);
                
        float w_mm   = (float) (2 * f_mm * Math.tan(mState.mFovXRad / 2));
        float h_mm   = (float) (2 * f_mm * Math.tan(mState.mFovYRad / 2));
        
        mState.mSensorAspectRatio = w_mm / h_mm;
        
        // LOG
        Log.d("Focal length",  "" + f_mm + " mm");
        Log.d("FovX",          "" + mState.mFovXDeg);
        Log.d("FovY",          "" + mState.mFovYDeg);
        Log.d("Sensor Size",   w_mm + " mm x " + h_mm + " mm");
        Log.d("Sensor Aspect", "" + mState.mSensorAspectRatio);
    }
    
}
