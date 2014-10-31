
public class CollideEvent extends Event {
  private Point p1;
  private Point p2;
  
  public CollideEvent(Point p1, Point p2, Manager manager, double time) {
    super(manager, time);
    this.p1 = p1;
    this.p2 = p2;
  }
  
  public void process() {
    double theta = Math.atan2(p2.y - p1.y, p2.x - p1.x);
    double vx1 = p1.vx * Math.cos(theta) * Math.cos(theta) + p1.vy * Math.sin(theta) * Math.cos(theta);
    double vy1 = p1.vx * Math.cos(theta) * Math.sin(theta) + p1.vy * Math.sin(theta) * Math.sin(theta);
    
    double vx2 = p2.vx * Math.cos(Math.PI+theta) * Math.cos(Math.PI+theta) + p2.vy * Math.sin(Math.PI+theta) * Math.cos(Math.PI+theta);
    double vy2 = p2.vx * Math.cos(Math.PI+theta) * Math.sin(Math.PI+theta) + p2.vy * Math.sin(Math.PI+theta) * Math.sin(Math.PI+theta);
    
    p1.vx += vx2 - vx1;
    p1.vy += vy2 - vy1;
    p2.vx += vx1 - vx2;
    p2.vy += vy1 - vy2;
    
    manager.invalidate(p1);
    manager.invalidate(p2);
  }
}
