package org.jabref.gui.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.ListIterator;
import java.util.function.Predicate;

import javafx.beans.NamedArg;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ObjectPropertyBase;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.collections.transformation.TransformationList;

/**
 * A custom class that extends {@link javafx.collections.transformation.FilteredList FilteredList} to provide additional functionality.
 * This class closely mirrors the behavior of {@code FilteredList} with the following key differences:
 * <ol>
 * <li>Supports an additional update predicate that allows filtering of elements during {@link javafx.collections.ListChangeListener.Change#wasUpdated()} events.</li>
 * <li>Offers access to the {@link #refilter()} method, enabling explicit re-evaluation of the filter criteria.</li>
 * </ol>
 *
 */
public class CustomFilteredList<E> extends TransformationList<E, E> {

    private int[] filtered;
    private int size;
    private SortHelper helper;
    private ObjectProperty<Predicate<? super E>> defaultPredicate;
    private ObjectProperty<Predicate<? super E>> updatePredicate;

    public CustomFilteredList(@NamedArg("source") ObservableList<E> source, @NamedArg("defaultPredicate") Predicate<? super E> defaultPredicate) {
        super(source);
        filtered = new int[source.size() * 3 / 2 + 1];
        if (defaultPredicate != null) {
            setDefaultPredicate(defaultPredicate);
        } else {
            for (size = 0; size < source.size(); size++) {
                filtered[size] = size;
            }
        }
    }

    public CustomFilteredList(@NamedArg("source") ObservableList<E> source) {
        this(source, null);
    }

    public final ObjectProperty<Predicate<? super E>> defaultPredicateProperty() {
        if (defaultPredicate == null) {
            defaultPredicate = new ObjectPropertyBase<>() {
                @Override
                protected void invalidated() {
                    refilter();
                }

                @Override
                public Object getBean() {
                    return CustomFilteredList.this;
                }

                @Override
                public String getName() {
                    return "defaultPredicate";
                }
            };
        }
        return defaultPredicate;
    }

    public final Predicate<? super E> getDefaultPredicate() {
        return defaultPredicate == null ? null : defaultPredicate.get();
    }

    public final void setDefaultPredicate(Predicate<? super E> defaultPredicate) {
        defaultPredicateProperty().set(defaultPredicate);
    }

    private Predicate<? super E> getDefaultPredicateImpl() {
        if (getDefaultPredicate() != null) {
            return getDefaultPredicate();
        }
        return t -> true;
    }

    public final ObjectProperty<Predicate<? super E>> updatePredicateProperty() {
        if (updatePredicate == null) {
            updatePredicate = new ObjectPropertyBase<>() {
                @Override
                public Object getBean() {
                    return CustomFilteredList.this;
                }

                @Override
                public String getName() {
                    return "updatePredicate";
                }
            };
        }
        return updatePredicate;
    }

    public final Predicate<? super E> getUpdatePredicate() {
        return updatePredicate == getDefaultPredicate() ? null : updatePredicate.get();
    }

    public final void setUpdatePredicate(Predicate<? super E> updatePredicate) {
        updatePredicateProperty().set(updatePredicate);
    }

    private Predicate<? super E> getUpdatePredicateImpl() {
        if (getUpdatePredicate() != null) {
            return getUpdatePredicate();
        }
        return t -> true;
    }

    @Override
    protected void sourceChanged(ListChangeListener.Change<? extends E> c) {
        beginChange();
        while (c.next()) {
            if (c.wasPermutated()) {
                permutate(c);
            } else if (c.wasUpdated()) {
                update(c);
            } else {
                addRemove(c);
            }
        }
        endChange();
    }

    @Override
    public int size() {
        return size;
    }

    @Override
    public E get(int index) {
        if (index >= size) {
            throw new IndexOutOfBoundsException();
        }
        return getSource().get(filtered[index]);
    }

    @Override
    public int getSourceIndex(int index) {
        if (index >= size) {
            throw new IndexOutOfBoundsException();
        }
        return filtered[index];
    }

    @Override
    public int getViewIndex(int index) {
        return Arrays.binarySearch(filtered, 0, size, index);
    }

    private SortHelper getSortHelper() {
        if (helper == null) {
            helper = new SortHelper();
        }
        return helper;
    }

    private int findPosition(int p) {
        if (filtered.length == 0) {
            return 0;
        }
        if (p == 0) {
            return 0;
        }
        int pos = Arrays.binarySearch(filtered, 0, size, p);
        if (pos < 0) {
            pos = ~pos;
        }
        return pos;
    }

    private void ensureSize(int size) {
        if (filtered.length < size) {
            int[] replacement = new int[size * 3 / 2 + 1];
            System.arraycopy(filtered, 0, replacement, 0, this.size);
            filtered = replacement;
        }
    }

    private void updateIndexes(int from, int delta) {
        for (int i = from; i < size; ++i) {
            filtered[i] += delta;
        }
    }

    private void permutate(ListChangeListener.Change<? extends E> c) {
        int from = findPosition(c.getFrom());
        int to = findPosition(c.getTo());

        if (to > from) {
            for (int i = from; i < to; ++i) {
                filtered[i] = c.getPermutation(filtered[i]);
            }

            int[] perm = getSortHelper().sort(filtered, from, to);
            nextPermutation(from, to, perm);
        }
    }

    private void addRemove(ListChangeListener.Change<? extends E> c) {
        Predicate<? super E> predicateImpl = getDefaultPredicateImpl();
        ensureSize(getSource().size());
        final int from = findPosition(c.getFrom());
        final int to = findPosition(c.getFrom() + c.getRemovedSize());

        // Mark the nodes that are going to be removed
        for (int i = from; i < to; ++i) {
            nextRemove(from, c.getRemoved().get(filtered[i] - c.getFrom()));
        }

        // Update indexes of the sublist following the last element that was removed
        updateIndexes(to, c.getAddedSize() - c.getRemovedSize());

        // Replace as many removed elements as possible
        int fpos = from;
        int pos = c.getFrom();

        ListIterator<? extends E> it = getSource().listIterator(pos);
        while (fpos < to && it.nextIndex() < c.getTo()) {
            if (predicateImpl.test(it.next())) {
                filtered[fpos] = it.previousIndex();
                nextAdd(fpos, fpos + 1);
                ++fpos;
            }
        }

        if (fpos < to) {
            // If there were more removed elements than added
            System.arraycopy(filtered, to, filtered, fpos, size - to);
            size -= to - fpos;
        } else {
            // Add the remaining elements
            while (it.nextIndex() < c.getTo()) {
                if (predicateImpl.test(it.next())) {
                    System.arraycopy(filtered, fpos, filtered, fpos + 1, size - fpos);
                    filtered[fpos] = it.previousIndex();
                    nextAdd(fpos, fpos + 1);
                    ++fpos;
                    ++size;
                }
                ++pos;
            }
        }
    }

    /**
     * This method applies the {@link #getUpdatePredicate() updatePredicate} if it is set; otherwise, it uses
     * the {@link #getDefaultPredicate() defaultPredicate}.
     */
    private void update(ListChangeListener.Change<? extends E> c) {
        Predicate<? super E> predicateImpl = getUpdatePredicateImpl();
        ensureSize(getSource().size());
        int sourceFrom = c.getFrom();
        int sourceTo = c.getTo();
        int filterFrom = findPosition(sourceFrom);
        int filterTo = findPosition(sourceTo);
        ListIterator<? extends E> it = getSource().listIterator(sourceFrom);
        int pos = filterFrom;
        while (pos < filterTo || sourceFrom < sourceTo) {
            E el = it.next();
            if (pos < size && filtered[pos] == sourceFrom) {
                if (!predicateImpl.test(el)) {
                    nextRemove(pos, el);
                    System.arraycopy(filtered, pos + 1, filtered, pos, size - pos - 1);
                    --size;
                    --filterTo;
                } else {
                    nextUpdate(pos);
                    ++pos;
                }
            } else {
                if (predicateImpl.test(el)) {
                    nextAdd(pos, pos + 1);
                    System.arraycopy(filtered, pos, filtered, pos + 1, size - pos);
                    filtered[pos] = sourceFrom;
                    ++size;
                    ++pos;
                    ++filterTo;
                }
            }
            sourceFrom++;
        }
    }

    public void refilter() {
        ensureSize(getSource().size());
        List<E> removed = null;
        if (hasListeners()) {
            removed = new ArrayList<>(this);
        }
        size = 0;
        int i = 0;
        Predicate<? super E> predicateImpl = getDefaultPredicateImpl();
        for (final E next : getSource()) {
            if (predicateImpl.test(next)) {
                filtered[size++] = i;
            }
            ++i;
        }
        if (hasListeners()) {
            fireChange(new NonIterableChange.GenericAddRemoveChange<>(0, size, removed, this));
        }
    }
}
