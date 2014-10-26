
public abstract class Event {
  protected Manager manager;
  private double time;
  
  public Event(Manager manager, double time) {
    this.manager = manager;
    this.time = time;
  }
  
  public abstract void process();
  
  public double getTime() {return time;}
}
