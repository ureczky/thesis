package hu.ureczky.utils;

import android.content.Context;
import android.media.MediaScannerConnection;

import java.io.File;

// e.g. on Nexus 5, MTP does not working correctly.
// When a file has been created it is visible on the phone, but not in Windows Explorer.
// Media Scanner should be informed to scan this file..    
public class MediaScannerHacker {
        
    public static void scan(Context context, File ... files) {
        String[] fileNames = new String[files.length];
        for(int i = 0; i < files.length; i++) {
            File file = files[i]; 
            //context.sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.fromFile(file)));
            fileNames[i] = file.getPath();
        }
        MediaScannerConnection.scanFile(context, fileNames, null, null);
    }
}
