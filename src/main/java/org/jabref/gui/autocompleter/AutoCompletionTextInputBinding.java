/**
 * Copyright (c) 2014, 2015, ControlsFX
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * * Redistributions of source code must retain the above copyright
 * notice, this list of conditions and the following disclaimer.
 * * Redistributions in binary form must reproduce the above copyright
 * notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 * * Neither the name of ControlsFX, any associated website, nor the
 * names of its contributors may be used to endorse or promote products
 * derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL CONTROLSFX BE LIABLE FOR ANY
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package org.jabref.gui.autocompleter;

import java.util.Collection;

import javafx.beans.value.ChangeListener;
import javafx.scene.control.TextInputControl;
import javafx.util.Callback;
import javafx.util.StringConverter;

import org.controlsfx.control.textfield.AutoCompletionBinding;

/**
 * Represents a binding between a text input control and a auto-completion popup
 * This class is a slightly modified version of {@link impl.org.controlsfx.autocompletion.AutoCompletionTextFieldBinding}
 * that works with general text input controls instead of just text fields.
 * @param <T>
 *
 */
public class AutoCompletionTextInputBinding<T> extends AutoCompletionBinding<T> {

    /**
     * String converter to be used to convert suggestions to strings.
     */
    private StringConverter<T> converter;
    private AutoCompletionStrategy inputAnalyzer;
    private final ChangeListener<String> textChangeListener = (obs, oldText, newText) -> {
        if (getCompletionTarget().isFocused()) {
            setUserInputText(newText);
        }
    };
    private boolean showOnFocus;
    private final ChangeListener<Boolean> focusChangedListener = (obs, oldFocused, newFocused) -> {
        if (newFocused) {
            if (showOnFocus) {
                setUserInputText(getCompletionTarget().getText());
            }
        } else {
            hidePopup();
        }
    };

    /**
     * Creates a new auto-completion binding between the given textInputControl
     * and the given suggestion provider.
     */
    private AutoCompletionTextInputBinding(final TextInputControl textInputControl,
                                           Callback<ISuggestionRequest, Collection<T>> suggestionProvider) {

        this(textInputControl,
                suggestionProvider,
                AutoCompletionTextInputBinding.defaultStringConverter(),
                new ReplaceStrategy());
    }

    private AutoCompletionTextInputBinding(final TextInputControl textInputControl,
                                           final Callback<ISuggestionRequest, Collection<T>> suggestionProvider,
                                           final StringConverter<T> converter) {
        this(textInputControl, suggestionProvider, converter, new ReplaceStrategy());
    }

    private AutoCompletionTextInputBinding(final TextInputControl textInputControl,
                                           final Callback<ISuggestionRequest, Collection<T>> suggestionProvider,
                                           final StringConverter<T> converter,
                                           final AutoCompletionStrategy inputAnalyzer) {

        super(textInputControl, suggestionProvider, converter);
        this.converter = converter;
        this.inputAnalyzer = inputAnalyzer;

        getCompletionTarget().textProperty().addListener(textChangeListener);
        getCompletionTarget().focusedProperty().addListener(focusChangedListener);
    }

    private static <T> StringConverter<T> defaultStringConverter() {
        return new StringConverter<T>() {
            @Override
            public String toString(T t) {
                return t == null ? null : t.toString();
            }

            @SuppressWarnings("unchecked")
            @Override
            public T fromString(String string) {
                return (T) string;
            }
        };
    }

    public static <T> void autoComplete(TextInputControl textArea, Callback<ISuggestionRequest, Collection<T>> suggestionProvider) {
        new AutoCompletionTextInputBinding<>(textArea, suggestionProvider);
    }

    public static <T> void autoComplete(TextInputControl textArea, Callback<ISuggestionRequest, Collection<T>> suggestionProvider, StringConverter<T> converter) {
        new AutoCompletionTextInputBinding<>(textArea, suggestionProvider, converter);
    }

    public static <T> AutoCompletionTextInputBinding<T> autoComplete(TextInputControl textArea, Callback<ISuggestionRequest, Collection<T>> suggestionProvider, StringConverter<T> converter, AutoCompletionStrategy inputAnalyzer) {
        return new AutoCompletionTextInputBinding<>(textArea, suggestionProvider, converter, inputAnalyzer);
    }

    public static <T> AutoCompletionTextInputBinding<T> autoComplete(TextInputControl textArea, Callback<ISuggestionRequest, Collection<T>> suggestionProvider, AutoCompletionStrategy inputAnalyzer) {
        return autoComplete(textArea, suggestionProvider, AutoCompletionTextInputBinding.defaultStringConverter(), inputAnalyzer);
    }

    private void setUserInputText(String newText) {
        if (newText == null) {
            newText = "";
        }
        AutoCompletionInput input = inputAnalyzer.analyze(newText);
        setUserInput(input.getUnfinishedPart());
    }

    @Override
    public TextInputControl getCompletionTarget() {
        return (TextInputControl) super.getCompletionTarget();
    }

    @Override
    public void dispose() {
        getCompletionTarget().textProperty().removeListener(textChangeListener);
        getCompletionTarget().focusedProperty().removeListener(focusChangedListener);
    }

    @Override
    protected void completeUserInput(T completion) {
        String completionText = converter.toString(completion);
        String inputText = getCompletionTarget().getText();
        if (inputText == null) {
            inputText = "";
        }
        AutoCompletionInput input = inputAnalyzer.analyze(inputText);
        String newText = input.getPrefix() + completionText;
        getCompletionTarget().setText(newText);
        getCompletionTarget().positionCaret(newText.length());
    }

    public void setShowOnFocus(boolean showOnFocus) {
        this.showOnFocus = showOnFocus;
    }
}
