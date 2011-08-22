package net.sf.jabref.oo;

import com.sun.star.awt.Point;

/**
 *
 */
public class ComparableMark implements Comparable {

    String name;
    Point position;

    public ComparableMark(String name, Point position) {
        this.name = name;
        this.position = position;
    }

    public int compareTo(Object o) {
        ComparableMark other = (ComparableMark)o;
        if (position.Y != other.position.Y)
            return position.Y-other.position.Y;
        else
            return position.X-other.position.X;
    }

    public String getName() {
        return name;
    }

    public Point getPosition() {
        return position;
    }
}
