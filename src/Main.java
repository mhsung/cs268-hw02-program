import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.util.Scanner;

import javax.swing.JFrame;


public class Main extends JFrame {
  private static final long serialVersionUID = 1L;
  
  private Manager manager;
  private static final double BOUND = 300;

  public Main(Point[] p) {
    super("Kinetic Delaunay Triangulation");
    
    manager = new Manager(p, BOUND);
    getContentPane().add(manager);
    
    addWindowListener(new WindowAdapter() {
      public void windowClosing(WindowEvent e) {manager.stop();}
    });
    
    pack();
    setResizable(false);
    setVisible(true);
  }
  
  public static void main(String[] args) throws Exception {
    if (args.length == 0) {
      System.out.println("no input file specified");
      System.exit(1);
    }
    Scanner s = new Scanner(new File(args[0]));
    // @taodu: add support for acceleration.
    boolean hasAcce = false;
    if (args.length > 1 && args[1].equals("-a")) {
      hasAcce = true;
    }
    int n = s.nextInt();
    double r = s.nextDouble();
    Point[] p = new Point[n];
    for (int i = 0; i < n; i++) {
      if (hasAcce) {
        p[i] = new Point(s.nextDouble(), s.nextDouble(), s.nextDouble(), s.nextDouble(),
                         s.nextDouble(), s.nextDouble(), r);
      } else {
        p[i] = new Point(s.nextDouble(), s.nextDouble(), s.nextDouble(), s.nextDouble(), r);
      }
    }
    s.close();
    new Main(p);
  }
}
