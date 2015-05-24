package hu.ureczky.celebrations.map;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.PointF;
import android.graphics.Rect;
import android.location.Location;
import android.view.MotionEvent;
import android.view.View;

import java.util.Collection;

public class MapView extends View
{
    private static final String TAG = "MapView";

	// MapView dimensions
	public int mWidth;

    public int mHeight;

	// Provides us with tiles
	private TilesProvider mTileProvider;

	// Handles calculations
	private TilesManager mTileManager;

	// Different paints
	private Paint mFontPaint;
	private Paint mBitmapPaint = new Paint();
	private Paint mCirclePaint = new Paint();

	// The location of the view center in longitude, latitude
	private PointF mSeekLocation = new PointF(47.5f, 19.0f);
	// Location of the phone using Gps data
	private Location mGpsLocation = null;
	// If true then seekLocation will always match gpsLocation
	private boolean mAutoFollow = false;

	// An image to draw at the phone's position
	private Bitmap mPositionMarker;

	// touch position values kept for panning\dragging
	//private PointD lastTouchPos = new PointD(-1, -1);
	private Point mLastTouchPos = new Point(-1, -1);
	
	private Point mOffset = new Point();
	private Point mPix = new Point();

	public MapView(Context context, int viewWidth, int viewHeight, TilesProvider tilesProvider, Bitmap positionMarker)
	{
		super(context);

		// Tiles provider is passed not created.
		// The idea is to hide the actual tiles source from the view
		// This way the view doesn't care whether the source is a database or
		// the internet
		mTileProvider = tilesProvider;

		// These values will be used later
		mWidth = viewWidth;
		mHeight = viewHeight;

		// Get the marker image
		mPositionMarker = positionMarker;

		// Creating a TilesManager assuming that the tile size is 256*256.
		// You might want to pass tile size as a parameter or even calculate it
		// somehow
		mTileManager = new TilesManager(256, viewWidth, viewHeight, getContext());

		// Initializes paints
		initPaints();

		// Fetching tiles from the tilesProvider
		fetchTiles();
	}

	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec)
	{
		// Setting width,height that was passed in the constructor as the view's dimensions
		setMeasuredDimension(mWidth, mHeight);
	}

	void initPaints()
	{
		// Font paint is used to draw text
		mFontPaint = new Paint();
		mFontPaint.setColor(Color.DKGRAY);
		mFontPaint.setShadowLayer(1, 1, 1, Color.BLACK);
		mFontPaint.setTextSize(20);

		// Used to draw a semi-transparent circle at the phone's gps location
		mCirclePaint.setARGB(128, 255, 0, 0);
		mCirclePaint.setAntiAlias(true);
	}

	void fetchTiles()
	{
		// Update tilesManager to have the center of the view as its location
		mTileManager.setLocation(mSeekLocation.x, mSeekLocation.y);

		// Get the visible tiles indices as a Rect
		Rect visibleRegion = mTileManager.getVisibleRegion();

		// Tell tiles provider what tiles we need and which zoom level.
		// The tiles will be stored inside the tilesProvider.
		// We can get those tiles later when drawing the view
		mTileProvider.fetchTiles(visibleRegion, mTileManager.getZoom());
	}

	@Override
	protected void onDraw(Canvas canvas)
	{
		// Clear the view to grey
		canvas.drawARGB(255, 100, 100, 100);

		/*
		 * To draw the map we need to find the position of the pixel representing the center of the view.
		 * We need the position to be relative to the full world map, lets call this pixel position "pix"
		 * pix.x will range from 0 to (2^zoom)*tileSize-1, same for pix.y		
		 * To draw anything on the map we subtract pix from the original position
		 * It's just like dragging the map so that the pixel representing the gps location gets into the center of the view 		 
		*/

		// In a square world map,
		// we need to know pix location as two values from 0.0 to 1.0
		PointF pixRatio = TilesManager.calcRatio(mSeekLocation.x, mSeekLocation.y);
		
		// Full world map width in pixels
		int mapWidth = mTileManager.mapSize() * 256;
		mPix.set((int) (pixRatio.x * mapWidth), (int) (pixRatio.y * mapWidth));

		/*
		 * Subtracting pix from each tile position will result in pix being drawn at the top left corner of the view 
		 * To drag it to the center we add (viewWidth/2, viewHeight/2) to the final result
		 * pos.x = pos.x - pix.x + viewWidth/2f
		 * pos.x = pox.x - (pix.x - viewWidth/2f)
		 * ---> offset.x =  (pix.x - viewWidth/2f)
		 * same for offset.y
		 */

		mOffset.set((int) (mPix.x - mWidth / 2f), (int) (mPix.y - mHeight / 2f));
		// offset is now ready to use

		// Drawing tiles in a separate function to make the code more readable
		drawTiles(canvas, mOffset);

		// Draw the marker that pinpoints the user's location
		drawMarker(canvas, mOffset);
	}

	void drawTiles(Canvas canvas, Point offset)
	{
		// Get tiles from the Hashtable inside tilesProvider
		Collection<Tile> tilesList = mTileProvider.getTiles().values();

		// x,y are the calculated offset

		// Go trough all the available tiles
		for (Tile tile : tilesList)
		{
			// We act as if we're drawing a map of the whole world at a specific zoom level
			// The top left corner of the map occupies the pixel (0,0) of the view
			int tileSize = mTileManager.getTileSize();
			long tileX = tile.x * tileSize;
			long tileY = tile.y * tileSize;

			// Subtract offset from the previous calculations
			long finalX = tileX - offset.x;
			long finalY = tileY - offset.y;

			// Draw the bitmap of the tiles using a simple paint
			canvas.drawBitmap(tile.img, finalX, finalY, mBitmapPaint);
		}
	}

	void drawMarker(Canvas canvas, Point offset)
	{
		// x,y are the calculated offset

		// Proceed only if a gps fix is available
		if (mGpsLocation != null)
		{
			// Get marker position in pixels as if we're going to draw it on a world map
		    // where the top left corner of the map occupies the (0,0) pixel of the view
			Point markerPos = mTileManager.lonLatToPixelXY((float) mGpsLocation.getLongitude(), (float) mGpsLocation.getLatitude());

			// Add offset to the marker position
			int markerX = markerPos.x - offset.x;
			int markerY = markerPos.y - offset.y;


			// Around the marker we will draw a circle representing the accuracy of the gps fix
			// We first calculate its radius
			
			// Calculate how many meters one pixel represents
			float ground = (float) mTileManager.calcGroundResolution(mGpsLocation.getLatitude());
 
			// Location.getAccuracy() returns the accuracy in meters.
			float rad = mGpsLocation.getAccuracy() / ground;

			canvas.drawCircle(markerX, markerY, rad, mCirclePaint);
			
			// Draw the marker and make sure you draw the center of the marker at the marker location
            canvas.drawBitmap(mPositionMarker, markerX - mPositionMarker.getWidth() / 2, markerY - mPositionMarker.getHeight(), mBitmapPaint);

			// Just drawing location info
			int pen = 1;
			canvas.drawText("lon:" + mGpsLocation.getLongitude(), 0, 20 * pen++, mFontPaint);
			canvas.drawText("lat:" + mGpsLocation.getLatitude(), 0, 20 * pen++, mFontPaint);
			canvas.drawText("Zoom:" + mTileManager.getZoom(), 0, 20 * pen++, mFontPaint);
		}
	}

	@Override
	public boolean onTouchEvent(MotionEvent event)
	{
		int action = event.getAction();

		if (action == MotionEvent.ACTION_DOWN)
		{
			// Keep touch position for later use (dragging)
			mLastTouchPos.x = (int) event.getX();
			mLastTouchPos.y = (int) event.getY();

			return true;
		}
		else if (action == MotionEvent.ACTION_MOVE)
		{
			mAutoFollow = false;

			Point current = new Point((int)event.getX(), (int)event.getY());
			
			// Find how many pixels the users finger moved in both x and y
			Point diff = new Point(current.x - mLastTouchPos.x, current.y - mLastTouchPos.y);

			// In a full world map, get the position of the center of the view in pixels
			Point pixels1 = mTileManager.lonLatToPixelXY(mSeekLocation.x, mSeekLocation.y);
			
			// Subtract diff from that position
			Point pixels2 = new Point(pixels1.x - (int) diff.x, pixels1.y - (int) diff.y);

			// Recover the final result to longitude, latitude point
			PointF newSeek = mTileManager.pixelXYToLonLat((int) pixels2.x, (int) pixels2.y);

			// Finally move the center of the view to the new location
			mSeekLocation = newSeek;

			invalidate();

			// Prepare for the next drag event
			mLastTouchPos.x = current.x;
			mLastTouchPos.y = current.y;

			return true;
		}
		else{
		    fetchTiles();
            invalidate();
		}

		return super.onTouchEvent(event);
	}

	// Fetch the tiles then draw, don't call to often
	public void refresh()
	{
		fetchTiles();
		invalidate();
	}

	// Like refresh but called from a non UI thread
	public void postRefresh()
	{
		fetchTiles();
		postInvalidate();
	}

	// Simply sets seek location to gpsLocation (if exists) 
	public void followMarker()
	{
		if (mGpsLocation != null)
		{
			mSeekLocation.x = (float) mGpsLocation.getLongitude();
			mSeekLocation.y = (float) mGpsLocation.getLatitude();
			mAutoFollow = true;
			
			fetchTiles();
			invalidate();
		}
	}

	public void zoomIn()
	{
		mTileManager.zoomIn();
		onMapZoomChanged();
	}

	public void zoomOut()
	{
		mTileManager.zoomOut();
		onMapZoomChanged();
	}
	
	private void onMapZoomChanged()
	{
		mTileProvider.clear();
		fetchTiles();
		invalidate();
	}

	// Returns the gps coordinates of the user
	public Location getGpsLocation()
	{
		return mGpsLocation;
	}

	// Returns the gps coordinates of our view center
	public PointF getSeekLocation()
	{
		return mSeekLocation;
	}

	// Centers the given gps coordinates in our view
	public void setSeekLocation(float longitude, float latitude)
	{
		mSeekLocation.x = longitude;
		mSeekLocation.y = latitude;
	}

	// Sets the marker position
	public void setGpsLocation(Location location)
	{
		setGpsLocation(location.getLongitude(), location.getLatitude(), location.getAccuracy());
	}

	// Sets the marker position
	public void setGpsLocation(double latitude, double longitude, float accuracy)
	{
		if (mGpsLocation == null) mGpsLocation = new Location("");
		mGpsLocation.setLongitude(longitude);
		mGpsLocation.setLatitude(latitude);
		mGpsLocation.setAccuracy(accuracy);

		if (mAutoFollow) followMarker();
	}

	public int getZoom()
	{
		return mTileManager.getZoom();
	}

	public void setZoom(int zoom)
	{
		mTileManager.setZoom(zoom);
		onMapZoomChanged();
	}
}