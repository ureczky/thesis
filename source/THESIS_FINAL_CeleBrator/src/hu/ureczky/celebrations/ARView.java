package hu.ureczky.celebrations;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.view.View;

import hu.ureczky.utils.CameraUtils;
import hu.ureczky.utils.SensorUtils;

public class ARView extends View {
    
    private Paint mPaint;
    private Paint mPaintTarget;
    private Paint mPaintTargetOutline;
    
    private State mState;
    private SensorWatcher mSensorWatcher;
    private Canvas mCanvas;
    
    private int w,h;
    double fovY2Tan;
    
    private static final float DP_TO_PX = Resources.getSystem().getDisplayMetrics().densityDpi / 160f;

    public ARView(Context context, State state, SensorWatcher sensorWatcher) {
        super(context);
        mState = state;
        mSensorWatcher = sensorWatcher;
        setFocusable(true);

        mPaint = new Paint();
        mPaint.setAntiAlias(true);
        mPaint.setStrokeWidth(1 * DP_TO_PX);
        mPaint.setTextSize(16);
        mPaint.setTextAlign(Paint.Align.RIGHT);
        
        // For target objects: sun, moon
        mPaintTarget = new Paint();
        mPaintTarget.setAntiAlias(true);
        mPaintTarget.setStrokeWidth(0);
        mPaintTarget.setColor(Color.BLACK);
        //mPaintTarget.setAlpha(128); //50%
        mPaintTarget.setAlpha(0); //transparent
        
        mPaintTargetOutline = new Paint();
        mPaintTargetOutline.setAntiAlias(true);
        mPaintTargetOutline.setStrokeWidth(1 * DP_TO_PX);
        mPaintTargetOutline.setColor(Color.GREEN);
        mPaintTargetOutline.setStyle(Paint.Style.STROKE);
    }

    private void drawScene() {
        mCanvas.save();
        mCanvas.translate(w/2, h/2);
        mCanvas.scale(1, -1);
            
            drawPlane();
            
            
        mCanvas.restore();
    }
    
    void drawPlane() {
        float[] orientation = mSensorWatcher.getOrientation();
        double azimuthRad   = Math.toRadians(orientation[0]);
        double elevationRad = Math.toRadians(orientation[1] + 90); //TODO
        float  rollDeg       = orientation[2];
        
        double eps = elevationRad;
        double epsTan = Math.tan(eps);
        double fovYRad = (mState.mFovYRad * 100 / mState.mZoomPercent);
        double fovY2Tan = Math.tan(fovYRad/2);
        double hr_0 = epsTan / fovY2Tan; // Height Ratio for 0 point
        double hr_inf = 1 / epsTan / fovY2Tan; // Height Ratio for infinity point
        
        float y_inf =  (float)(h*hr_inf/2);
        float y_0   = -(float)(h*hr_0/2);
                
        float r = 3 * DP_TO_PX;
        
        
        float f = mState.mFocalLength_px;
        float dT = (float) (f * Math.tan(elevationRad));
        
        float dia = (float) Math.sqrt(w*w + h*h); //diameter in pixels
        
        mCanvas.save();
        mCanvas.rotate(-rollDeg);
            
            mPaint.setColor(Color.RED);
            
            // Orthopoint
            mCanvas.drawCircle(0, y_0, r, mPaint);
            
            // Horizon
            mCanvas.drawLine(-dia/2, y_inf, dia/2, y_inf, mPaint);
                        
            // T: compass-azimuth
            float azimDeg0 = mState.mOrientation[0];
            for(int azimDeg = 0; azimDeg < 360; azimDeg += 5) {
                
                //azim:0..360 -> angleDiff: -180..180
                float angleDiffDeg = SensorUtils.azimToSignedDeg(azimDeg - azimDeg0);
                
                if(Math.abs(angleDiffDeg) < 120/2) { // only show the visibles. TODO Math.max(fovXDeg,fovYDeg)/2, but the current!
                 
                    float dx = (float) (f * Math.tan(Math.toRadians(angleDiffDeg)) / Math.sin(elevationRad));
                    
                    float e = 4 * DP_TO_PX;
                    mCanvas.drawLine(dx, y_inf-e, dx, y_inf+e, mPaint);
                    //mCanvas.drawCircle(dx, y_inf, r, mPaint);
                    
                    
                    String text = azimDeg + "°";
                    float textSize = 10 * DP_TO_PX;
                    mPaint.setTextSize(textSize);
                    
                    float sx = mPaint.measureText(text, 0, text.length()) / 2; // align text center horizontally width this correction
                    
                    mCanvas.scale(1, -1);
                    mCanvas.drawText(text, dx + sx, -y_inf + 15 * DP_TO_PX, mPaint);
                    mCanvas.scale(1, -1);
                    
                    //mCanvas.drawLine(dx, y_inf, 0, y_0, mPaint);
                }
            }
            
            mCanvas.drawLine(0, -y_0, 0, y_0, mPaint);
            mCanvas.drawLine(0, y_inf, 0, y_0, mPaint);
            
            // Aim
            drawLookAt();
            
        mCanvas.restore();
    }
    
    void drawLookAt() {
        double targetAngleDeg = Math.toRadians(0.5); // Sun/Moons view angle, approximately 0.5 degree. TODO get the calculated value
        float rx = (float) (mState.mFocalLength_px * Math.tan(targetAngleDeg / 2)); // tan(fovy/2);
        
        float r = 2 * rx * DP_TO_PX; 
        float d1 = 0.1f * rx * DP_TO_PX; // in
        float d2 = 0.5f * rx * DP_TO_PX; // out
        mCanvas.drawCircle(0, 0, r, mPaintTarget);
        mCanvas.drawCircle(0, 0, r, mPaintTargetOutline);
        
        mCanvas.drawLine(-r-d2,  0,   -r+d1,   0,   mPaintTargetOutline); // Left
        mCanvas.drawLine(+r-d1,  0,   +r+d2,   0,   mPaintTargetOutline); // Right
        mCanvas.drawLine(0,    -r-d2,   0,   -r+d1, mPaintTargetOutline); // Top
        mCanvas.drawLine(0,    +r-d1,   0,   +r+d2, mPaintTargetOutline); // Bootom
    }
    
    @Override protected void onDraw(Canvas canvas) {
        mCanvas = canvas;
        h = canvas.getHeight();
        w = canvas.getWidth();
        fovY2Tan = Math.tan(mState.mFovYRad/2);
        
        // Clear canvas
        canvas.drawColor(0);
        
        drawScene();
    }
}