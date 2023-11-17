package org.jabref.gui.autocompleter;

import com.google.common.base.Equivalence;
import java.util.Comparator;
import java.util.stream.Stream;
import org.controlsfx.control.textfield.AutoCompletionBinding;

public class EmptySuggestionProvider extends SuggestionProvider<String> {

    @Override
    protected Equivalence<String> getEquivalence() {
        return Equivalence.equals().onResultOf(value -> value);
    }

    @Override
    protected Comparator<String> getComparator() {
        return Comparator.naturalOrder();
    }

    @Override
    protected boolean isMatch(
        String candidate,
        AutoCompletionBinding.ISuggestionRequest request
    ) {
        return false;
    }

    @Override
    public Stream<String> getSource() {
        return Stream.empty();
    }
}
