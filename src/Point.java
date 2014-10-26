
public class Point {
  public double x, y;
  public double vx, vy;
  public double r;
  
  public Point(double x, double y) {this(x, y, 0, 0, 0);}
  
  public Point(double x, double y, double vx, double vy) {this(x, y, vx, vy, 0);}
  
  public Point(double x, double y, double r) {this(x, y, 0, 0, r);}
  
  public Point(double x, double y, double vx, double vy, double r) {
    this.x = x;
    this.y = y;
    this.vx = vx;
    this.vy = vy;
    this.r = r;
  }
  
  public String toString() {return "(" + x + "," + y + ")";}
  
  private static double det2(double a, double b, double c, double d) {return a * d - b * c;}
  
  private static double det3(double a, double b, double c, double d, double e, double f, double g, double h, double i) {
    return a * det2(e, f, h, i) - b * det2(d, f, g, i) + c * det2(d, e, g, h);
  }
  
  private static double ccw(Point a, Point b, Point c) {
    return det3(a.x, a.y, a.w, b.x, b.y, b.w, c.x, c.y, c.w);
  }
  
  public boolean inTriangle(Point a, Point b, Point c) {
    a.w = b.w = c.w = this.w = 1;
    return ccw(a, b, this) >= 0 && ccw(b, c, this) >= 0 && ccw(c, a, this) >= 0;
  }
  
  private double w;
  public int inCircle(Point a, Point b, Point c) {
    a.w = a.x * a.x + a.y * a.y;
    b.w = b.x * b.x + b.y * b.y;
    c.w = c.x * c.x + c.y * c.y;
    w = x * x + y * y;
    return (int) Math.signum(-ccw(b, c, this) + ccw(a, c, this) - ccw(a, b, this) + ccw(a, b, c));
  }
}
