package org.jabref.gui.util.uithreadaware;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

import javafx.beans.InvalidationListener;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;

/**
 * This class can be used to wrap an @see ObservableList inside it. When wrapped, any Listener listening for updates to the wrapped ObservableList (for example because of a binding to it) is ensured to be notified on the JavaFX Application Thread. It should be used to implement bindings where updates come in from a background thread but should be reflected in the UI where it is necessary that changes to the UI are performed on the JavaFX Application thread.
 *
 * @param <E> the type of the elements of the wrapped ObservableList.
 */
public class UiThreadObservableList<E> implements ObservableList<E> {

    private final ObservableList<E> delegate;

    public UiThreadObservableList(ObservableList<E> delegate) {
        this.delegate = delegate;
    }

    @Override
    public void addListener(ListChangeListener<? super E> listener) {
        delegate.addListener(new UiThreadListChangeListener(listener));
    }

    @Override
    public void removeListener(ListChangeListener<? super E> listener) {
        delegate.removeListener(listener);
    }

    @Override
    public boolean addAll(E... elements) {
        return delegate.addAll(elements);
    }

    @Override
    public boolean setAll(E... elements) {
        return delegate.setAll(elements);
    }

    @Override
    public boolean setAll(Collection<? extends E> col) {
        return delegate.setAll(col);
    }

    @Override
    public boolean removeAll(E... elements) {
        return delegate.removeAll(elements);
    }

    @Override
    public boolean retainAll(E... elements) {
        return delegate.retainAll(elements);
    }

    @Override
    public void remove(int from, int to) {
        delegate.remove(from, to);
    }

    @Override
    public int size() {
        return delegate.size();
    }

    @Override
    public boolean isEmpty() {
        return delegate.isEmpty();
    }

    @Override
    public boolean contains(Object o) {
        return delegate.contains(o);
    }

    @Override
    public Iterator<E> iterator() {
        return delegate.iterator();
    }

    @Override
    public Object[] toArray() {
        return delegate.toArray();
    }

    @Override
    public <T> T[] toArray(T[] a) {
        return delegate.toArray(a);
    }

    @Override
    public boolean add(E e) {
        return delegate.add(e);
    }

    @Override
    public boolean remove(Object o) {
        return delegate.remove(o);
    }

    @Override
    public boolean containsAll(Collection<?> c) {
        return delegate.containsAll(c);
    }

    @Override
    public boolean addAll(Collection<? extends E> c) {
        return delegate.addAll(c);
    }

    @Override
    public boolean addAll(int index, Collection<? extends E> c) {
        return delegate.addAll(index, c);
    }

    @Override
    public boolean removeAll(Collection<?> c) {
        return delegate.removeAll(c);
    }

    @Override
    public boolean retainAll(Collection<?> c) {
        return delegate.retainAll(c);
    }

    @Override
    public void clear() {
        delegate.clear();
    }

    @Override
    public E get(int index) {
        return delegate.get(index);
    }

    @Override
    public E set(int index, E element) {
        return delegate.set(index, element);
    }

    @Override
    public void add(int index, E element) {
        delegate.add(index, element);
    }

    @Override
    public E remove(int index) {
        return delegate.remove(index);
    }

    @Override
    public int indexOf(Object o) {
        return delegate.indexOf(o);
    }

    @Override
    public int lastIndexOf(Object o) {
        return delegate.lastIndexOf(o);
    }

    @Override
    public ListIterator<E> listIterator() {
        return delegate.listIterator();
    }

    @Override
    public ListIterator<E> listIterator(int index) {
        return delegate.listIterator(index);
    }

    @Override
    public List<E> subList(int fromIndex, int toIndex) {
        return delegate.subList(fromIndex, toIndex);
    }

    @Override
    public void addListener(InvalidationListener listener) {
        delegate.addListener(new UiThreadInvalidationListener(listener));
    }

    @Override
    public void removeListener(InvalidationListener listener) {
        delegate.removeListener(listener);
    }
}
