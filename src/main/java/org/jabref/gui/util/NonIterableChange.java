package org.jabref.gui.util;

import java.util.List;

import javafx.collections.ListChangeListener.Change;
import javafx.collections.ObservableList;

/**
 * This class replicates the necessary functionality of {@link com.sun.javafx.collections.NonIterableChange NonIterableChange},
 * which is part of an internal JavaFX package and cannot be imported or used directly.
 * It is used by {@link org.jabref.gui.util.CustomFilteredList CustomFilteredList}.
 */
public abstract class NonIterableChange<E> extends Change<E> {

    private static final int[] EMPTY_PERM = new int[0];
    private final int from;
    private final int to;
    private boolean invalid = true;

    protected NonIterableChange(int from, int to, ObservableList<E> list) {
        super(list);
        this.from = from;
        this.to = to;
    }

    @Override
    public int getFrom() {
        checkState();
        return from;
    }

    @Override
    public int getTo() {
        checkState();
        return to;
    }

    @Override
    protected int[] getPermutation() {
        checkState();
        return EMPTY_PERM;
    }

    @Override
    public boolean next() {
        if (invalid) {
            invalid = false;
            return true;
        }
        return false;
    }

    @Override
    public void reset() {
        invalid = true;
    }

    public void checkState() {
        if (invalid) {
            throw new IllegalStateException("Invalid Change state: next() must be called before inspecting the Change.");
        }
    }

    public static class GenericAddRemoveChange<E> extends NonIterableChange<E> {

        private final List<E> removed;

        public GenericAddRemoveChange(int from, int to, List<E> removed, ObservableList<E> list) {
            super(from, to, list);
            this.removed = removed;
        }

        @Override
        public List<E> getRemoved() {
            checkState();
            return removed;
        }
    }
}
