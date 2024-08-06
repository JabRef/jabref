package org.jabref.gui.util;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.ListIterator;
import java.util.function.Predicate;

import javafx.collections.ObservableListBase;
import javafx.collections.transformation.FilteredList;

import org.jabref.gui.maintable.BibEntryTableViewModel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FilteredListProxy {
    private static final Logger LOGGER = LoggerFactory.getLogger(FilteredListProxy.class);

    private FilteredListProxy() {
    }

    public static void refilterListReflection(FilteredList<BibEntryTableViewModel> filteredList) {
        try {
            Method refilter = FilteredList.class.getDeclaredMethod("refilter");
            refilter.setAccessible(true);
            refilter.invoke(filteredList);
        } catch (Exception e) {
            LOGGER.warn("Could not refilter list", e);
        }
    }

    public static void refilterListReflection(FilteredList<BibEntryTableViewModel> filteredList, int sourceFrom, int sourceTo) {
        try {
            if (sourceFrom < 0 || sourceTo > filteredList.getSource().size() || sourceFrom > sourceTo) {
                throw new IndexOutOfBoundsException();
            }

            invoke(filteredList, ObservableListBase.class, "beginChange");

            invoke(filteredList, FilteredList.class, "ensureSize", filteredList.getSource().size());

            @SuppressWarnings("unchecked")
            Predicate<BibEntryTableViewModel> predicateImpl = (Predicate<BibEntryTableViewModel>) invoke(filteredList, FilteredList.class, "getPredicateImpl");
            ListIterator<? extends BibEntryTableViewModel> it = filteredList.getSource().listIterator(sourceFrom);

            Field filteredField = getField("filtered");
            int[] filtered = (int[]) filteredField.get(filteredList);

            Field sizeField = getField("size");
            int size = (int) sizeField.get(filteredList);

            for (int i = sourceFrom; i < sourceTo; ++i) {
                BibEntryTableViewModel el = it.next();
                int pos = Arrays.binarySearch(filtered, 0, size, i);
                boolean passedBefore = pos >= 0;
                boolean passedNow = predicateImpl.test(el);
                /* 1. passed before and now -> nextUpdate
                 * 2. passed before and not now -> nextRemove
                 * 3. not passed before and now -> nextAdd
                 * 4. not passed before and not now -> do nothing */
                if (passedBefore && passedNow) {
                    invoke(filteredList, ObservableListBase.class, "nextUpdate", pos);
                } else if (passedBefore) {
                    invoke(filteredList, ObservableListBase.class, "nextRemove", pos, el);
                    System.arraycopy(filtered, pos + 1, filtered, pos, size - pos - 1);
                    size--;
                } else if (passedNow) {
                    int insertionPoint = ~pos;
                    System.arraycopy(filtered, insertionPoint, filtered, insertionPoint + 1, size - insertionPoint);
                    filtered[insertionPoint] = i;
                    invoke(filteredList, ObservableListBase.class, "nextAdd", insertionPoint, insertionPoint + 1);
                    size++;
                }
            }

            // Write back
            filteredField.set(filteredList, filtered);
            sizeField.set(filteredList, size);

            invoke(filteredList, ObservableListBase.class, "endChange");
        } catch (ReflectiveOperationException e) {
            LOGGER.warn("Could not refilter list", e);
        }
    }

    /**
     * We directly invoke the specified method on the given filteredList
     */
    private static Object invoke(FilteredList<?> filteredList, Class<?> clazz, String methodName, Object... params) throws ReflectiveOperationException {
        // Determine the parameter types for the method lookup
        Class<?>[] paramTypes = new Class[params.length];
        for (int i = 0; i < params.length; i++) {
            paramTypes[i] = params[i].getClass();
            if (paramTypes[i] == Integer.class) {
                // quick hack, because int is converted to Object when calling this method.
                paramTypes[i] = int.class;
            }
        }
        Method method = clazz.getDeclaredMethod(methodName, paramTypes);
        method.setAccessible(true);
        return method.invoke(filteredList, params);
    }

    /**
     * Get the class field (we need it for read and write later)
     */
    private static Field getField(String fieldName) throws ReflectiveOperationException {
        Field field = FilteredList.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        return field;
    }
}
