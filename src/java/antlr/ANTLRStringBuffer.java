package antlr;

/* ANTLR Translator Generator
 * Project led by Terence Parr at http://www.jGuru.com
 * Software rights: http://www.antlr.org/license.html
 *
 * $Id$
 */

// Implementation of a StringBuffer-like object that does not have the
// unfortunate side-effect of creating Strings with very large buffers.

public class ANTLRStringBuffer {
    protected char[] buffer = null;
    protected int length = 0;		// length and also where to store next char


    public ANTLRStringBuffer() {
        buffer = new char[50];
    }

    public ANTLRStringBuffer(int n) {
        buffer = new char[n];
    }

    public final void append(char c) {
        // This would normally be  an "ensureCapacity" method, but inlined
        // here for speed.
        if (length >= buffer.length) {
            // Compute a new length that is at least double old length
            int newSize = buffer.length;
            while (length >= newSize) {
                newSize *= 2;
            }
            // Allocate new array and copy buffer
            char[] newBuffer = new char[newSize];
            for (int i = 0; i < length; i++) {
                newBuffer[i] = buffer[i];
            }
            buffer = newBuffer;
        }
        buffer[length] = c;
        length++;
    }

    public final void append(String s) {
        for (int i = 0; i < s.length(); i++) {
            append(s.charAt(i));
        }
    }

    public final char charAt(int index) {
        return buffer[index];
    }

    final public char[] getBuffer() {
        return buffer;
    }

    public final int length() {
        return length;
    }

    public final void setCharAt(int index, char ch) {
        buffer[index] = ch;
    }

    public final void setLength(int newLength) {
        if (newLength < length) {
            length = newLength;
        }
        else {
            while (newLength > length) {
                append('\0');
            }
        }
    }

    public final String toString() {
        return new String(buffer, 0, length);
    }
}
