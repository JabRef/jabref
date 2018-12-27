package org.jabref.gui.autocompleter;

import org.controlsfx.control.textfield.AutoCompletionBinding;

public class AutoCompleterUtil {
    public static AutoCompletionBinding.ISuggestionRequest getRequest(String text) {
        return new AutoCompletionBinding.ISuggestionRequest() {
            @Override
            public boolean isCancelled() {
                return false;
            }

            @Override
            public String getUserText() {
                return text;
            }
        };
    }
}
