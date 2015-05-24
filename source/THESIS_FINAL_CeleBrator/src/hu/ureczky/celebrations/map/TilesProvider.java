package hu.ureczky.celebrations.map;

import android.content.Context;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Rect;
import android.os.Environment;
import android.util.Log;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Set;

public class TilesProvider
{
    private static final String TAG = "TilesProvider";
    
    private static final String PACKAGE_NAME = "hu.ureczky.celebrations";
    public static final File SD_APP_DIR = new File(Environment.getExternalStorageDirectory(),
            "Android" + File.separator + 
            "data" + File.separator + 
            PACKAGE_NAME + File.separator +
            "files");
    public static final String TILES_DIR_NAME = "map_tiles";
    public static final File SD_TILES_DIR = new File(SD_APP_DIR, TILES_DIR_NAME);
    
	// Tiles will be stored here, the index\key will be in this format zoom.x.y.png
	private Hashtable<String, Tile> mTiles = new Hashtable<String, Tile>();
	Set<String> mDirtyTiles = new HashSet<String>();
	
	private Context mContext;
	private AssetManager mAssetManager;
	
	private enum FileLocation {
	    ASSETS,
	    SD_CARD
	}
    
	public TilesProvider(Context context) {
	    mContext = context;
	}
	
    // Gets the hashtable where the tiles are stored
    public Hashtable<String, Tile> getTiles()
    {
        return mTiles;
    }

    public void clear()
    {
        mTiles.clear();
    }
	
	// Updates the tiles in the hashtable
	public void fetchTiles(Rect rect, int zoom)
	{
	    // Mark dirty
	    mDirtyTiles.clear(); 
        for(String s: mTiles.keySet()) {
            mDirtyTiles.add(s);
        }
        
        fetchTiles(FileLocation.SD_CARD, rect, zoom);
        fetchTiles(FileLocation.ASSETS,  rect, zoom);

		// Remove dirties
	    for(String s: mDirtyTiles){
	        mTiles.remove(s);
	    }
	    mDirtyTiles.clear();
	}
	
	private void fetchTiles(FileLocation fileLocation, Rect rect, int zoom) {
	            
        String[] tileNames = listTiles(fileLocation, zoom);
        
        if(tileNames != null) {
            for(String tileName: tileNames) // tile.zoom.x.y
            {
                String[] bits = tileName.split("\\.");
                int x = Integer.valueOf(bits[2]);
                int y = Integer.valueOf(bits[3]);
                String tileId = String.format("tile.%d.%d.%d", zoom, x, y);
                int p = 1; // extra
                if((rect.left - p <= x) && (x <= rect.right  + p)
                && (rect.top  - p <= y) && (y <= rect.bottom + p)) {
                    // Try to get this tile from the hashtable we have
                    Tile tile = mTiles.get(tileId);
    
                    // If This is a new tile, we didn't fetch it in the previous fetchTiles call.
                    if (tile == null) {                    
                        fetchTile(fileLocation, tileName, tileId, zoom, x, y);
                    } else {
                        mDirtyTiles.remove(tileId);
                    }
                }
            }
        }
	}
	
	// Find files according to zoom level
	private String[] listTiles(FileLocation fileLocation, int zoom) {
	    
	    String[] tileNames = null;
	    
	    switch(fileLocation) {
            case ASSETS:
                mAssetManager = mContext.getAssets();
                try {
                    tileNames = mAssetManager.list(TILES_DIR_NAME + File.separator + zoom);
                } catch (IOException e) {
                    Log.e(TAG, "Failed to get asset file list.", e);
                }
                break;
            case SD_CARD:
                File dir = new File(SD_TILES_DIR, String.valueOf(zoom));
                tileNames = dir.list();
                break;
            default:
                Log.e(TAG, "Unknown file location");
	    }
	    
	    return tileNames;
	}
	
	private void fetchTile(FileLocation fileLocation, String tileName, String tileId, int zoom, int x, int y) {
	    InputStream inStream = readTile(fileLocation, zoom, tileName);
	    
	    if(inStream != null) {
	        // Decode bitmap
	        BitmapFactory.Options options = new BitmapFactory.Options();
	        options.inPreferredConfig = Bitmap.Config.ARGB_8888;
	        Bitmap tileBitmap = BitmapFactory.decodeStream(inStream, null, options);
	                            
	        // Create the new tile
	        Tile tile = new Tile(x, y, tileBitmap);
	        
	        // Add the tile to the temp hashtable
	        mTiles.put(tileId, tile);
	    }
	}
	
	private InputStream readTile(FileLocation fileLocation, int zoom, String tileName) {
	    InputStream inStream = null;
	    switch(fileLocation) {
            case ASSETS:
                try {
                    inStream = mAssetManager.open(TILES_DIR_NAME + File.separator + zoom + File.separator + tileName);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                break;
            case SD_CARD:
                File file = new File(SD_TILES_DIR, zoom + File.separator + tileName);
                try {
                    inStream = new BufferedInputStream(new FileInputStream(file));
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                }
                break;
            default:
                Log.e(TAG, "Unknown file location");
        }
	    return inStream;
	}

}