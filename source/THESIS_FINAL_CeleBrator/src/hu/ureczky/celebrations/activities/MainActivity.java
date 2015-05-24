package hu.ureczky.celebrations.activities;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import hu.ureczky.celebrations.R;
import hu.ureczky.celebrations.TaskType;
import hu.ureczky.utils.AndroidUtils;
import hu.ureczky.utils.tests.Tests;

public class MainActivity extends Activity {
    
	final static String TAG = "MainActivity";
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        AndroidUtils.setFullScreen(this);
        
        setContentView(R.layout.main_view);
                
        Tests.test();
    }
    
    public void launchPosition(View _) {
        Intent intent = new Intent(this, ARActivity.class);
        intent.putExtra(TaskType.className, TaskType.POSITION);
        startActivity(intent);
    }
    
    public void launchTime(View _) {
        Intent intent = new Intent(this, ARActivity.class);
        intent.putExtra(TaskType.className, TaskType.TIME);
        startActivity(intent);
    }
    
    public void launchCompass(View _) {
        Intent intent = new Intent(this, ARActivity.class);
        intent.putExtra(TaskType.className, TaskType.COMPASS);
        startActivity(intent);
    }
    
    /*
    public void launchCalibration(View _) {
        Intent intent = new Intent(this, ARActivity.class);
        //intent.putExtra(CAMERA_VIEW_TYPE, "Calibration");
        startActivity(intent);
    }
    */
    
}