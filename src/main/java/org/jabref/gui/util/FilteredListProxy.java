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
    private static boolean initialized = false;
    private static Method BEGIN_CHANGE_METHOD;
    private static Method END_CHANGE_METHOD;
    private static Method NEXT_ADD_METHOD;
    private static Method NEXT_UPDATE_METHOD;
    private static Method NEXT_REMOVE_METHOD;
    private static Method REFILTER_METHOD;
    private static Method ENSURE_SIZE_METHOD;
    private static Method GET_PREDICATE_IMPL_METHOD;
    private static Field FILTERED_FIELD;
    private static Field SIZE_FIELD;

    public static void refilterListReflection(FilteredList<BibEntryTableViewModel> filteredList) {
        try {
            if (!initialized) {
                initReflection();
            }
            REFILTER_METHOD.invoke(filteredList);
        } catch (Exception e) {
            LOGGER.warn("Could not refilter list", e);
        }
    }

    public static void refilterListReflection(FilteredList<BibEntryTableViewModel> filteredList, int sourceFrom, int sourceTo) {
        try {
            if (!initialized) {
                initReflection();
            }
            if (sourceFrom < 0 || sourceTo > filteredList.getSource().size() || sourceFrom > sourceTo) {
                throw new IndexOutOfBoundsException();
            }

            BEGIN_CHANGE_METHOD.invoke(filteredList);
            ENSURE_SIZE_METHOD.invoke(filteredList, filteredList.getSource().size());

            @SuppressWarnings("unchecked")
            Predicate<BibEntryTableViewModel> predicateImpl = (Predicate<BibEntryTableViewModel>) GET_PREDICATE_IMPL_METHOD.invoke(filteredList);
            ListIterator<? extends BibEntryTableViewModel> it = filteredList.getSource().listIterator(sourceFrom);

            int[] filtered = (int[]) FILTERED_FIELD.get(filteredList);
            int size = (int) SIZE_FIELD.get(filteredList);

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
                    NEXT_UPDATE_METHOD.invoke(filteredList, pos);
                } else if (passedBefore) {
                    NEXT_REMOVE_METHOD.invoke(filteredList, pos, el);
                    System.arraycopy(filtered, pos + 1, filtered, pos, size - pos - 1);
                    size--;
                } else if (passedNow) {
                    int insertionPoint = ~pos;
                    System.arraycopy(filtered, insertionPoint, filtered, insertionPoint + 1, size - insertionPoint);
                    filtered[insertionPoint] = i;
                    NEXT_ADD_METHOD.invoke(filteredList, insertionPoint, insertionPoint + 1);
                    size++;
                }
            }

            // Write back
            FILTERED_FIELD.set(filteredList, filtered);
            SIZE_FIELD.set(filteredList, size);

            END_CHANGE_METHOD.invoke(filteredList);
        } catch (ReflectiveOperationException e) {
            LOGGER.warn("Could not refilter list", e);
        }
    }

    private static void initReflection() throws NoSuchMethodException, NoSuchFieldException {
        BEGIN_CHANGE_METHOD = ObservableListBase.class.getDeclaredMethod("beginChange");
        END_CHANGE_METHOD = ObservableListBase.class.getDeclaredMethod("endChange");
        NEXT_ADD_METHOD = ObservableListBase.class.getDeclaredMethod("nextAdd", int.class, int.class);
        NEXT_UPDATE_METHOD = ObservableListBase.class.getDeclaredMethod("nextUpdate", int.class);
        NEXT_REMOVE_METHOD = ObservableListBase.class.getDeclaredMethod("nextRemove", int.class, Object.class);

        REFILTER_METHOD = FilteredList.class.getDeclaredMethod("refilter");
        ENSURE_SIZE_METHOD = FilteredList.class.getDeclaredMethod("ensureSize", int.class);
        GET_PREDICATE_IMPL_METHOD = FilteredList.class.getDeclaredMethod("getPredicateImpl");

        FILTERED_FIELD = FilteredList.class.getDeclaredField("filtered");
        SIZE_FIELD = FilteredList.class.getDeclaredField("size");

        BEGIN_CHANGE_METHOD.setAccessible(true);
        END_CHANGE_METHOD.setAccessible(true);
        NEXT_ADD_METHOD.setAccessible(true);
        NEXT_UPDATE_METHOD.setAccessible(true);
        NEXT_REMOVE_METHOD.setAccessible(true);

        REFILTER_METHOD.setAccessible(true);
        ENSURE_SIZE_METHOD.setAccessible(true);
        GET_PREDICATE_IMPL_METHOD.setAccessible(true);

        FILTERED_FIELD.setAccessible(true);
        SIZE_FIELD.setAccessible(true);

        initialized = true;
    }
}
