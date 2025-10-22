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

import org.jabref.gui.util.UiTaskExecutor;

import org.controlsfx.control.textfield.AutoCompletionBinding;

/**
 * Represents a binding between a text input control and an auto-completion popup.
 * This class is a slightly modified version of {@link impl.org.controlsfx.autocompletion.AutoCompletionTextFieldBinding}
 * that works with general text input controls instead of just text fields.
 * <p>
 * Use the {@link Builder} to create instances of this class.
 * </p>
 *
 * @param <T> the type of the suggestions
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
     * Private constructor to enforce the use of Builder.
     * Creates a new auto-completion binding between the given textInputControl
     * and the given suggestion provider.
     *
     * @param builder the builder containing all configuration parameters
     */
    private AutoCompletionTextInputBinding(Builder<T> builder) {
        super(builder.textInputControl, builder.suggestionProvider, builder.converter);
        this.converter = builder.converter;
        this.inputAnalyzer = builder.inputAnalyzer;
        this.showOnFocus = builder.showOnFocus;

        getCompletionTarget().textProperty().addListener(textChangeListener);
        getCompletionTarget().focusedProperty().addListener(focusChangedListener);
    }

    /**
     * Returns the default string converter that uses toString() for conversion.
     *
     * @param <T> the type of the suggestions
     * @return a default string converter
     */
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
     * Creates a new Builder for constructing AutoCompletionTextInputBinding instances.
     *
     * @param <T>                the type of the suggestions
     * @param textInputControl   the text input control to bind auto-completion to
     * @param suggestionProvider the callback to provide suggestions based on user input
     * @return a new Builder instance
     */
    public static <T> Builder<T> builder(TextInputControl textInputControl,
                                         Callback<ISuggestionRequest, Collection<T>> suggestionProvider) {
        return new Builder<>(textInputControl, suggestionProvider);
    }

    /**
     * Convenience method for simple auto-completion setup.
     * Equivalent to: {@code builder(textArea, suggestionProvider).build()}
     *
     * @param <T>                the type of the suggestions
     * @param textArea           the text input control to bind auto-completion to
     * @param suggestionProvider the callback to provide suggestions based on user input
     * @return a new AutoCompletionTextInputBinding instance
     */
    public static <T> AutoCompletionTextInputBinding<T> autoComplete(TextInputControl textArea,
                                                                       Callback<ISuggestionRequest, Collection<T>> suggestionProvider) {
        return builder(textArea, suggestionProvider).build();
    }

    /**
     * Convenience method for auto-completion with custom converter.
     * Equivalent to: {@code builder(textArea, suggestionProvider).converter(converter).build()}
     *
     * @param <T>                the type of the suggestions
     * @param textArea           the text input control to bind auto-completion to
     * @param suggestionProvider the callback to provide suggestions based on user input
     * @param converter          the string converter for suggestion display
     * @return a new AutoCompletionTextInputBinding instance
     */
    public static <T> AutoCompletionTextInputBinding<T> autoComplete(TextInputControl textArea,
                                                                       Callback<ISuggestionRequest, Collection<T>> suggestionProvider,
                                                                       StringConverter<T> converter) {
        return builder(textArea, suggestionProvider).converter(converter).build();
    }

    /**
     * Convenience method for auto-completion with custom converter and input analyzer.
     * Equivalent to: {@code builder(textArea, suggestionProvider).converter(converter).inputAnalyzer(inputAnalyzer).build()}
     *
     * @param <T>                the type of the suggestions
     * @param textArea           the text input control to bind auto-completion to
     * @param suggestionProvider the callback to provide suggestions based on user input
     * @param converter          the string converter for suggestion display
     * @param inputAnalyzer      the strategy for analyzing user input
     * @return a new AutoCompletionTextInputBinding instance
     */
    public static <T> AutoCompletionTextInputBinding<T> autoComplete(TextInputControl textArea,
                                                                       Callback<ISuggestionRequest, Collection<T>> suggestionProvider,
                                                                       StringConverter<T> converter,
                                                                       AutoCompletionStrategy inputAnalyzer) {
        return builder(textArea, suggestionProvider)
                .converter(converter)
                .inputAnalyzer(inputAnalyzer)
                .build();
    }

    /**
     * Convenience method for auto-completion with custom input analyzer.
     * Equivalent to: {@code builder(textArea, suggestionProvider).inputAnalyzer(inputAnalyzer).build()}
     *
     * @param <T>                the type of the suggestions
     * @param textArea           the text input control to bind auto-completion to
     * @param suggestionProvider the callback to provide suggestions based on user input
     * @param inputAnalyzer      the strategy for analyzing user input
     * @return a new AutoCompletionTextInputBinding instance
     */
    public static <T> AutoCompletionTextInputBinding<T> autoComplete(TextInputControl textArea,
                                                                       Callback<ISuggestionRequest, Collection<T>> suggestionProvider,
                                                                       AutoCompletionStrategy inputAnalyzer) {
        return builder(textArea, suggestionProvider)
                .inputAnalyzer(inputAnalyzer)
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
    }

    /**
     * Sets whether the auto-completion popup should be shown when the control gains focus.
     *
     * @param showOnFocus true to show popup on focus, false otherwise
     */
    public void setShowOnFocus(boolean showOnFocus) {
        this.showOnFocus = showOnFocus;
    }

    /**
     * Builder for creating {@link AutoCompletionTextInputBinding} instances.
     * This builder allows flexible configuration of auto-completion behavior with a fluent API.
     *
     * <p>Example usage:</p>
     * <pre>{@code
     * AutoCompletionTextInputBinding<String> binding =
     *     AutoCompletionTextInputBinding.<String>builder(textField, suggestionProvider)
     *         .converter(customConverter)
     *         .inputAnalyzer(new CustomStrategy())
     *         .showOnFocus(true)
     *         .build();
     * }</pre>
     *
     * @param <T> the type of the suggestions
     */
    public static class Builder<T> {
        // Required parameters
        private final TextInputControl textInputControl;
        private final Callback<ISuggestionRequest, Collection<T>> suggestionProvider;

        // Optional parameters with default values
        private StringConverter<T> converter = defaultStringConverter();
        private AutoCompletionStrategy inputAnalyzer = new ReplaceStrategy();
        private boolean showOnFocus = false;

        /**
         * Constructs a new Builder with required parameters.
         *
         * @param textInputControl   the text input control to bind auto-completion to (must not be null)
         * @param suggestionProvider the callback to provide suggestions based on user input (must not be null)
         */
        public Builder(TextInputControl textInputControl,
                       Callback<ISuggestionRequest, Collection<T>> suggestionProvider) {
            if (textInputControl == null) {
                throw new IllegalArgumentException("textInputControl cannot be null");
            }
            if (suggestionProvider == null) {
                throw new IllegalArgumentException("suggestionProvider cannot be null");
            }
            this.textInputControl = textInputControl;
            this.suggestionProvider = suggestionProvider;
        }

        /**
         * Sets the string converter for displaying suggestions.
         *
         * @param converter the string converter (must not be null)
         * @return this Builder instance for method chaining
         */
        public Builder<T> converter(StringConverter<T> converter) {
            if (converter == null) {
                throw new IllegalArgumentException("converter cannot be null");
            }
            this.converter = converter;
            return this;
        }

        /**
         * Sets the input analyzer strategy for processing user input.
         *
         * @param inputAnalyzer the auto-completion strategy (must not be null)
         * @return this Builder instance for method chaining
         */
        public Builder<T> inputAnalyzer(AutoCompletionStrategy inputAnalyzer) {
            if (inputAnalyzer == null) {
                throw new IllegalArgumentException("inputAnalyzer cannot be null");
            }
            this.inputAnalyzer = inputAnalyzer;
            return this;
        }

        /**
         * Sets whether the auto-completion popup should be shown when the control gains focus.
         *
         * @param showOnFocus true to show popup on focus, false otherwise
         * @return this Builder instance for method chaining
         */
        public Builder<T> showOnFocus(boolean showOnFocus) {
            this.showOnFocus = showOnFocus;
            return this;
        }

        /**
         * Builds and returns a new {@link AutoCompletionTextInputBinding} instance
         * with the configured parameters.
         *
         * @return a new AutoCompletionTextInputBinding instance
         */
        public AutoCompletionTextInputBinding<T> build() {
            return new AutoCompletionTextInputBinding<>(this);
        }
    }
}
