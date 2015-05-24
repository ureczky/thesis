package hu.ureczky.orthocam;

public class MathUtil {
    
    /* GCD: Greatest Common Divisor */
    public static int gcd(int a, int b){
        return a>b ? lnko_ordered(a,b) : lnko_ordered(b,a);
    }
    
    /* Helper method for GCD, where a >= b */
    public static int lnko_ordered(int a, int b){
        if (b==0) return a;
        return lnko_ordered(b, a%b);
    }
    
}
