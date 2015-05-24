package hu.ureczky.celebrations.activities;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.view.Display;
import android.view.View;
import android.widget.FrameLayout;

import hu.ureczky.celebrations.R;
import hu.ureczky.celebrations.map.DbTransformer;
import hu.ureczky.celebrations.map.MapView;
import hu.ureczky.celebrations.map.TilesProvider;
import hu.ureczky.utils.AndroidUtils;

public class MapActivity extends Activity
{
    private static final String TAG = "MapActivity";
    
    FrameLayout mMapHolder;
	MapView mMapView; // Our only view, created in code

	// Provides us with Tiles objects, passed to MapView
	TilesProvider mTilesProvider;
	
	Location mSavedGpsLocation;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        AndroidUtils.setFullScreen(this);
        
        setContentView(R.layout.map_view);
        mMapHolder = (FrameLayout) findViewById(R.id.mapContainer);
    }
	
	@Override
	protected void onResume()
	{	    
		// Create MapView
		initViews();
		
		// Restore zoom and location data for the MapView
		mMapView.setZoom(3);
		mMapView.setGpsLocation(47.5, 19, 100000); //TODO hardcoded location
		
		mMapHolder.addView(mMapView);
		
		super.onResume();
	}

	void initViews()
	{
		// Creating the bitmap of the marker from the resources
		Bitmap marker = BitmapFactory.decodeResource(getResources(), R.drawable.pin_red);    
		
		new DbTransformer(this);
		
		mTilesProvider = new TilesProvider(this);
       
		// Creating the mapView and make sure it fills the screen
		Display display = getWindowManager().getDefaultDisplay();
		mMapView = new MapView(this, display.getWidth(), display.getHeight(), mTilesProvider, marker);

		// If a location was saved while pausing the app then use it.
		if (mSavedGpsLocation != null) mMapView.setGpsLocation(mSavedGpsLocation);

		// Update and draw the map view
		mMapView.refresh();
	}

	@Override
	protected void onPause()
	{
		// Clears the tiles held in the tilesProvider
		mTilesProvider.clear();

		// Release mapView pointer
		mMapView = null;

		super.onPause();
	}
	
	public void onZoomPlus(View _) {
        Log.v(TAG, "onZoomPlus()");
        mMapView.zoomIn();
        
    }
	
	public void onZoomMinus(View _) {
        Log.v(TAG, "onZoomMinus()");
        mMapView.zoomOut();
    }
	
	public void onGoto(View _) {
        Log.v(TAG, "onGoto()");
        mMapView.followMarker();
    }

}