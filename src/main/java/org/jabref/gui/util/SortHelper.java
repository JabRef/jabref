package org.jabref.gui.util;

import java.lang.reflect.Array;
import java.util.Arrays;

/**
 * This class replicates the necessary functionality of {@link com.sun.javafx.collections.SortHelper SortHelper},
 * which is part of an internal JavaFX package and cannot be imported or used directly.
 * It is used by {@link org.jabref.gui.util.CustomFilteredList CustomFilteredList}.
 */
public class SortHelper {
    private static final int INSERTIONSORT_THRESHOLD = 7;
    private int[] permutation;
    private int[] reversePermutation;

    /** used only by {@link CustomFilteredList} */
    public int[] sort(int[] indices, int fromIndex, int toIndex) {
        rangeCheck(indices.length, fromIndex, toIndex);
        int[] aux = copyOfRange(indices, fromIndex, toIndex);
        int[] result = initPermutation(indices.length);
        mergeSort(aux, indices, fromIndex, toIndex, -fromIndex);
        reversePermutation = null;
        permutation = null;
        return Arrays.copyOfRange(result, fromIndex, toIndex);
    }

    private static void rangeCheck(int arrayLen, int fromIndex, int toIndex) {
        if (fromIndex > toIndex) {
            throw new IllegalArgumentException("fromIndex(" + fromIndex + ") > toIndex(" + toIndex + ")");
        }
        if (fromIndex < 0) {
            throw new ArrayIndexOutOfBoundsException(fromIndex);
        }
        if (toIndex > arrayLen) {
            throw new ArrayIndexOutOfBoundsException(toIndex);
        }
    }

    private static int[] copyOfRange(int[] original, int from, int to) {
        int newLength = to - from;
        if (newLength < 0) {
            throw new IllegalArgumentException(from + " > " + to);
        }
        int[] copy = new int[newLength];
        System.arraycopy(original, from, copy, 0, Math.min(original.length - from, newLength));
        return copy;
    }

    private static <T, U> T[] copyOfRange(U[] original, int from, int to, Class<? extends T[]> newType) {
        int newLength = to - from;
        if (newLength < 0) {
            throw new IllegalArgumentException(from + " > " + to);
        }
        T[] copy = (Object) newType == (Object) Object[].class ? (T[]) new Object[newLength]
                : (T[]) Array.newInstance(newType.getComponentType(), newLength);
        System.arraycopy(original, from, copy, 0, Math.min(original.length - from, newLength));
        return copy;
    }

    private void mergeSort(int[] src, int[] dest, int low, int high, int off) {
        int length = high - low;

        // Insertion sort on smallest arrays
        if (length < INSERTIONSORT_THRESHOLD) {
            for (int i = low; i < high; i++) {
                for (int j = i; j > low && Integer.compare(dest[j - 1], dest[j]) > 0; j--) {
                    swap(dest, j, j - 1);
                }
            }
            return;
        }

        // Recursively sort halves of dest into src
        int destLow = low;
        int destHigh = high;
        low += off;
        high += off;
        int mid = (low + high) >>> 1;
        mergeSort(dest, src, low, mid, -off);
        mergeSort(dest, src, mid, high, -off);

        // If list is already sorted, just copy from src to dest. This is an
        // optimization that results in faster sorts for nearly ordered lists.
        if (Integer.compare(src[mid - 1], src[mid]) <= 0) {
            System.arraycopy(src, low, dest, destLow, length);
            return;
        }

        // Merge sorted halves (now in src) into dest
        for (int i = destLow, p = low, q = mid; i < destHigh; i++) {
            if (q >= high || p < mid && Integer.compare(src[p], src[q]) <= 0) {
                dest[i] = src[p];
                permutation[reversePermutation[p++]] = i;
            } else {
                dest[i] = src[q];
                permutation[reversePermutation[q++]] = i;
            }
        }

        for (int i = destLow; i < destHigh; ++i) {
            reversePermutation[permutation[i]] = i;
        }
    }

    private void swap(int[] x, int a, int b) {
        int t = x[a];
        x[a] = x[b];
        x[b] = t;
        permutation[reversePermutation[a]] = b;
        permutation[reversePermutation[b]] = a;
        int tp = reversePermutation[a];
        reversePermutation[a] = reversePermutation[b];
        reversePermutation[b] = tp;
    }

    private int[] initPermutation(int length) {
        permutation = new int[length];
        reversePermutation = new int[length];
        for (int i = 0; i < length; ++i) {
            permutation[i] = reversePermutation[i] = i;
        }
        return permutation;
    }
}
