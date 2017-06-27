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
package org.jabref.gui.util;

import java.util.Collection;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
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

    /***************************************************************************
     *                                                                         *
     * Event Listeners                                                         *
     *                                                                         *
     **************************************************************************/


    private final ChangeListener<String> textChangeListener = new ChangeListener<String>() {
        @Override
        public void changed(ObservableValue<? extends String> obs, String oldText, String newText) {
            if (getCompletionTarget().isFocused()) {
                setUserInput(newText);
            }
        }
    };

    /***************************************************************************
     *                                                                         *
     * Private fields                                                          *
     *                                                                         *
     **************************************************************************/
    private final ChangeListener<Boolean> focusChangedListener = new ChangeListener<Boolean>() {
        @Override
        public void changed(ObservableValue<? extends Boolean> obs, Boolean oldFocused, Boolean newFocused) {
            if (newFocused == false)
                hidePopup();
        }
    };


    /***************************************************************************
     *                                                                         *
     * Constructors                                                            *
     *                                                                         *
     **************************************************************************/
    /**
     * String converter to be used to convert suggestions to strings.
     */
    private StringConverter<T> converter;

    /**
     * Creates a new auto-completion binding between the given textInputControl
     * and the given suggestion provider.
     *
     * @param textInputControl
     * @param suggestionProvider
     */
    public AutoCompletionTextInputBinding(final TextInputControl textInputControl,
                                          Callback<ISuggestionRequest, Collection<T>> suggestionProvider) {

        this(textInputControl, suggestionProvider, AutoCompletionTextInputBinding
                .<T>defaultStringConverter());
    }


    /***************************************************************************
     *                                                                         *
     * Public API                                                              *
     *                                                                         *
     **************************************************************************/

    /**
     * Creates a new auto-completion binding between the given textInputControl
     * and the given suggestion provider.
     *
     * @param textInputControl
     * @param suggestionProvider
     */
    public AutoCompletionTextInputBinding(final TextInputControl textInputControl,
                                          Callback<ISuggestionRequest, Collection<T>> suggestionProvider,
                                          final StringConverter<T> converter) {

        super(textInputControl, suggestionProvider, converter);
        this.converter = converter;

        getCompletionTarget().textProperty().addListener(textChangeListener);
        getCompletionTarget().focusedProperty().addListener(focusChangedListener);
    }

    /***************************************************************************
     *                                                                         *
     * Static properties and methods                                           *
     *                                                                         *
     **************************************************************************/

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

    /** {@inheritDoc} */
    @Override
    public TextInputControl getCompletionTarget() {
        return (TextInputControl) super.getCompletionTarget();
    }

    /** {@inheritDoc} */
    @Override
    public void dispose() {
        getCompletionTarget().textProperty().removeListener(textChangeListener);
        getCompletionTarget().focusedProperty().removeListener(focusChangedListener);
    }

    /** {@inheritDoc} */
    @Override
    protected void completeUserInput(T completion) {
        String newText = converter.toString(completion);
        getCompletionTarget().setText(newText);
        getCompletionTarget().positionCaret(newText.length());
    }
}
