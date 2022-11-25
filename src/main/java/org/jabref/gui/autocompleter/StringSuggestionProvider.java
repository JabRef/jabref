package org.jabref.gui.autocompleter;

import java.util.Comparator;
import java.util.stream.Stream;

import org.jabref.model.strings.StringUtil;

import com.google.common.base.Equivalence;
import org.controlsfx.control.textfield.AutoCompletionBinding;

abstract class StringSuggestionProvider extends SuggestionProvider<String> {

    @Override
    protected Equivalence<String> getEquivalence() {
        return Equivalence.equals().onResultOf(value -> value);
    }

    @Override
    protected Comparator<String> getComparator() {
        return Comparator.naturalOrder();
    }

    @Override
    protected boolean isMatch(String candidate, AutoCompletionBinding.ISuggestionRequest request) {
        return StringUtil.containsIgnoreCase(candidate, request.getUserText());
    }

    @Override
    public abstract Stream<String> getSource();
}
