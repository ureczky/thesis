package hu.ureczky.celebrations.map;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;

import hu.ureczky.utils.MediaScannerHacker;

import java.io.File;
import java.io.FileOutputStream;

public class DbTransformer {

    private static final String TAG = "DbTransformer";
    private Context mContext;
            
    public DbTransformer(Context context) {
        mContext = context;

        if(!TilesProvider.SD_TILES_DIR.exists()) {
            TilesProvider.SD_TILES_DIR.mkdirs();
        }
        
        transformDB();
        Log.d("DbTransformer", "Transformed");
    }
    
    private void transformDB() {
        
        File[] files = TilesProvider.SD_TILES_DIR.listFiles();
        for(File dbFile : files) {
            if(!dbFile.getName().endsWith(".sqlite")) continue; // not a db file
            
            String dbName = dbFile.getName();
            Log.d(TAG, "Transform " + dbName);
            dbName = dbName.substring(0, dbName.lastIndexOf('.')); // cut extension
                                    
            SQLiteDatabase tilesDB = SQLiteDatabase.openDatabase(dbFile.getPath(), null, SQLiteDatabase.NO_LOCALIZED_COLLATORS | SQLiteDatabase.OPEN_READONLY);
            
            String query = "SELECT key,tile FROM tiles";
            Cursor cursor = tilesDB.rawQuery(query, null);
                    
            if (cursor.moveToFirst())
            {
                do
                {
                    // Getting the index of this tile
                    int dbKey = cursor.getInt(0);
    
                    // Get the binary image data from the third cursor column
                    byte[] img = cursor.getBlob(1);
                    
                    String newKey = transformKey(dbKey);
    
                    saveImage(img, newKey);
                                 
                }
                while (cursor.moveToNext()); // Move to next tile in the query result
    
            }
            tilesDB.close();
            
            // Delete sqlite file
            boolean deleted = dbFile.delete();
            Log.d(TAG, "Delete " + dbFile.getName() + ": " + deleted);
            
            // TODO Refresh SD Card?
            /*
            Not working on KITKAT:
            mContext.sendBroadcast(new Intent(
                    Intent.ACTION_MEDIA_MOUNTED,
                    Uri.parse("file://" +  Environment.getExternalStorageDirectory())));
            */
            /*
            Can't find file:
            mContext.getContentResolver().delete(Uri.fromFile(dbFile), null, null);
            */
        }
    }
    
    private String transformKey(int key){
        // Find z,b,e
        int b = 0, e = 1; // begin/end index
        int d = 1; // size: 2^z, z: current zoom
        int z = 0;
        for(; !(b <= key && key < e) ; z++) {
            d *= 2;
            b = e * 4;
            e = b + d*d;
        }
        
        int idx = key - b;
        int x = idx / d;
        int y = idx % d;
        
        return "tile." + z + "." + x + "." + y;
    }
    
    private void saveImage(byte[] img, String key) {
        
        int zoom = Integer.valueOf(key.split("\\.")[1]);
        
        File zoomDir = new File(TilesProvider.SD_TILES_DIR, String.valueOf(zoom));
        if(!zoomDir.exists()) {
            zoomDir.mkdirs();
        }
        
        // Create a bitmap (expensive operation)
        Bitmap tileBitmap = BitmapFactory.decodeByteArray(img, 0, img.length);
        
        File file = new File(zoomDir, key + ".jpg");
        try {
            FileOutputStream fOut = new FileOutputStream(file);
            tileBitmap.compress(Bitmap.CompressFormat.JPEG, 80, fOut);
            fOut.flush();
            fOut.close();
            MediaScannerHacker.scan(mContext, file);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
}
