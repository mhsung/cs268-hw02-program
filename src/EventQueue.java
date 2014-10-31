import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;


/*
 * We need a custom implementation of a priority queue in order to have log-time arbitrary removal.
 */
public class EventQueue {
  private Map<Event, Integer> idxtable = new HashMap<Event, Integer>();
  private ArrayList<Event> arr = new ArrayList<Event>();
  
  public EventQueue() {
    arr.add(null);
  }
  
  public void add(Event e) {
    if (e == null) return;
    int idx = arr.size();
    arr.add(e);
    idxtable.put(e, idx);
    while (idx > 1 && arr.get(idx >> 1).getTime() > arr.get(idx).getTime()) {
      swap(idx >> 1, idx);
      idx >>= 1;
    }
  }
  
  public Event peek() {return arr.get(1);}
  
  public Event remove() {
    return remove(1);
  }
  
  public Event remove(Event e) {
    if (!idxtable.containsKey(e)) return null;
    return remove(idxtable.get(e));
  }
  
  public boolean contains(Event e) {
    return idxtable.containsKey(e);
  }
  
  private Event remove(int idx) {
    int last = arr.size() - 1;
    swap(idx, last);
    while (idx > 1 && arr.get(idx >> 1).getTime() > arr.get(idx).getTime()) {
      swap(idx >> 1, idx);
      idx >>= 1;
    }
    while ((idx << 1) < last) {
      double t = arr.get(idx).getTime();
      if ((idx << 1) + 1 == last) {
        if (t > arr.get(idx << 1).getTime()) swap(idx, idx << 1);
        break;
      }
      
      double t1 = arr.get(idx << 1).getTime();
      double t2 = arr.get((idx << 1) + 1).getTime();
      if (t > t1 || t > t2) {
        if (t1 < t2) {
          swap(idx, idx << 1);
          idx = (idx << 1);
        } else {
          swap(idx, (idx << 1) + 1);
          idx = (idx << 1) + 1;
        }
      } else {
        break;
      }
    }
    
    
    Event ans = arr.remove(last);
    idxtable.remove(ans);
    return ans;
  }
  
  private void swap(int i, int j) {
    Event temp = arr.get(i); arr.set(i, arr.get(j)); arr.set(j, temp);
    idxtable.put(arr.get(i), i);
    idxtable.put(arr.get(j), j);
  }
  
  public boolean isEmpty() {return arr.size() == 1;}
  
  public int size() {return arr.size() - 1;}
}
