package hu.ureczky.utils;

import android.graphics.Matrix;

public class TransformUtils {
    public static Matrix remapToDevice(int width, int height) {
        return createMatrix3(
            1,  0, -width/2,
            0, -1, height/2,
            0,  0, 1
        );
    }
    
    public static Matrix remapFromDevice(int width, int height) {
        return createMatrix3(
            1,  0, width/2,
            0, -1, height/2,
            0,  0, 1 
        );
    }
    
    public static void remap(Matrix M, float w, float h/*, boolean yflip*/){
        
        Matrix pre = TransformUtils.createMatrix3(
            1, 0, -w/2,
            0, 1, -h/2,
            0, 0, 1
        );
        
        Matrix post = TransformUtils.createMatrix3(
            1, 0, w/2,
            0, 1, h/2,
            0, 0, 1
        );
        
        M.preConcat(pre);
        M.postConcat(post);
    }
    
    public static Matrix createMatrix3(
        float m11, float m12, float m13,
        float m21, float m22, float m23,
        float m31, float m32, float m33
    ){
        Matrix m = new Matrix();
        m.setValues(new float[]{
            m11, m12, m13,
            m21, m22, m23,
            m31, m32, m33
        });
        return m;
    }
    
    /**
     * Interpolate two matrices element-by-element linearly (warning: this is probably not the result you expected). 
     * @param m1 matrix 1
     * @param m2 matrix 2
     * @param ratio ratio of the weights. It should be in the interval of <b>0..1</b>.
     *        <br/>If <b>m1</b> matrix is weighted by <b>w1</b> and <b>m2</b> matrix is weighted by <b>w2</b>, then <b>ratio = w2/w1</b>.
     *        <br/>Example: <b>0</b> means <b>m1</b>, <b>1</b> means <b>m2</b> will be resulted.
     * @return interpolated matrix: <b>(1-ratio) * m1 + ratio * m2</b>
     */
    public static Matrix interpolateLinear(Matrix m1, Matrix m2, float ratio){
    
        float[] a1 = new float[9];
        float[] a2 = new float[9];
        float[] a3 = new float[9];
        m1.getValues(a1);
        m2.getValues(a2);
        for(int i=0; i<9; i++){
            a3[i] = (1-ratio) * a1[i] + ratio * a2[i];
        }
        Matrix m3 = new Matrix();
        m3.setValues(a3);
        return m3;
    }
}
