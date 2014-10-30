import java.awt.Color;
import java.awt.Graphics2D;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Set;

/*
 * Uses a halfedge data structure, where all edges are oriented counterclockwise.
 */
public class Triangulation {
  public static class Face {
    public Edge edge = null;
    
    private LocationTriangle lt = null;
    
    public String toString() {return edge.vertex + " " + edge.next.vertex + " " + edge.next.next.vertex;}
  }
  
  public static class Vertex {
    public Point p;
    public Edge edge = null;
    
    public Vertex(Point p) {this.p = p;}
    
    public String toString() {return p.toString();}
  }
  
  public static class Edge {
    public Vertex vertex = null;
    public Edge pair = null;
    public Face face = null;
    public Edge next = null;
    
    public Edge withFace(Face f) {this.face = f; return this;}
    public Edge withVertex(Vertex v) {this.vertex = v; return this;}
    
    public String toString() {return vertex + " " + next.vertex;}
  }
  
  private Set<Face> faces = new HashSet<Face>();
  private Set<Vertex> vertices = new HashSet<Vertex>();
  private Set<Edge> edges = new HashSet<Edge>();
  
  private Set<Vertex> boundaryVertices = new HashSet<Vertex>();
  
  private boolean maintainLocations = true;
  
  /*
   * Triangulation assumes that the points always remain within 
   * the rectangle [-bound, bound] x [-bound, bound].
   * 
   * For ease of computation, we create a very large triangle to encompass
   * the entire region. This means that the triangulation around the convex
   * hull of the points may not be strictly accurate, but it also means
   * we can treat convex hull change events as flip events for edges using
   * the bounding points. This bounding triangle is therefore retained in
   * our structure, and silently suppressed in rendering.
   * 
   * This constructor runs in expected O(n lg n) time, provided the points
   * are randomly ordered in the collection.
   */
  public Triangulation(Collection<Point> points, double bound) {
    Face f = newFace();
    
    Vertex v0 = newVertex(new Point(-10000 * bound, -10000 * bound));
    Vertex v1 = newVertex(new Point(10000 * bound, -10000 * bound));
    Vertex v2 = newVertex(new Point(0, 10000 * bound));
    
    boundaryVertices.add(v0);
    boundaryVertices.add(v1);
    boundaryVertices.add(v2);
    
    Edge e0 = newEdge().withFace(f).withVertex(v0);
    Edge e1 = newEdge().withFace(f).withVertex(v1);
    Edge e2 = newEdge().withFace(f).withVertex(v2);
    
    f.edge = e0;
    
    v0.edge = e0;
    v1.edge = e1;
    v2.edge = e2;
    
    e0.next = e1;
    e1.next = e2;
    e2.next = e0;
    
    LocationTriangle root = new LocationTriangle(f);
    f.lt = root;
    
    for (Point p : points) {
      f = root.getFace(p);
      
      Vertex v = newVertex(p);
      
      Queue<Edge> suspect = new LinkedList<Edge>();
      
      // split face
      Edge[] es = new Edge[6];
      for (int i = 0; i < 6; i++) es[i] = newEdge();
      
      v.edge = es[1];
      
      Edge e = f.edge;
      suspect.add(e);
      Edge ne = e.next;
      e.next = es[0];
      e.next.face = e.face;
      e.next.vertex = ne.vertex;
      e.next.pair = es[3];
      e.next.next = es[1];
      e.next.next.face = e.face;
      e.next.next.vertex = v;
      e.next.next.pair = es[4];
      e.next.next.next = e;
      e = ne;
      suspect.add(e);
      ne = e.next;
      Face f1 = newFace();
      f1.edge = e;
      e.face = f1;
      e.next = es[2];
      e.next.face = e.face;
      e.next.vertex = ne.vertex;
      e.next.pair = es[5];
      e.next.next = es[3];
      e.next.next.face = e.face;
      e.next.next.vertex = v;
      e.next.next.pair = es[0];
      e.next.next.next = e;
      e = ne;
      suspect.add(e);
      ne = e.next;
      Face f2 = newFace();
      f2.edge = e;
      e.face = f2;
      e.next = es[4];
      e.next.face = e.face;
      e.next.vertex = ne.vertex;
      e.next.pair = es[1];
      e.next.next = es[5];
      e.next.next.face = e.face;
      e.next.next.vertex = v;
      e.next.next.pair = es[2];
      e.next.next.next = e;
      
      LocationTriangle t0 = new LocationTriangle(f);
      LocationTriangle t1 = new LocationTriangle(f1);
      LocationTriangle t2 = new LocationTriangle(f2);
      
      f.lt.addChild(t0);
      f.lt.addChild(t1);
      f.lt.addChild(t2);
      
      f.lt = t0;
      f1.lt = t1;
      f2.lt = t2;
      
      while (!suspect.isEmpty()) {
        e = suspect.remove();
        Edge pair = e.pair;
        if (pair == null) continue;
        if (p.inCircle(pair.vertex.p, pair.next.vertex.p, pair.next.next.vertex.p) > 0) {
          edgeFlip(e);
          suspect.add(e.next);
          suspect.add(pair.next.next);
        }
      }
    }
    
    maintainLocations = false;
  }
  
  private static class LocationTriangle {
    private Point a, b, c;
    private Face f;
    public LocationTriangle(Face f) {
      this.f = f;
      this.a = f.edge.vertex.p;
      this.b = f.edge.next.vertex.p;
      this.c = f.edge.next.next.vertex.p;
    }
    
    private ArrayList<LocationTriangle> children = new ArrayList<LocationTriangle>();
    
    public void addChild(LocationTriangle child) {children.add(child);}
    
    public Face getFace(Point p) {
      for (LocationTriangle child : children) {
        if (p.inTriangle(child.a, child.b, child.c)) return child.getFace(p);
      }
      return f;
    }
  }
  
  private Face newFace() {
    Face f = new Face();
    faces.add(f);
    return f;
  }
  
  private Edge newEdge() {
    Edge e = new Edge();
    edges.add(e);
    return e;
  }
  
  private Vertex newVertex(Point p) {
    Vertex v = new Vertex(p);
    vertices.add(v);
    return v;
  }
  
  /*
   * Before:
   *     /|\
   *    / | \
   *   /a | d\
   *  /  e|f  \
   *  \   |   /
   *   \b | c/
   *    \ | /
   *     \|/
   * 
   * 
   * After:
   *     / \
   *    /   \
   *   /a   d\
   *  /___e___\
   *  \   f   /
   *   \b   c/
   *    \   /
   *     \ /
   * 
   * Remember that all edges point counterclockwise.
   */
  public void edgeFlip(Edge e) {
    assert edges.contains(e);
    assert e.pair != null;
    
    Edge f = e.pair;
    e.face.edge = e;
    f.face.edge = f;
    
    Edge a = e.next;
    Edge b = a.next;
    Edge c = e.pair.next;
    Edge d = c.next;
    
    e.vertex = b.vertex;
    e.next = d;
    f.vertex = d.vertex;
    f.next = b;
    
    a.next = e;
    a.vertex.edge = a;
    c.next = f;
    c.vertex.edge = c;
    b.next = c;
    b.face = c.face;
    d.next = a;
    d.face = a.face;
    
    if (maintainLocations) {
      LocationTriangle et = new LocationTriangle(e.face);
      LocationTriangle ft = new LocationTriangle(f.face);
      e.face.lt.addChild(et);
      e.face.lt.addChild(ft);
      f.face.lt.addChild(et);
      f.face.lt.addChild(ft);
      e.face.lt = et;
      f.face.lt = ft;
    }
  }
  
  public void render(Graphics2D g, Color edgeColor, Color boundaryColor, Color vertexColor) {
    for (Edge e : edges) {
      g.setColor(edgeColor);
      if (touchesBoundary(e)) g.setColor(boundaryColor);
      Point p1 = e.vertex.p;
      Point p2 = e.next.vertex.p;
      g.drawLine((int) p1.x, (int) p1.y, (int) p2.x, (int) p2.y); 
    }
    
    g.setColor(vertexColor);
    for (Vertex v : vertices) {
      if (isBoundary(v)) continue;
      g.fillOval((int) (v.p.x - v.p.r), (int) (v.p.y - v.p.r), (int) (2 * v.p.r), (int) (2 * v.p.r));
    }
  }
  
  public void render(Graphics2D g) {
    render(g, Color.BLACK, Color.RED, Color.BLUE);
  }
  
  public boolean touchesBoundary(Edge e) {
    return isBoundary(e.vertex) || isBoundary(e.next.vertex);
  }
  
  public boolean isBoundary(Edge e) {
    return e.pair == null;
  }
  
  public boolean isBoundary(Vertex v) {
    return boundaryVertices.contains(v);
  }
  
  public boolean isOnHull(Vertex v) {
    Set<Vertex> neighbors = getNeighbors(v);
    neighbors.retainAll(boundaryVertices);
    return !neighbors.isEmpty();
  }
  
  public Set<Vertex> getNeighbors(Vertex v) {
    assert vertices.contains(v);
    assert !boundaryVertices.contains(v);
    Set<Vertex> answer = new HashSet<Vertex>();
    Edge first = v.edge;
    Edge cur = first;
    answer.add(cur.pair.vertex);
    while (cur.pair.next != first) {
      cur = cur.pair.next;
      answer.add(cur.pair.vertex);
    }
    return answer;
  }
  
  public Set<Edge> getOutgoingEdges(Vertex v) {
    assert vertices.contains(v);
    assert !boundaryVertices.contains(v);
    Set<Edge> answer = new HashSet<Edge>();
    Edge first = v.edge;
    Edge cur = first;
    answer.add(cur);
    while (cur.pair.next != first) {
      answer.add(cur = cur.pair.next);
    }
    return answer;
  }
  
  public Set<Face> getFaces() {
    return Collections.unmodifiableSet(faces);
  }
  
  public Set<Vertex> getVertices() {
    return Collections.unmodifiableSet(vertices);
  }
  
  public Set<Edge> getEdges() {
    return Collections.unmodifiableSet(edges);
  }

  // @mhsung
  public Vertex findVertex(Point p) {
    for (Vertex v : getVertices()) {
      if (v.p == p) {
        return v;
      }
    }

    return null;
  }
}
