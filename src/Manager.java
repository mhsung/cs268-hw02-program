import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Toolkit;
import java.awt.geom.AffineTransform;
import java.util.HashMap;
import java.util.HashSet;
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
  
  private Triangulation triangulation;
  private EventQueue queue;
  
  private Map<Point, ReflectEvent> horizontalReflectEvents = new HashMap<Point, ReflectEvent>();
  private Map<Point, ReflectEvent> verticalReflectEvents = new HashMap<Point, ReflectEvent>();
  
  private Map<Point, Map<Point, CollideEvent>> collideEvents = new HashMap<Point, Map<Point, CollideEvent>>();
  
  
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
  
  private void initQueue() {
    for (Point p : points) {
      collideEvents.put(p, new HashMap<Point, CollideEvent>());
    }
    
    for (Point p : points) {
      addReflectEvents(p);
      
      for (Point p2 : points) {
        if (p2 == p) break;
        CollideEvent e = getCollideInstance(p, p2);
        collideEvents.get(p).put(p2, e);
        collideEvents.get(p2).put(p, e);
        queue.add(e);
      }
    }
  }
  
  
  /*
   * Removes and recomputes all events associated with the given Point.
   * This must be called whenever a Point's trajectory changes.
   */
  public void invalidate(Point p) {
    // update collisions with walls
    removeReflectEvents(p);
    
    addReflectEvents(p);
    
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
    triangulation = new Triangulation(points, bound);
  }
  
  private void updatePoints(double delta) {
    for (Point p : points) {
      p.x += p.vx * delta;
      p.y += p.vy * delta;
    }
    time += delta;
  }
  
  public ReflectEvent getReflectInstance(Point p, boolean isHorizontal) {
    if (isHorizontal) {
      if (p.vx > 0) return new ReflectEvent(p, isHorizontal, this, time + (bound - p.x - p.r) / p.vx);
      else if (p.vx < 0) return new ReflectEvent(p, isHorizontal, this, time - (p.x - p.r + bound) / p.vx);
      else return null;
    } else {
      if (p.vy > 0) {
        return new ReflectEvent(p, isHorizontal, this, time + (bound - p.y - p.r) / p.vy);
      }
      else if (p.vy < 0) return new ReflectEvent(p, isHorizontal, this, time - (p.y - p.r + bound) / p.vy);
      else return null;
    }
  }
  
  public CollideEvent getCollideInstance(Point p1, Point p2) {
    double dx = p1.x - p2.x;
    double dy = p1.y - p2.y;
    double dvx = p1.vx - p2.vx;
    double dvy = p1.vy - p2.vy;
    
    double a = dvx * dvx + dvy * dvy;
    if (a == 0) return null;
    double b = dx * dvx + dy * dvy;
    double c = dx * dx + dy * dy - (p1.r + p2.r) * (p1.r + p2.r);
    
    double disc = b * b - a * c;
    if (disc <= 0) return null;
    double t = (-b - Math.sqrt(disc)) / a;
    if (t <= 1e-6) return null;
    
    return new CollideEvent(p1, p2, this, time + t);
  }
  
  public EdgeFlipEvent getEdgeFlipInstance(Triangulation.Edge e) {
    if (triangulation.isBoundary(e)) return null;
    
    Point a = e.vertex.p;
    Point b = e.next.vertex.p;
    Point c = e.next.next.vertex.p;
    Point d = e.pair.next.next.vertex.p;
    Poly ax = new Poly(new double[] {a.x, a.vx}), ay = new Poly(new double[] {a.y, a.vy});
    Poly bx = new Poly(new double[] {b.x, b.vx}), by = new Poly(new double[] {b.y, b.vy});
    Poly cx = new Poly(new double[] {c.x, c.vx}), cy = new Poly(new double[] {c.y, c.vy});
    Poly dx = new Poly(new double[] {d.x, d.vx}), dy = new Poly(new double[] {d.y, d.vy});
    Poly az = Poly.add(Poly.mult(ax, ax), Poly.mult(ay, ay));
    Poly bz = Poly.add(Poly.mult(bx, bx), Poly.mult(by, by));
    Poly cz = Poly.add(Poly.mult(cx, cx), Poly.mult(cy, cy));
    Poly dz = Poly.add(Poly.mult(dx, dx), Poly.mult(dy, dy));
    
    Poly inCircle = Poly.add(Poly.subtract(Poly.det3(ax, ay, az, bx, by, bz, cx, cy, cz),
                                           Poly.det3(ax, ay, az, bx, by, bz, dx, dy, dz)),
                             Poly.subtract(Poly.det3(ax, ay, az, cx, cy, cz, dx, dy, dz),
                                           Poly.det3(bx, by, bz, cx, cy, cz, dx, dy, dz)));
    
    double t = inCircle.firstPositiveAscendingRoot();
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