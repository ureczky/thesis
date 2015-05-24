package hu.ureczky.orthocam;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.view.View;

public class ARView extends View {
    private Paint mPaint;
    private CameraPreview mPreview;

    public ARView(CameraPreview preview) {
        super(preview);
        mPreview = preview;
        setFocusable(true);

        mPaint = new Paint();
        mPaint.setAntiAlias(true);
        mPaint.setStrokeWidth(6);
        mPaint.setTextSize(16);
        mPaint.setTextAlign(Paint.Align.RIGHT);

    }

    private void drawScene(Canvas canvas) {
        
        int h = canvas.getHeight();
        int w = canvas.getWidth();
       
        canvas.save();
        canvas.translate(w/2, h/2);
        canvas.scale(1, -1);
            
            drawPlane(canvas, w, h);
            drawOrigRect(canvas, w, h);
            drawLookAt(canvas);
            
        canvas.restore();

    }
    
    void drawPlane(Canvas canvas, int w, int h){
        double elevation = mPreview.mSensorListener.getElevation();
        float[] g = mPreview.mSensorListener.getGravity();
        
        double roll = Math.atan2(g[0],g[1]);
        
        double eps = elevation;
        double epsTan = Math.tan(eps);
        //float epsCos = (float) Math.cos(eps);
        //float epsSin = (float) Math.sin(eps);
        double fovY2Tan = Math.tan(mPreview.fovY/2);
        double hr_0 = epsTan / fovY2Tan; // Height Ratio for 0 point
        double hr_inf = 1 / epsTan / fovY2Tan; // Height Ratio for infinity point
        
        float y_inf = (float)(h*hr_inf/2);
        float y_0 = -(float)(h*hr_0/2);
        
        int r = 20;
        
        canvas.save();
        canvas.rotate((float) (-roll*180/Math.PI));
            
        // Horizont
        mPaint.setColor(Color.RED);
        canvas.drawLine(-w/2, y_inf, w/2, y_inf, mPaint);
        canvas.drawCircle(0, y_0, r, mPaint);
        
        //drawGrid(canvas, w, h, eps);
        
            
        canvas.restore();
    }
    
    void drawGrid(Canvas canvas, int w, int h, float eps) {
        
        double epsTan = Math.tan(eps);
        float epsCos = (float) Math.cos(eps);
        //float epsSin = (float) Math.sin(eps);
        double fovY2Tan = Math.tan(mPreview.fovY/2);
        double hr_0 = epsTan / fovY2Tan; // Height Ratio for 0 point
        double hr_inf = 1 / epsTan / fovY2Tan; // Height Ratio for infinity point
        
        float y_inf = (float)(h*hr_inf/2);
        float y_0 = -(float)(h*hr_0/2);
        
        double delta = Math.PI/10;
        double deltaTan = Math.tan(delta); 
        float d = (float) (h*deltaTan/fovY2Tan/2);
        
        // Vertical Grid
        int size = 10;
        mPaint.setColor(Color.BLUE);
        for(int i = -size; i <= size; i++) {
            canvas.drawLine(i*d/epsCos, y_0, 0, y_inf, mPaint);
        }
        
        // Horizontal Grid
        for(int i = 0; i <= size; i++) { //TODO 0 elott is
            double dist = i*deltaTan;
            double et = Math.abs(epsTan);
            float hh = (float)(-Math.signum(epsTan)*h*(et-dist)/(1+et*dist)/fovY2Tan/2);
            canvas.drawLine(-w/2, hh, w/2, hh, mPaint);
        }
    }
    
    void drawLookAt(Canvas canvas){
        int r = 20;
        mPaint.setColor(Color.LTGRAY);
        canvas.drawCircle(0, 0, r, mPaint);
    }
    
    void drawOrigRect(Canvas canvas, int w, int h){
        
        //Magic transform
        //Matrix M = mPreview.mSensorListener.getOrthoView(mPreview.f);
        Matrix M = mPreview.getMagicTransformation(w, h, mPreview.f_px, true);
        Matrix Minv = new Matrix();
        M.invert(Minv);
        // Set the transformations origo to the picture's midpoint //TODO ez így jó-e, nem keveredik-e a kétféle középpont fogalom??(preview vs. textureview)
        
        //mPreview.remap(Minv, w, h);
        
        
        float[] src = new float[]{-w/2, h/2, w/2, h/2, w/2, -h/2, -w/2, -h/2};
        float[] dst = new float[8];
        Minv.mapPoints(dst, src);

        mPaint.setColor(Color.RED);
        canvas.drawLine(dst[0], dst[1], dst[2], dst[3], mPaint);
        canvas.drawLine(dst[2], dst[3], dst[4], dst[5], mPaint);
        canvas.drawLine(dst[4], dst[5], dst[6], dst[7], mPaint);
        canvas.drawLine(dst[6], dst[7], dst[0], dst[1], mPaint);
    }

    @Override protected void onDraw(Canvas canvas) {
        canvas.drawColor(0);
        drawScene(canvas);
    }
}