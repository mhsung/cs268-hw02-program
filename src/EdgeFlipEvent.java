
public class EdgeFlipEvent extends Event {
  private Triangulation t;
  private Triangulation.Edge e;
  
  public EdgeFlipEvent(Triangulation t, Triangulation.Edge e, Manager manager, double time) {
    super(manager, time);
    this.t = t;
    this.e = e;
  }
  
  public void process() {
    // TODO: Your code goes here.
    t.edgeFlip(e);
  }
}
