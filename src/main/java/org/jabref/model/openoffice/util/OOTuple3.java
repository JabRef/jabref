package org.jabref.model.openoffice.util;

/**
 * This class allows three objects to be packed together, and later accessed as fields `a`, `b` and
 * `c`.
 *
 * Can be used to avoid creating a new class for just this purpose.
 *
 * Can be useful if you do not have `Trifunction` at hand but need to pass three objects at a time.
 *
 */
public class OOTuple3<A, B, C> {

    public final A a;
    public final B b;
    public final C c;

    public OOTuple3(A a, B b, C c) {
        this.a = a;
        this.b = b;
        this.c = c;
    }
}

