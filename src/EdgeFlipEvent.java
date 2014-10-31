
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
    // @mhsung
    Triangulation.Vertex a = e.vertex;
    Triangulation.Vertex b = e.next.vertex;
    Triangulation.Vertex c = e.next.next.vertex;
    Triangulation.Vertex d = e.pair.next.next.vertex;

    boolean is_a_on_hull_old = !t.isBoundary(a) && t.isOnHull(a);
    boolean is_b_on_hull_old = !t.isBoundary(b) && t.isOnHull(b);
    boolean is_c_on_hull_old = !t.isBoundary(c) && t.isOnHull(c);
    boolean is_d_on_hull_old = !t.isBoundary(d) && t.isOnHull(d);

    t.edgeFlip(e);

    // @mhsung
    boolean is_a_on_hull_new = !t.isBoundary(a) && t.isOnHull(a);
    boolean is_b_on_hull_new = !t.isBoundary(b) && t.isOnHull(b);
    boolean is_c_on_hull_new = !t.isBoundary(c) && t.isOnHull(c);
    boolean is_d_on_hull_new = !t.isBoundary(d) && t.isOnHull(d);

    // Update reflection events.
    assert is_a_on_hull_old || !is_a_on_hull_new;
    assert is_b_on_hull_old || !is_b_on_hull_new;
    if (!is_c_on_hull_old && is_c_on_hull_new) manager.addReflectEvents(c.p);
    if (!is_d_on_hull_old && is_d_on_hull_new) manager.addReflectEvents(d.p);


    // Update collision events.
    if (!t.isBoundary(a) && !t.isBoundary(b)) {
      manager.removeCollisionEvents(a.p, b.p);
    }

    if (!t.isBoundary(c) && !t.isBoundary(d)) {
      manager.addCollisionEvents(c.p, d.p);
    }

    // Update edge flip events.
    manager.invalidate(e.next);
    manager.invalidate(e.next.next);
    manager.invalidate(e.pair.next);
    manager.invalidate(e.pair.next.next);

    manager.addEdgeFlipEvents(e);
  }
}
