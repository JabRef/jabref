package net.sf.jabref;

import java.util.HashMap;

/**
 * HashMap that empties itself upon reaching a maximum entry count. Used to cache
 * name transformations for better performance. By doing this, we accomplish removing
 * old entries that may never be used again.
 * @author Morten O. Alver
 */
public class NameCache extends HashMap {

  int max, toAdd;

  public NameCache(int max_) {
    max = max_;
    toAdd = max_;
  }

  public Object put(Object key, Object o) {
    super.put(key, o);
    if (size() > max) {

      // We have reached the maximum entry count. Thus we remove all entries, and increase the
      // max value.
      clear();
      max += toAdd;
      super.put(key, o);
    }

    return o;
  }
}
