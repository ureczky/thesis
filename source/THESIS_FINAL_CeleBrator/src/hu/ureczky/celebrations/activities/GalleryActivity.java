package hu.ureczky.celebrations.activities;

import android.app.Activity;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Typeface;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.Gallery;
import android.widget.ImageView;
import android.widget.TextView;

import hu.ureczky.celebrations.R;
import hu.ureczky.utils.AndroidUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * PicSelectActivity presents image gallery
 * - user can select new images to display within scrolling thumbnail gallery
 * - user can select individual image to display at larger size
  */
public class GalleryActivity extends Activity {
    
    private static final String TAG = "GalleryActivity";
	
	//variable to store the currently selected image
	private int mCurrPicIdx = 0;
	//adapter for gallery view
	private MeasurementAdapter mImgAdapt;
	//gallery object
	private Gallery mPicGallery;
	//image view for larger display
	private ImageView mPicView;
	private TextView mDataView;
	
    /** Instantiate the interactive gallery */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        
        super.onCreate(savedInstanceState);
        AndroidUtils.setFullScreen(this);
        
        setContentView(R.layout.gallery_view);
        
        mPicView    = (ImageView) findViewById(R.id.picture);
        mDataView   = (TextView)  findViewById(R.id.tvMetaData);
        mPicGallery = (Gallery) findViewById(R.id.gallery);
        
        mDataView.setTypeface(Typeface.MONOSPACE);
        mImgAdapt = new MeasurementAdapter(this);
        mPicGallery.setAdapter(mImgAdapt);
                
        mPicGallery.setOnItemClickListener(new OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View v, int position, long id) {
                mPicView.setImageBitmap(mImgAdapt.getPic(position));
                mDataView.setText(mImgAdapt.getMetaData(position));
            }
        });
    }
    
    private class Measurement {
        
        private Bitmap mBitmap;
        private String mMetaData;
        
        public Measurement(Bitmap bmp, String metaData) {
            mBitmap = bmp;
            metaData = metaData.replaceAll("\"", "");     // del "
            metaData = metaData.replaceAll("\\{", "");   // del {
            metaData = metaData.replaceAll("\\}", "");   // del }
            metaData = metaData.replaceAll(",\n", "\n"); // ,\n -> \n
            mMetaData = metaData;
        }
        
        public Bitmap getBitmap() {
            return mBitmap;
        }
        
        public String getMetaData() {
            return mMetaData;
        }
    }
    
    /**
     * Base Adapter subclass creates Gallery view
     * - provides method for adding new images from user selection
     * - provides method to return bitmaps from array
     */
    public class MeasurementAdapter extends BaseAdapter {
    	
    	//use the default gallery background image
        int mDefaultItemBackground;
        
        private Context mContext;
        File mPicDir;

        private List<Measurement> mMeasurements;

        public MeasurementAdapter(Context context) {
        	
        	mContext = context;
        	mPicDir = new File(mContext.getExternalFilesDir(null), "calc_position");
            
            initMeasurements();
            
            //get the styling attributes - use default Andorid system resources
            TypedArray styleAttrs = mContext.obtainStyledAttributes(R.styleable.PicGallery);
            
            //get the background resource
            mDefaultItemBackground = styleAttrs.getResourceId(R.styleable.PicGallery_android_galleryItemBackground, 0);
            
            //recycle attributes
            styleAttrs.recycle();
        }
        
        private void initMeasurements() {
            mMeasurements  = new ArrayList<Measurement>();
            File[] files = mPicDir.listFiles();
            if(files != null) {
                for (File file : files) {
                    String fileName = file.getName();
                    if(fileName.endsWith(".jpg")) {
                        String picPath = file.getAbsolutePath();
                        String metaPath = picPath.replaceAll(".jpg", ".json");
                        String metaData = readFile(metaPath);
                        Bitmap bmp = decodeSampledBitmapFromUri(picPath, 220, 220);
                        if(bmp != null && metaData != null) {
                            mMeasurements.add(new Measurement(bmp, metaData));
                        }
                    }
                }
            }
        }
                
        public Bitmap decodeSampledBitmapFromUri(String path, int reqWidth, int reqHeight) {
            Bitmap bm = null;

            // First decode with inJustDecodeBounds=true to check dimensions
            final BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            BitmapFactory.decodeFile(path, options);

            // Calculate inSampleSize
            options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight);

            // Decode bitmap with inSampleSize set
            options.inJustDecodeBounds = false;
            bm = BitmapFactory.decodeFile(path, options);

            return bm;
        }
        
        private int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {
            // Raw height and width of image
            final int height = options.outHeight;
            final int width  = options.outWidth;
            int inSampleSize = 1;

            if (height > reqHeight || width > reqWidth) {
                if (width > height) {
                    inSampleSize = Math.round((float) height / (float) reqHeight);
                } else {
                    inSampleSize = Math.round((float) width / (float) reqWidth);
                }
            }

            return inSampleSize;
        }
        
        private String readFile(String path) {
            try {
                BufferedReader br = new BufferedReader(new FileReader(new File(path)));
                String line;
                StringBuilder text = new StringBuilder();

                while ((line = br.readLine()) != null) {
                    text.append(line);
                    text.append('\n');
                }
                return text.toString();
            } catch (IOException e) {
                return null;
            }
        }

        //BaseAdapter methods
        
        //return number of data items i.e. bitmap images
        public int getCount() {
            return mMeasurements.size();
        }

        //return item at specified position
        public Object getItem(int position) {
            return position;
        }

        //return item ID at specified position
        public long getItemId(int position) {
            return position;
        }

        //get view specifies layout and display options for each thumbnail in the gallery
        public View getView(int position, View convertView, ViewGroup parent) {

            ImageView imageView = new ImageView(mContext);
            imageView.setImageBitmap(mMeasurements.get(position).mBitmap);
            imageView.setLayoutParams(new Gallery.LayoutParams(300, 200));
            imageView.setScaleType(ImageView.ScaleType.FIT_CENTER);
            imageView.setBackgroundResource(mDefaultItemBackground);
            return imageView;
        }
        
        public Bitmap getPic(int idx) {
        	return mMeasurements.get(idx).mBitmap;
        }
        
        public String getMetaData(int idx) {
            return mMeasurements.get(idx).mMetaData;
        }
        
        public int removePic(int idx) {
            mMeasurements.remove(idx);
            this.notifyDataSetChanged();
            return Math.min(idx, mMeasurements.size() - 1);
        }
    }
    
    public void onClickDelete(View _) {
        Log.i(TAG, "onClickDelete()");
        mCurrPicIdx = mImgAdapt.removePic(mCurrPicIdx);
    }
    
    public void onClickShow(View _) {
        Log.i(TAG, "onClickShow()");
    }
    
}
