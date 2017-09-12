package org.jabref.gui.util;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.collections.transformation.TransformationList;

/**
 * MappedList implementation based on https://github.com/corda/corda/blob/master/client/jfx/src/main/kotlin/net/corda/client/jfx/utils/MappedList.kt
 */
public final class MappedList<A, B> extends TransformationList<A, B> {

    private final Function<B, A> mapper;
    private final List<A> backingList;

    public MappedList(ObservableList<? extends B> sourceList, Function<B, A> mapper) {
        super(sourceList);
        this.mapper = mapper;
        this.backingList = new ArrayList<>(sourceList.size());
        sourceList.stream().map(mapper::apply).forEach(backingList::add);
    }

    @Override
    protected void sourceChanged(ListChangeListener.Change<? extends B> change) {
        beginChange();
        while (change.next()) {
            if (change.wasPermutated()) {
                int from = change.getFrom();
                int to = change.getTo();

                // get permutation array
                int[] permutation = new int[to - from];
                for (int i = 0; i < to - from; i++) {
                    permutation[i] = change.getPermutation(i);
                }

                // perform permutation
                Object[] permutedPart = new Object[to - from];
                for (int i = from; i < to; i++) {
                    permutedPart[permutation[i]] = backingList.get(i);
                }

                // update backingList
                for (int i = 0; i < to; i++) {
                    backingList.set(i + from, (A) permutedPart[i]);
                }
                nextPermutation(from, to, permutation);
            } else if (change.wasUpdated()) {
                backingList.set(change.getFrom(), mapper.apply(getSource().get(change.getFrom())));
                nextUpdate(change.getFrom());
            } else {
                if (change.wasRemoved()) {
                    int removePosition = change.getFrom();
                    List<A> removed = new ArrayList<>(change.getRemovedSize());
                    for (int i = 0; i < change.getRemovedSize(); i++) {
                        removed.add(backingList.remove(removePosition));
                    }
                    nextRemove(change.getFrom(), removed);
                }
                if (change.wasAdded()) {
                    int addStart = change.getFrom();
                    int addEnd = change.getTo();
                    for (int i = addStart; i < addEnd; i++) {
                        backingList.add(i, mapper.apply(change.getList().get(i)));
                    }
                    nextAdd(addStart, addEnd);
                }
            }
        }
        endChange();
    }

    @Override
    public int getSourceIndex(int index) {
        return index;
    }

    @Override
    public A get(int index) {
        return backingList.get(index);
    }

    @Override
    public int size() {
        return backingList.size();
    }
}
