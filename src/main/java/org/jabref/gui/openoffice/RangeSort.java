package org.jabref.gui.openoffice;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Collections;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.sun.star.awt.Point;
import com.sun.star.awt.Selection;
import com.sun.star.beans.UnknownPropertyException;
import com.sun.star.container.NoSuchElementException;
import com.sun.star.container.XNameAccess;
import com.sun.star.lang.WrappedTargetException;
import com.sun.star.lang.XServiceInfo;
import com.sun.star.text.XText;
import com.sun.star.text.XTextCursor;
import com.sun.star.text.XTextContent;
import com.sun.star.text.XTextRange;
import com.sun.star.text.XTextRangeCompare;
import com.sun.star.text.XTextViewCursor;
import com.sun.star.text.XTextViewCursorSupplier;
import com.sun.star.util.InvalidStateException;

// import org.jabref.logic.JabRefException;
// import org.jabref.model.database.BibDatabase;
// import org.jabref.model.entry.BibEntry;
// import org.slf4j.Logger;
// import org.slf4j.LoggerFactory;

class RangeSort {

    /**
     * This is what {@code visualSort} needs in its input.
     *
     * But actually there is nothing visual in it.
     * Maybe we could reuse it for other sorters.
     *
     */
    public interface RangeSortable<T> {
        public XTextRange getRange();
        public int getIndexInPosition();
        public T getContent();
    }

    /**
     * A simple implementation of {@code RangeSortable}
     */
    public static class RangeSortEntry<T> implements RangeSortable<T> {
        public XTextRange range;
        public int indexInPosition;
        public T content;

        RangeSortEntry(
            XTextRange range,
            int indexInPosition,
            T content
            ) {
            this.range = range;
            this.indexInPosition = indexInPosition;
            this.content = content;
        }

        @Override
        public XTextRange getRange() {
            return range;
        }

        @Override
        public int getIndexInPosition() {
            return indexInPosition;
        }

        @Override
        public T getContent() {
            return content;
        }
    }

}
