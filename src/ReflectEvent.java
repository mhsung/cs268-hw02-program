
public class ReflectEvent extends Event {
  private Point p;
  private boolean isHorizontal;
  
  public ReflectEvent(Point p, boolean isHorizontal, Manager manager, double time) {
    super(manager, time);
    this.p = p;
    this.isHorizontal = isHorizontal;
  }
  
  public void process() {
    if (isHorizontal) {
      p.vx *= -1;
    } else {
      p.vy *= -1;
    }
    
    manager.invalidate(p);
  }
}
