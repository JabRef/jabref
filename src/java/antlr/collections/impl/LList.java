package antlr.collections.impl;

/* ANTLR Translator Generator
 * Project led by Terence Parr at http://www.jGuru.com
 * Software rights: http://www.antlr.org/license.html
 *
 * $Id$
 */

import antlr.collections.List;
import antlr.collections.Stack;

import java.util.Enumeration;
import java.util.NoSuchElementException;

import antlr.collections.impl.LLCell;

/**A Linked List Implementation (not thread-safe for simplicity)
 * (adds to the tail) (has an enumeration)
 */
public class LList implements List, Stack {
    protected LLCell head = null, tail = null;
    protected int length = 0;


    /** Add an object to the end of the list.
     * @param o the object to add
     */
    public void add(Object o) {
        append(o);
    }

    /** Append an object to the end of the list.
     * @param o the object to append
     */
    public void append(Object o) {
        LLCell n = new LLCell(o);
        if (length == 0) {
            head = tail = n;
            length = 1;
        }
        else {
            tail.next = n;
            tail = n;
            length++;
        }
    }

    /**Delete the object at the head of the list.
     * @return the object found at the head of the list.
     * @exception NoSuchElementException if the list is empty.
     */
    protected Object deleteHead() throws NoSuchElementException {
        if (head == null) throw new NoSuchElementException();
        Object o = head.data;
        head = head.next;
        length--;
        return o;
    }

    /**Get the ith element in the list.
     * @param i the index (from 0) of the requested element.
     * @return the object at index i
     * NoSuchElementException is thrown if i out of range
     */
    public Object elementAt(int i) throws NoSuchElementException {
        int j = 0;
        for (LLCell p = head; p != null; p = p.next) {
            if (i == j) return p.data;
            j++;
        }
        throw new NoSuchElementException();
    }

    /**Return an enumeration of the list elements */
    public Enumeration elements() {
        return new LLEnumeration(this);
    }

    /** How high is the stack? */
    public int height() {
        return length;
    }

    /** Answers whether or not an object is contained in the list
     * @param o the object to test for inclusion.
     * @return true if object is contained else false.
     */
    public boolean includes(Object o) {
        for (LLCell p = head; p != null; p = p.next) {
            if (p.data.equals(o)) return true;
        }
        return false;
    }
    // The next two methods make LLQueues and LLStacks easier.

    /** Insert an object at the head of the list.
     * @param o the object to add
     */
    protected void insertHead(Object o) {
        LLCell c = head;
        head = new LLCell(o);
        head.next = c;
        length++;
        if (tail == null) tail = head;
    }

    /**Return the length of the list.*/
    public int length() {
        return length;
    }

    /** Pop the top element of the stack off.
     * @return the top of stack that was popped off.
     * @exception NoSuchElementException if the stack is empty.
     */
    public Object pop() throws NoSuchElementException {
        Object o = deleteHead();
        return o;
    }
    // Satisfy the Stack interface now.

    /** Push an object onto the stack.
     * @param o the object to push
     */
    public void push(Object o) {
        insertHead(o);
    }

    public Object top() throws NoSuchElementException {
        if (head == null) throw new NoSuchElementException();
        return head.data;
    }
}
