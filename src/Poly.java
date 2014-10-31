import java.util.ArrayList;



public class Poly {
  private double[] coeff;
  
  public static Poly ONE = new Poly(new double[] {1.0});
  public static Poly ZERO = new Poly(new double[] {0.0});
  
  public Poly(double[] coeff) {this.coeff = coeff;}
  
  public static Poly add(Poly a, Poly b) {
    double[] coeff = new double[Math.max(a.coeff.length, b.coeff.length)];
    for (int i = 0; i < a.coeff.length; i++) coeff[i] += a.coeff[i];
    for (int i = 0; i < b.coeff.length; i++) coeff[i] += b.coeff[i];
    return new Poly(coeff);
  }
  
  public static Poly subtract(Poly a, Poly b) {
    double[] coeff = new double[Math.max(a.coeff.length, b.coeff.length)];
    for (int i = 0; i < a.coeff.length; i++) coeff[i] += a.coeff[i];
    for (int i = 0; i < b.coeff.length; i++) coeff[i] -= b.coeff[i];
    return new Poly(coeff);
  }
  
  public static Poly mult(Poly a, Poly b) {
    double[] coeff = new double[a.coeff.length + b.coeff.length - 1];
    for (int i = 0; i < a.coeff.length; i++) {
      for (int j = 0; j < b.coeff.length; j++) {
        coeff[i + j] += a.coeff[i] * b.coeff[j];
      }
    }
    return new Poly(coeff);
  }
  
  public static Poly det2(Poly a, Poly b, Poly c, Poly d) {
    return subtract(mult(a, d), mult(b, c));
  }
  
  public static Poly det3(Poly a, Poly b, Poly c, Poly d, Poly e, Poly f, Poly g, Poly h, Poly i) {
    return add(subtract(mult(a, det2(e, f, h, i)), mult(b, det2(d, f, g, i))), mult(c, det2(d, e, g, h)));
  }
  
  public double eval(double t) {
    double answer = 0;
    for (int i = coeff.length - 1; i >= 0; i--) {
      answer *= t;
      answer += coeff[i];
    }
    return answer;
  }
  
  public Poly derivative() {
    if (coeff.length == 1) return Poly.ZERO;
    
    double[] coeff = new double[this.coeff.length - 1];
    for (int i = 0; i < coeff.length; i++) {
      coeff[i] = this.coeff[i + 1] * (i + 1);
    }
    
    return new Poly(coeff);
  }
  
  public ArrayList<Double> positiveRoots() {
    ArrayList<Double> ans = new ArrayList<Double>();
    int degree = coeff.length - 1;
    while (degree > 0 && coeff[degree] == 0) degree--;
    if (degree == 0) return ans;
    if (degree == 1) {
      double root = -coeff[0] / coeff[1];
      if (root > 0) ans.add(root);
      return ans;
    }
    
    ArrayList<Double> cv = derivative().positiveRoots();
    cv.add(0, 0.0);
    double ub = 2 * cv.get(cv.size() - 1) + 1;
    while (Math.signum(eval(ub)) != Math.signum(coeff[degree])) ub *= 2;
    cv.add(ub);
    for (int i = 1; i < cv.size(); i++) {
      double leftSign = Math.signum(eval(cv.get(i-1)));
      double rightSign = Math.signum(eval(cv.get(i)));
      if (leftSign == rightSign) continue;
      
      double low = cv.get(i-1), high = cv.get(i);
      while (low + Math.ulp(low) < high) {
        double mid = (low + high) / 2;
        if (Math.signum(eval(mid)) == leftSign) {
          low = mid;
        } else {
          high = mid;
        }
      }
      ans.add(low);
    }
    
    return ans;
  }
  
  public double firstPositiveAscendingRoot() {
    ArrayList<Double> roots = positiveRoots();
    int cur = 0;
    Poly d = derivative();
    while (cur < roots.size() && d.eval(roots.get(cur)) <= 0) cur++;
    
    return cur < roots.size() ? roots.get(cur) : Double.NaN;
  }
}
