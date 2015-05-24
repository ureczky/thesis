package hu.ureczky.celebrations.activities;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.util.Log;
import android.view.Display;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;

import hu.ureczky.celebrations.ARView;
import hu.ureczky.celebrations.CameraSurface;
import hu.ureczky.celebrations.R;
import hu.ureczky.celebrations.SensorWatcher;
import hu.ureczky.celebrations.State;
import hu.ureczky.celebrations.TaskType;
import hu.ureczky.utils.AndroidUtils;

public class ARActivity extends Activity{
    
    final static String TAG = "ARActivity";
    
    private State mState;
    private SensorWatcher mSensorWatcher;
    
    // Views
    private CameraSurface mCameraPreview;
	private ImageView mSettingsBtn;
	private TextView mHistoryCounter;
	private SeekBar mZoomBar;
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        AndroidUtils.setFullScreen(this);
        
        mState = State.getInstance();
        
        setContentView(R.layout.camera_view);
        
        TaskType type = (TaskType) getIntent().getSerializableExtra(TaskType.className);
        if(type == null) {
            try{throw new Exception("Null is possible"); }catch(Exception e) {e.printStackTrace();} //TODO kell-e
            type = mState.mTaskType;
        }
        
        switch(type)
        {
            case POSITION: mState.mTaskType = TaskType.POSITION; break;
            case TIME:     mState.mTaskType = TaskType.TIME;     break;
            case COMPASS:  mState.mTaskType = TaskType.COMPASS;  break;
            default: Log.e(TAG, "Unknown type");
        }
                        
        mCameraPreview  = (CameraSurface)findViewById(R.id.cameraPreview);
        mHistoryCounter = (TextView)findViewById(R.id.historyCounter);
        
        mZoomBar = (SeekBar)findViewById(R.id.zoomBar);
        mZoomBar.setOnSeekBarChangeListener(mZoomListener);
        
        mSettingsBtn = (ImageView)findViewById(R.id.settingsIcon);
        
        Display display = ((WindowManager) getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
        int orientation = display.getRotation();
         
        TextView infoText = (TextView) findViewById(R.id.infos);
        infoText.setTypeface(Typeface.MONOSPACE);
                
        mSensorWatcher = new SensorWatcher(infoText, orientation);
        mCameraPreview.setsensorWatcher(mSensorWatcher);
        
        LinearLayout mLayout = (LinearLayout) findViewById(R.id.cameraExtraLayer);
        ARView myCanvas = new ARView(this, mState, mSensorWatcher);
        mLayout.addView(myCanvas);
        mSensorWatcher.addListener(myCanvas);
    }
              
    @Override
    public void onResume() {
        super.onResume();
        Log.d(TAG, "onResume");
        
        mSensorWatcher.onResume();
        mCameraPreview.onResume();
        mZoomBar.setProgress(0); //TODO visszaallitani mState-bol(csak ott igazi percent van), camera-t is beallitani
        
    }
      
    @Override
    public void onPause() {
        Log.d(TAG, "onPause");
        
        mCameraPreview.onPause();
        mSensorWatcher.onPause();
        super.onPause();
    }
    
    public void shootClick(View _){
        mCameraPreview.shoot();
    }
    
    public void switchTarget(View view) {
        ImageView btn = (ImageView) view;
        boolean debugMode0 = mState.mDebugEnabler.mDebugMode;
        boolean debugMode1 = mState.switchTarget();
        btn.setImageResource(mState.mTarget.mIconID);
        Log.d(TAG, "switchTarget to " + mState.mTarget);
        if(debugMode1 != debugMode0) {
                mSettingsBtn.setVisibility(debugMode1 ? View.VISIBLE : View.GONE);
                Log.d(TAG, "enabled debug mode");
        }
    }
    
    public void launchResult(View _) {
        Intent intent = new Intent(this, MapActivity.class);
        intent.putExtra(TaskType.className, mState.mTaskType);
        startActivity(intent);
    }
    
    public void launchHistory(View _) {
        Intent intent = new Intent(this, GalleryActivity.class);
        intent.putExtra(TaskType.className, mState.mTaskType);
        startActivity(intent);
    }
    
    public void launchSettings(View _) {
        //Intent intent = new Intent(this, SettingsActivity.class);
        //startActivity(intent);
    }
    
    private OnSeekBarChangeListener mZoomListener = new OnSeekBarChangeListener() {
        
        @Override
        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
            if(fromUser) {
                mCameraPreview.changeZoomPercent(progress);
            }
        }
        
        @Override public void onStartTrackingTouch(SeekBar _) {}
        @Override public void onStopTrackingTouch(SeekBar _) {}
    };
    
}