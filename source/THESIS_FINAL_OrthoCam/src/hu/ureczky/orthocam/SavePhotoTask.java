package hu.ureczky.orthocam;

import android.content.Context;
import android.content.Intent;
import android.hardware.Camera;
import android.hardware.Camera.PictureCallback;
import android.hardware.Camera.ShutterCallback;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Environment;
import android.util.Log;
import android.widget.Toast;

import hu.ureczky.utils.MediaScannerHacker;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

public class SavePhotoTask
    extends AsyncTask<Boolean, Void, Void> 
    implements PictureCallback
{
    
    private static final String TAG = "SavePhotoTask";
    private Camera mCamera;
    private Context mContext;
    private SensorListener mSensorListener;
    private double mElevationDeg;
    
    public SavePhotoTask(Context ctx, Camera camera, SensorListener sensorListener){
        mCamera = camera;
        mContext = ctx;
        mSensorListener = sensorListener;
    }
    
    @Override
    protected void onPreExecute() {        
        Toast.makeText(mContext, "Saving picture", Toast.LENGTH_SHORT).show();
    }
    
    @Override
    protected Void doInBackground(Boolean... params) {
        mCamera.takePicture(shutterCallback, rawCallback, this);
        return null;
    }
    
    @Override
    protected void onPostExecute(final Void success) {
        Toast.makeText(mContext, "finished", Toast.LENGTH_SHORT).show();
    }
    
    @Override
    public void onPictureTaken(byte[] data, Camera camera) {       
        try {
            FileOutputStream outStream = null;
            try {
                // write to local sandbox file system
                // outStream =
                // CameraDemo.this.openFileOutput(String.format("%d.jpg",
                // System.currentTimeMillis()), 0);
                // Or write to sdcard
                
                
                
                String fileName = String.format("%d_%f.jpg", System.currentTimeMillis(), mElevationDeg);
                
                
                //TODO
                
                //File dir = new File(mContext.getFilesDir(),"RawPictures");
                //File dir = mContext.getExternalFilesDir(null);
                File dir = Environment.getExternalStorageDirectory();
                //File dir = new File(new File(new File(new File(Environment.getExternalStorageDirectory(),"Android"),"data"),mContext.getPackageName()),"RawPictures");
                //File dir = new File(mContext.getExternalFilesDir(Environment.DIRECTORY_PICTURES), "RawPictures");
                
                dir = new File(dir, "OrthoCam");
                if(!dir.exists())
                    dir.mkdirs();
                
                File file = new File(dir, fileName);
                
                outStream = new FileOutputStream(file);
                outStream.write(data);
                outStream.close();
                
                // Hack to scan it
                MediaScannerHacker.scan(mContext, file);
                
                Log.d(TAG, "onPictureTaken to " + file.getPath() + "- wrote bytes: " + data.length);
                Toast.makeText(mContext, "onPictureTaken to " + file.getPath(), Toast.LENGTH_LONG).show();
            } catch (FileNotFoundException e) {
                Log.e(TAG, "FileNotFound");
                e.printStackTrace();
            } catch (IOException e) {
                Log.e(TAG, "IOException");
                e.printStackTrace();
            } catch (Exception e) {
                Log.e(TAG, "Unknown Exception");
                e.printStackTrace();
            } finally {
            }
            Log.d(TAG, "onPictureTaken - jpeg");
        } catch (final Exception e) {
        }
    }
    
    ShutterCallback shutterCallback = new ShutterCallback() {
        public void onShutter() {
            mElevationDeg = Math.toDegrees(mSensorListener.getElevation());
            Log.d(TAG, "onShutter - " + mElevationDeg);
        }
    };

    PictureCallback rawCallback = new PictureCallback() {
        public void onPictureTaken(byte[] _data, Camera _camera) {
            Log.d(TAG, "onPictureTaken - raw");
        }
    };
}