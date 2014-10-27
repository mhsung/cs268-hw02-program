
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
    Triangulation.Vertex a = e.vertex;
    Triangulation.Vertex b = e.next.vertex;
    System.out.println("Flipped: " + a.p + " - " + b.p);

    t.edgeFlip(e);

    // @mhsung
    manager.invalidate(e.next);
    manager.invalidate(e.next.next);
    manager.invalidate(e.pair.next);
    manager.invalidate(e.pair.next.next);

    manager.addEdgeFlipEvents(e);

    //if(!t.isBoundary(a)) manager.invalidate(a.p);
    //if(!t.isBoundary(b)) manager.invalidate(b.p);
    //if(!t.isBoundary(c)) manager.invalidate(c.p);
    //if(!t.isBoundary(d)) manager.invalidate(d.p);
  }
}
