import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Toolkit;
import java.awt.geom.AffineTransform;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.JPanel;


public class Manager extends JPanel implements Runnable {
  // TODO: Modify this file as needed to incorporate EdgeFlipEvents and stop doing the
  // additional work that EdgeFlipEvents makes unnecessary
  // You may find it useful to maintain a map from Points to Triangulation.Vertex objects
  private Set<Point> points = new HashSet<Point>();
  private double time = 0;
  private double bound;

  // @taodu: epsilon to tolerate numerical issues.
  private static double EPSILON = 1e-6;
  
  private Triangulation triangulation;
  private EventQueue queue;
  
  private Map<Point, ReflectEvent> horizontalReflectEvents = new HashMap<Point, ReflectEvent>();
  private Map<Point, ReflectEvent> verticalReflectEvents = new HashMap<Point, ReflectEvent>();
  
  private Map<Point, Map<Point, CollideEvent>> collideEvents = new HashMap<Point, Map<Point, CollideEvent>>();
  //@mhsung
  // A new map for edge flip events in the queue.
  private Map<Triangulation.Edge, EdgeFlipEvent> edgeFlipEvents = new HashMap<Triangulation.Edge, EdgeFlipEvent>();
  
  
  public Manager(Point[] p, double bound) {
    super();
    
    // GUI code; safe to ignore
    this.pWidth = (int) (2 * bound);
    this.pHeight = (int) (2 * bound);
    setBackground(Color.WHITE);
    setPreferredSize(new Dimension(pWidth, pHeight));
    
    // Add the points in random order.
    this.bound = bound;
    for (int i = 0; i < p.length; i++) {
      assert p[i].x + p[i].r < bound;
      assert p[i].x - p[i].r > -bound;
      assert p[i].y + p[i].r < bound;
      assert p[i].y - p[i].r > -bound;
      points.add(p[i]);
    }
    
    // Initialize the data structures.
    triangulation = new Triangulation(points, bound);
    queue = new EventQueue();
    initQueue();
  }
  
  public void addReflectEvents(Point p) {
    if (horizontalReflectEvents.containsKey(p)) return;
    ReflectEvent evt = getReflectInstance(p, true);
    horizontalReflectEvents.put(p, evt);
    queue.add(evt);
    evt = getReflectInstance(p, false);
    verticalReflectEvents.put(p, evt);
    queue.add(evt);
  }
  
  public void removeReflectEvents(Point p) {
    queue.remove(horizontalReflectEvents.remove(p));
    queue.remove(verticalReflectEvents.remove(p));
  }

  // @mhsung
  public void addCollisionEvents(Point p, Point p2) {
    assert p != null;
    assert p2 != null;
    assert p2 != p;

    if (collideEvents.get(p).get(p2) != null) return;

    assert collideEvents.get(p2).get(p) == null;
    CollideEvent evt = getCollideInstance(p, p2);
    collideEvents.get(p).put(p2, evt);
    collideEvents.get(p2).put(p, evt);
    queue.add(evt);
  }

  // @mhsung
  public void removeCollisionEvents(Point p, Point p2) {
    CollideEvent evt = collideEvents.get(p).get(p2);
    if (evt == null) {
      assert collideEvents.get(p2).get(p) == null;
    } else {
      assert collideEvents.get(p2).get(p) != null;
      collideEvents.get(p).remove(p2);
      collideEvents.get(p2).remove(p);
      queue.remove(evt);
    }
  }

  // @mhsung
  public void addEdgeFlipEvents(Triangulation.Edge e) {
    assert edgeFlipEvents.get(e.pair) == null;
    EdgeFlipEvent evt = getEdgeFlipInstance(e);
    edgeFlipEvents.put(e, evt);
    queue.add(evt);
  }

  private void initQueue() {
    for (Point p : points) {
      collideEvents.put(p, new HashMap<Point, CollideEvent>());
    }

    // @mhsung
    // Add reflection events in the queue only when the point is on the convex
    // hull.
    for (Point p : points) {
      //addReflectEvents(p);

      Triangulation.Vertex v = triangulation.findVertex(p);
      assert v != null;
      if (triangulation.isOnHull(v))
        addReflectEvents(p);
    }

    //@mhsung
    // Add collision events in the queue only when the two points are on the
    // triangulation edge.
    /*
    for (Point p : points) {
      for (Point p2 : points) {
        if (p2 == p) continue;
        CollideEvent e = getCollideInstance(p, p2);
        collideEvents.get(p).put(p2, e);
        collideEvents.get(p2).put(p, e);
        queue.add(e);
      }
    }
    */

    for(Triangulation.Edge e : triangulation.getEdges()) {
      Triangulation.Vertex v = e.vertex;
      Triangulation.Vertex v2 = e.next.vertex;
      if (!triangulation.isBoundary(v) && !triangulation.isBoundary(v2)) {
        if (collideEvents.get(v.p).get(v2.p) == null) {
          assert collideEvents.get(v2.p).get(v.p) == null;
          addCollisionEvents(v.p, v2.p);
        }
      }
    }

    //@mhsung
    // Add edge flip events in the queue.
    edgeFlipEvents.clear();

    for(Triangulation.Edge e : triangulation.getEdges()) {
      if (!triangulation.isBoundary(e) && edgeFlipEvents.get(e.pair) == null) {
        EdgeFlipEvent evt = getEdgeFlipInstance(e);
        if(evt != null) {
          edgeFlipEvents.put(e, evt);
          queue.add(evt);
        }
      }
    }
  }
  
  /*
   * Removes and recomputes all events associated with the given Point.
   * This must be called whenever a Point's trajectory changes.
   */
  public void invalidate(Point p) {
    // @mhsung
    if (horizontalReflectEvents.containsKey(p)) {
      // update collisions with walls
      removeReflectEvents(p);
      addReflectEvents(p);
    }

    // @mhsung
    // Update collision events for the triangulation edges.
    /*
    // update collisions with other points
    for (Map.Entry<Point, CollideEvent> e : collideEvents.get(p).entrySet()) {
      collideEvents.get(e.getKey()).remove(p);
      queue.remove(e.getValue());
    }
    collideEvents.get(p).clear();
    
    for (Point p2 : points) {
      if (p2 == p) continue;
      CollideEvent e = getCollideInstance(p, p2);
      collideEvents.get(p).put(p2, e);
      collideEvents.get(p2).put(p, e);
      queue.add(e);
    }
    */

    List<Point> p2_list = new LinkedList<Point>();
    for (Map.Entry<Point, CollideEvent> e : collideEvents.get(p).entrySet()) {
      collideEvents.get(e.getKey()).remove(p);
      queue.remove(e.getValue());
      p2_list.add(e.getKey());
    }
    collideEvents.get(p).clear();

    for (Point p2 : p2_list) {
      assert p2 != p;
      addCollisionEvents(p, p2);
    }

    // @mhsung
    // Update edge flip events when the edge is one of out-going edges, or the
    // next of one of out-going edges of the invalidated vertex.
    Triangulation.Vertex v = triangulation.findVertex(p);
    assert v != null;

    Set<Triangulation.Edge> edges = triangulation.getOutgoingEdges(v);
    for(Triangulation.Edge e : edges) {
      assert e.next != null;
      invalidate(e);
      invalidate(e.next);
    }
  }

  // @mhsung
  // Invalidate edge.
  public void invalidate(Triangulation.Edge e) {
    queue.remove(edgeFlipEvents.get(e));
    queue.remove(edgeFlipEvents.get(e.pair));

    edgeFlipEvents.remove(e);
    edgeFlipEvents.remove(e.pair);

    EdgeFlipEvent evt = getEdgeFlipInstance(e);
    if(evt != null) {
      edgeFlipEvents.put(e, evt);
      queue.add(evt);
    }
  }
  
  public void update() {
    double finalTime = time + DELTA;
    while (!queue.isEmpty() && queue.peek().getTime() <= finalTime) {
      Event cur = queue.remove();
      updatePoints(cur.getTime() - time);
      cur.process();
    }
    
    updatePoints(finalTime - time);
    
    // TODO: This line should be removed once you have edge flip events working.
    //triangulation = new Triangulation(points, bound);
  }

  // @taodu:
  private void updatePoints(double delta) {
    for (Point p : points) {
      double vx0 = p.vx;
      double vy0 = p.vy;
      p.vx += p.ax * delta;
      p.vy += p.ay * delta;
      p.x += (vx0 + p.vx) * delta / 2;
      p.y += (vy0 + p.vy) * delta / 2;
    }
    time += delta;
  }

  // @taodu
  private double getReflectTime(Point p, boolean isHorizontal) {
    double t = Double.POSITIVE_INFINITY;
    Poly poly;
    double z = bound - p.r;
    double a = isHorizontal ? p.ax : p.ay;
    double v = isHorizontal ? p.vx : p.vy;
    double z0 = isHorizontal ? p.x : p.y;
    poly = new Poly(new double[]{z0 - z, v, 0.5 * a});
    ArrayList<Double> root = poly.positiveRoots();
    while (root.size() > 0 && root.get(0) < EPSILON) root.remove(0);
    if (root.size() > 0) t = root.get(0);
    poly = new Poly(new double[]{z0 + z, v, 0.5 * a});
    root = poly.positiveRoots();
    while (root.size() > 0 && root.get(0) < EPSILON) root.remove(0);
    if (root.size() > 0) t = Math.min(t, root.get(0));
    return t;
  }

  // @taodu
  public ReflectEvent getReflectInstance(Point p, boolean isHorizontal) {
    double t = getReflectTime(p, isHorizontal);
    if (t == Double.POSITIVE_INFINITY) return null;
    else return new ReflectEvent(p, isHorizontal, this, time + t);
  }

  // @taodu
  public CollideEvent getCollideInstance(Point p1, Point p2) {
    double x = p1.x - p2.x;
    double y = p1.y - p2.y;
    double vx = p1.vx - p2.vx;
    double vy = p1.vy - p2.vy;
    double ax = p1.ax - p2.ax;
    double ay = p1.ay - p2.ay;
    double r = p1.r + p2.r;

    // x(t) = x + vx * t + 0.5 * ax * t^2
    // y(t) = y + vy * t + 0.5 * ay * t^2
    // x^2 + y^2 == r^2
    Poly polyx = new Poly(new double[]{x, vx, 0.5 * ax});
    Poly polyy = new Poly(new double[]{y, vy, 0.5 * ay});
    Poly poly = Poly.add(Poly.mult(polyx, polyx), Poly.mult(polyy, polyy));
    Poly polyr = new Poly(new double[]{r * r});
    poly = Poly.subtract(poly, polyr);
    ArrayList<Double> root = poly.positiveRoots();
    while (root.size() > 0 && root.get(0) < EPSILON) root.remove(0);
    if (root.size() == 0) return null;
    double t = root.get(0);
    return new CollideEvent(p1, p2, this, time + t);
  }

  // @taodu
  public EdgeFlipEvent getEdgeFlipInstance(Triangulation.Edge e) {
    if (triangulation.isBoundary(e)) return null;
    
    Point a = e.vertex.p;
    Point b = e.next.vertex.p;
    Point c = e.next.next.vertex.p;
    Point d = e.pair.next.next.vertex.p;
    Poly ax = new Poly(new double[] {a.x, a.vx, 0.5 * a.ax}), ay = new Poly(new double[] {a.y, a.vy, 0.5 * a.ay});
    Poly bx = new Poly(new double[] {b.x, b.vx, 0.5 * b.ax}), by = new Poly(new double[] {b.y, b.vy, 0.5 * b.ay});
    Poly cx = new Poly(new double[] {c.x, c.vx, 0.5 * c.ax}), cy = new Poly(new double[] {c.y, c.vy, 0.5 * c.ay});
    Poly dx = new Poly(new double[] {d.x, d.vx, 0.5 * d.ax}), dy = new Poly(new double[] {d.y, d.vy, 0.5 * d.ay});
    Poly az = Poly.add(Poly.mult(ax, ax), Poly.mult(ay, ay));
    Poly bz = Poly.add(Poly.mult(bx, bx), Poly.mult(by, by));
    Poly cz = Poly.add(Poly.mult(cx, cx), Poly.mult(cy, cy));
    Poly dz = Poly.add(Poly.mult(dx, dx), Poly.mult(dy, dy));
    
    Poly inCircle = Poly.add(Poly.subtract(Poly.det3(ax, ay, az, bx, by, bz, cx, cy, cz),
                                           Poly.det3(ax, ay, az, bx, by, bz, dx, dy, dz)),
                             Poly.subtract(Poly.det3(ax, ay, az, cx, cy, cz, dx, dy, dz),
                                           Poly.det3(bx, by, bz, cx, cy, cz, dx, dy, dz)));

    ArrayList<Double> roots = inCircle.positiveRoots();
    int cur = 0;
    Poly de = inCircle.derivative();
    while (cur < roots.size() && (roots.get(cur) < EPSILON || de.eval(roots.get(cur)) <= 0)) cur++;
    double t = cur < roots.size() ? roots.get(cur) : Double.NaN;
    if (Double.isNaN(t)) return null;
    return new EdgeFlipEvent(triangulation, e, this, time + t);
  }
  
  //// Everything below this line deals with animation and can be ignored. ////
  // Large portions of this code come from http://fivedots.coe.psu.ac.th/~ad/jg/
  
  private static final long serialVersionUID = 1L;

  private int pWidth, pHeight;   // dimensions of the panel
  
  private static final int FPS = 60;
  private static final double DELTA = 1.0 / FPS;
  private static final long PERIOD = 1000000000L / FPS;
  private static final int MAX_FRAME_SKIPS = 5;
  
  private Graphics2D dbg; 
  private Image dbImage = null;
  private Thread animator = null;
  
  private boolean running;
  
  private void paintScreen()
  // use active rendering to put the buffered image on-screen
  { 
    Graphics g;
    try {
      g = this.getGraphics();
      if ((g != null) && (dbImage != null))
        g.drawImage(dbImage, 0, 0, null);
      // Sync the display on some systems.
      // (on Linux, this fixes event queue problems)
      Toolkit.getDefaultToolkit().sync();
      g.dispose();
    }
    catch (Exception e)   // quite commonly seen at applet destruction
    { System.out.println("Graphics error: " + e);  }
  } // end of paintScreen()
  
  public void stop() {running = false;}
  
  public void addNotify() {
    super.addNotify();   // creates the peer
    start();    // start the thread
  }

  private void start() {
    if (animator == null || !running) {
      animator = new Thread(this);
      animator.start();
    }
  }
  
  public void run() {
    long beforeTime, afterTime, timeDiff, sleepTime;
    long overSleepTime = 0L;
    long excess = 0L;

    beforeTime = System.nanoTime();
  
    running = true;
  
    while(running) {
      update(); 
      render();
      paintScreen();  // draw the buffer on-screen
  
      afterTime = System.nanoTime();
      timeDiff = afterTime - beforeTime;
      sleepTime = (PERIOD - timeDiff) - overSleepTime;  
  
      if (sleepTime > 0) {   // some time left in this cycle
        try {
          Thread.sleep(sleepTime/1000000L);  // nano -> ms
        }
        catch(InterruptedException ex){}
        overSleepTime = (System.nanoTime() - afterTime) - sleepTime;
      } else {    // sleepTime <= 0; the frame took longer than the period
        excess -= sleepTime;  // store excess time value
        overSleepTime = 0L;
      }
  
      beforeTime = System.nanoTime();
  
        /* If frame animation is taking too long, update the game state
           without rendering it, to get the updates/sec nearer to
           the required FPS. */
      int skips = 0;
      while((excess > PERIOD) && (skips < MAX_FRAME_SKIPS)) {
        excess -= PERIOD;
        update();    // update state but don't render
        skips++;
      }
    }

    System.exit(0);   // so window disappears
  }
  
  public void render() {
    if (dbImage == null){
      dbImage = createImage(pWidth, pHeight);
      if (dbImage == null) {
        System.out.println("dbImage is null");
        return;
      } else {
        dbg = (Graphics2D) dbImage.getGraphics();
        AffineTransform xfrm = AffineTransform.getTranslateInstance(bound, bound);
        xfrm.concatenate(new AffineTransform(new double[] {1.0,0.0,0.0,-1.0}));
        dbg.setTransform(xfrm);
      }
        
    }

    // clear the background
    dbg.setColor(Color.WHITE);
    dbg.fillRect((int) -bound, (int) -bound, (int) (2 * bound), (int) (2 * bound));
    dbg.setColor(Color.BLACK);
    dbg.drawRect((int) -bound+1, (int) -bound+1, (int) (2 * bound) - 2, (int) (2 * bound) - 2);
    
    triangulation.render(dbg);
  }
}
