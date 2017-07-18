package org.jabref.gui.util;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.collections.transformation.TransformationList;

public final class MappedList<A, B> extends TransformationList<A, B> {

    private final Function<B, A> mapper;

    public MappedList(ObservableList<? extends B> source, Function<B, A> mapper) {
        super(source);
        this.mapper = mapper;
    }

    @Override
    protected void sourceChanged(ListChangeListener.Change<? extends B> change) {
        fireChange(new ListChangeListener.Change<A>(this) {

            @Override
            public boolean wasAdded() {
                return change.wasAdded();
            }

            @Override
            public boolean wasRemoved() {
                return change.wasRemoved();
            }

            @Override
            public boolean wasReplaced() {
                return change.wasReplaced();
            }

            @Override
            public boolean wasUpdated() {
                return change.wasUpdated();
            }

            @Override
            public boolean wasPermutated() {
                return change.wasPermutated();
            }

            @Override
            public int getPermutation(int i) {
                return change.getPermutation(i);
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
            public List<A> getRemoved() {
                List<A> result = new ArrayList<>(change.getRemovedSize());
                for (B element : change.getRemoved()) {
                    result.add(mapper.apply(element));
                }
                return result;
            }

            @Override
            public int getFrom() {
                return change.getFrom();
            }

            @Override
            public int getTo() {
                return change.getTo();
            }

            @Override
            public boolean next() {
                return change.next();
            }

            @Override
            public void reset() {
                change.reset();
            }
        });
    }

    @Override
    public int getSourceIndex(int index) {
        return index;
    }

    @Override
    public A get(int index) {
        return mapper.apply(super.getSource().get(index));
    }

    @Override
    public int size() {
        return super.getSource().size();
    }
}
