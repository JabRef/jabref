package antlr.collections.impl;

/* ANTLR Translator Generator
 * Project led by Terence Parr at http://www.jGuru.com
 * Software rights: http://www.antlr.org/license.html
 *
 * $Id$
 */

import java.util.Hashtable;
import java.util.Enumeration;

import antlr.collections.impl.Vector;

/**
 * A simple indexed vector: a normal vector except that you must
 * specify a key when adding an element.  This allows fast lookup
 * and allows the order of specification to be preserved.
 */
public class IndexedVector {
    protected Vector elements;
    protected Hashtable index;


    /**
     * IndexedVector constructor comment.
     */
    public IndexedVector() {
        elements = new Vector(10);
        index = new Hashtable(10);
    }

    /**
     * IndexedVector constructor comment.
     * @param size int
     */
    public IndexedVector(int size) {
        elements = new Vector(size);
        index = new Hashtable(size);
    }

    public synchronized void appendElement(Object key, Object value) {
        elements.appendElement(value);
        index.put(key, value);
    }

    /**
     * Returns the element at the specified index.
     * @param index the index of the desired element
     * @exception ArrayIndexOutOfBoundsException If an invalid
     * index was given.
     */
    public Object elementAt(int i) {
        return elements.elementAt(i);
    }

    public Enumeration elements() {
        return elements.elements();
    }

    public Object getElement(Object key) {
        Object o = index.get(key);
        return o;
    }

    /** remove element referred to by key NOT value; return false if not found. */
    public synchronized boolean removeElement(Object key) {
        Object value = index.get(key);
        if (value == null) {
            return false;
        }
        index.remove(key);
        elements.removeElement(value);
        return false;
    }

    public int size() {
        return elements.size();
    }
}
