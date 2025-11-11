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
import java.util.Objects;

import javafx.beans.value.ChangeListener;
import javafx.scene.control.TextInputControl;
import javafx.util.Callback;
import javafx.util.StringConverter;

import org.jabref.gui.util.UiTaskExecutor;

import org.controlsfx.control.textfield.AutoCompletionBinding;

/**
 * Represents a binding between a text input control and an auto-completion popup.
 *
 * <p>This class is a slightly modified version of
 * {@link impl.org.controlsfx.autocompletion.AutoCompletionTextFieldBinding}
 * that works with general text input controls instead of just text fields.</p>
 *
 * <p>Use the {@link Builder} to create instances with customized behavior,
 * or use the convenience methods {@link #autoComplete} for common cases.</p>
 */
public class AutoCompletionTextInputBinding<T> extends AutoCompletionBinding<T> {

    /**
     * String converter to be used to convert suggestions to strings.
     */
    private StringConverter<T> converter;
    private AutoCompletionStrategy inputAnalyzer;
    private final ChangeListener<String> textChangeListener = (_, _, newText) -> {
        if (getCompletionTarget().isFocused()) {
            setUserInputText(newText);
        }
    };
    private boolean showOnFocus;
    private final ChangeListener<Boolean> focusChangedListener = (_, _, newFocused) -> {
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
        return new StringConverter<>() {
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

    /**
     * Creates an auto-completion binding with default settings.
     *
     * @param textArea the text input control
     * @param suggestionProvider the suggestion provider
     * @param <T> the type of suggestions
     */
    public static <T> void autoComplete(TextInputControl textArea, Callback<ISuggestionRequest, Collection<T>> suggestionProvider) {
        new Builder<>(textArea, suggestionProvider).build();
    }

    /**
     * Creates an auto-completion binding with a custom converter.
     *
     * @param textArea the text input control
     * @param suggestionProvider the suggestion provider
     * @param converter the string converter for suggestions
     * @param <T> the type of suggestions
     */
    public static <T> void autoComplete(TextInputControl textArea, Callback<ISuggestionRequest, Collection<T>> suggestionProvider, StringConverter<T> converter) {
        new Builder<>(textArea, suggestionProvider)
                .withConverter(converter)
                .build();
    }

    /**
     * Creates an auto-completion binding with custom converter and input analyzer.
     *
     * @param textArea the text input control
     * @param suggestionProvider the suggestion provider
     * @param converter the string converter for suggestions
     * @param inputAnalyzer the input analyzer strategy
     * @param <T> the type of suggestions
     * @return the created binding
     */
    public static <T> AutoCompletionTextInputBinding<T> autoComplete(TextInputControl textArea, Callback<ISuggestionRequest, Collection<T>> suggestionProvider, StringConverter<T> converter, AutoCompletionStrategy inputAnalyzer) {
        return new Builder<>(textArea, suggestionProvider)
                .withConverter(converter)
                .withInputAnalyzer(inputAnalyzer)
                .build();
    }

    /**
     * Creates an auto-completion binding with a custom input analyzer.
     *
     * @param textArea the text input control
     * @param suggestionProvider the suggestion provider
     * @param inputAnalyzer the input analyzer strategy
     * @param <T> the type of suggestions
     * @return the created binding
     */
    public static <T> AutoCompletionTextInputBinding<T> autoComplete(TextInputControl textArea, Callback<ISuggestionRequest, Collection<T>> suggestionProvider, AutoCompletionStrategy inputAnalyzer) {
        return new Builder<>(textArea, suggestionProvider)
                .withInputAnalyzer(inputAnalyzer)
                .build();
    }

    private void setUserInputText(String newText) {
        if (newText == null) {
            newText = "";
        }
        AutoCompletionInput input = inputAnalyzer.analyze(newText);
        UiTaskExecutor.runInJavaFXThread(() -> setUserInput(input.getUnfinishedPart()));
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

    /**
     * Builder for creating AutoCompletionTextInputBinding instances.
     *
     * @param <T> the type of suggestions
     */
    public static class Builder<T> {
        private final TextInputControl textInputControl;
        private final Callback<ISuggestionRequest, Collection<T>> suggestionProvider;
        private StringConverter<T> converter = AutoCompletionTextInputBinding.defaultStringConverter();
        private AutoCompletionStrategy inputAnalyzer = new ReplaceStrategy();
        private boolean showOnFocus = false;

        /**
         * Creates a builder with required parameters.
         *
         * @param textInputControl the text input control to bind
         * @param suggestionProvider the suggestion provider
         */
        public Builder(TextInputControl textInputControl,
                       Callback<ISuggestionRequest, Collection<T>> suggestionProvider) {
            this.textInputControl = Objects.requireNonNull(textInputControl);
            this.suggestionProvider = Objects.requireNonNull(suggestionProvider);
        }

        /**
         * Sets the string converter for suggestions.
         *
         * @param converter the converter to use
         * @return this builder
         */
        public Builder<T> withConverter(StringConverter<T> converter) {
            this.converter = Objects.requireNonNull(converter);
            return this;
        }

        /**
         * Sets the input analyzer strategy.
         *
         * @param inputAnalyzer the input analyzer to use
         * @return this builder
         */
        public Builder<T> withInputAnalyzer(AutoCompletionStrategy inputAnalyzer) {
            this.inputAnalyzer = Objects.requireNonNull(inputAnalyzer);
            return this;
        }

        /**
         * Sets whether to show suggestions on focus.
         *
         * @param showOnFocus true to show on focus
         * @return this builder
         */
        public Builder<T> withShowOnFocus(boolean showOnFocus) {
            this.showOnFocus = showOnFocus;
            return this;
        }

        /**
         * Builds the AutoCompletionTextInputBinding.
         *
         * @return the created binding
         */
        public AutoCompletionTextInputBinding<T> build() {
            AutoCompletionTextInputBinding<T> binding =
                new AutoCompletionTextInputBinding<>(
                    textInputControl,
                    suggestionProvider,
                    converter,
                    inputAnalyzer
                );
            binding.setShowOnFocus(showOnFocus);
            return binding;
        }
    }
}
