package hu.ureczky.celebrations.map;

import android.content.Context;
import android.graphics.Point;
import android.graphics.PointF;
import android.graphics.Rect;
import android.util.Log;

import java.io.File;
import java.io.IOException;

public class TilesManager
{
	public final static double EarthRadius  =  6378137; // in meters
	public final static double MinLatitude  = -85.05112878; // Near South pole
	public final static double MaxLatitude  =  85.05112878; // Near North pole
	public final static double MinLongitude = -180; // West
	public final static double MaxLongitude =  180; // East

	private int mMinZoom = 2;
	private int mMaxZoom; // will be extracted from DB
	private int mTileSize = 256; // Size in pixels of a single tile image

	// Dimensions in pixels of the view the map is rendered in.
	private int mWidth, mHeight;

	// Number of tiles (horizontally and vertically) needed to fill the view, calculated later.
	private int mTileCountX, mTileCountY;

	// Will hold the indices of the visible tiles
	private Rect mVisibleRegion;

	// Current location of the tiles manager
	private PointF mLocation = new PointF(0, 0);

	// Current zoom level
	private int mZoom = 0;

	public TilesManager(int tileSize, int viewWidth, int viewHeight, Context context)
	{
		mTileSize = tileSize;

		mWidth = viewWidth;
		mHeight = viewHeight;

		// Simple math :)
		mTileCountX = (int) ((float) viewWidth / tileSize);
		mTileCountY = (int) ((float) viewHeight / tileSize);
		
		calcMaxZoom(context);

		// Updates visible region, this function will be explained later
		updateVisibleRegion(mLocation.x, mLocation.y, mZoom);
	}

	public static PointF calcRatio(float longitude, float latitude)
	{
		double ratioX = ((longitude + 180.0) / 360.0);

		double sinLatitude = Math.sin(latitude * Math.PI / 180.0);
		double ratioY = (0.5 - Math.log((1 + sinLatitude) / (1.0 - sinLatitude)) / (4.0 * Math.PI));

		return new PointF((float)ratioX, (float)ratioY);
	}

	public int mapSize()
	{
		return (int) Math.pow(2, mZoom);
	}

	private Point calcTileIndices(float longitude, float latitude)
	{
		// Simple calculations
		PointF ratio = calcRatio(longitude, latitude);
		int mapSize = mapSize();

		return new Point((int) (ratio.x * mapSize), (int) (ratio.y * mapSize));
	}

	private void updateVisibleRegion(double longitude, double latitude, int zoom)
	{
		// Update manager state
		mLocation.x = (float) longitude;
		mLocation.y = (float) latitude;
		mZoom = zoom;

		// Get the index of the tile we are interested in
		Point tileIndex = calcTileIndices(mLocation.x, mLocation.y);

		// We get some of the neighbors from left and some from right
		// Same thing for up and down
		int halfTileCountX = (int) ((float) (mTileCountX + 1) / 2f);
		int halfTileCountY = (int) ((float) (mTileCountY + 1) / 2f);

		mVisibleRegion = new Rect(tileIndex.x - halfTileCountX, tileIndex.y - halfTileCountY, tileIndex.x + halfTileCountX, tileIndex.y
				+ halfTileCountY);
	}

	// Simple clamp function
	private static double clamp(double x, double min, double max)
	{
		return Math.min(Math.max(x, min), max);
	}

	public double calcGroundResolution(double latitude)
	{
		latitude = clamp(latitude, MinLatitude, MaxLatitude);
		return Math.cos(latitude * Math.PI / 180.0) * 2.0 * Math.PI * EarthRadius / (double) (mTileSize * mapSize());
	}

	public Point lonLatToPixelXY(float longitude, float latitude)
	{
		// Clamp values
		longitude = (float) clamp(longitude, MinLongitude, MaxLongitude);
		latitude  = (float) clamp(latitude,  MinLatitude,  MaxLatitude);

		PointF ratio = calcRatio(longitude, latitude);
		double x = ratio.x;
		double y = ratio.y;

		long mapSize = mapSize() * mTileSize;
		int pixelX = (int) clamp(x * mapSize + 0.5, 0, mapSize - 1);
		int pixelY = (int) clamp(y * mapSize + 0.5, 0, mapSize - 1);

		return new Point(pixelX, pixelY);
	}

	public PointF pixelXYToLonLat(int pixelX, int pixelY)
	{
		double mapSize = mapSize() * mTileSize;
		double x = (clamp(pixelX, 0, mapSize - 1) / mapSize) - 0.5;
		double y = 0.5 - (clamp(pixelY, 0, mapSize - 1) / mapSize);

		double latitude = 90.0 - 360.0 * Math.atan(Math.exp(-y * 2.0 * Math.PI)) / Math.PI;
		double longitude = 360.0 * x;

		return new PointF((float)longitude, (float)latitude);
	}

	public void setZoom(int zoom)
	{
		zoom = (int) clamp(zoom, mMinZoom, mMaxZoom);
		updateVisibleRegion(mLocation.x, mLocation.y, zoom);
	}

	public void setLocation(double longitude, double latitude)
	{
		updateVisibleRegion(longitude, latitude, mZoom);
	}

	public Rect getVisibleRegion()
	{
		return mVisibleRegion;
	}

	public int getZoom()
	{
		return mZoom;
	}

	public int zoomIn()
	{
		setZoom(mZoom + 1);
		return mZoom;
	}

	public int zoomOut()
	{
		setZoom(mZoom - 1);
		return mZoom;
	}

	public int getTileSize()
	{
		return mTileSize;
	}

	public int getMaxZoom()
	{
		return mMaxZoom;
	}

	public void setMaxZoom(int maxZoom)
	{
		mMaxZoom = maxZoom;
	}
	
	public void calcMaxZoom(Context context)
    {
	    // Scan assets
        String[] assetFiles = null;
        try {
            assetFiles = context.getAssets().list(TilesProvider.TILES_DIR_NAME);
        } catch (IOException e) {
            Log.e("tag", "Failed to get asset file list.", e);
        }
        
        for(String zoomStr: assetFiles) {
            try {
                Integer zoom = Integer.parseInt(zoomStr);
                mMaxZoom = Math.max(mMaxZoom, zoom);
            } catch (NumberFormatException e){}
        }
        
        // Scan SD card
        File dir = TilesProvider.SD_TILES_DIR;
        String[] sdDirs = dir.list();
        for(String zoomStr: sdDirs) {
            try {
                Integer zoom = Integer.parseInt(zoomStr);
                mMaxZoom = Math.max(mMaxZoom, zoom);
            } catch (NumberFormatException e){}
        }
    }

}