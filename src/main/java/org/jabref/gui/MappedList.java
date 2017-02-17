package org.jabref.gui;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import javafx.collections.ListChangeListener.Change;
import javafx.collections.ObservableList;
import javafx.collections.transformation.TransformationList;

/**
 * Mapped view of a {@link ObservableList}.
 *
 * It is a planned to include a similar version of this class in the Java core, see
 * https://bugs.openjdk.java.net/browse/JDK-8091967
 * Until it is available in Java, we use a own implementation.
 * Source code taken from
 * https://gist.github.com/TomasMikula/8883719#file-mappedlist-java-L1
 *
 * @param <E> source type
 * @param <F> target type
 */
public class MappedList<E, F> extends TransformationList<F, E> {

    private final Function<E, F> mapper;

    public MappedList(ObservableList<? extends E> source, Function<E, F> mapper) {
        super(source);
        this.mapper = mapper;
    }

    @Override
    public int getSourceIndex(int index) {
        return index;
    }

    @Override
    public F get(int index) {
        return mapper.apply(getSource().get(index));
    }

    @Override
    public int size() {
        return getSource().size();
    }

    @Override
    protected void sourceChanged(Change<? extends E> c) {
        fireChange(new Change<F>(this) {

            @Override
            public boolean wasAdded() {
                return c.wasAdded();
            }

            @Override
            public boolean wasRemoved() {
                return c.wasRemoved();
            }

            @Override
            public boolean wasReplaced() {
                return c.wasReplaced();
            }

            @Override
            public boolean wasUpdated() {
                return c.wasUpdated();
            }

            @Override
            public boolean wasPermutated() {
                return c.wasPermutated();
            }

            @Override
            public int getPermutation(int i) {
                return c.getPermutation(i);
            }

            @Override
            protected int[] getPermutation() {
                // This method is only called by the superclass methods
                // wasPermutated() and getPermutation(int), which are
                // both overriden by this class. There is no other way
                // this method can be called.
                throw new AssertionError("Unreachable code");
            }

            @Override
            public List<F> getRemoved() {
                ArrayList<F> res = new ArrayList<>(c.getRemovedSize());
                for(E e: c.getRemoved()) {
                    res.add(mapper.apply(e));
                }
                return res;
            }

            @Override
            public int getFrom() {
                return c.getFrom();
            }

            @Override
            public int getTo() {
                return c.getTo();
            }

            @Override
            public boolean next() {
                return c.next();
            }

            @Override
            public void reset() {
                c.reset();
            }
        });
    }
}
