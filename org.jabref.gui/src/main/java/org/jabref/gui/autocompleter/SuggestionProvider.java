/**
 * Copyright (c) 2014, 2016 ControlsFX
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;

import javafx.util.Callback;

import org.controlsfx.control.textfield.AutoCompletionBinding.ISuggestionRequest;

/**
 * This is a simple implementation of a generic suggestion provider callback.
 * The complexity of suggestion generation is O(n) where n is the number of possible suggestions.
 *
 * @param <T> Type of suggestions
 *
 * This class is a copy of {@link impl.org.controlsfx.autocompletion.SuggestionProvider} with the only difference that
 *           we use a set instead of list to store the suggestions in order to eliminate duplicates.
 */
public abstract class SuggestionProvider<T> implements Callback<ISuggestionRequest, Collection<T>> {

    private final Collection<T> possibleSuggestions = new HashSet<>();
    private final Object possibleSuggestionsLock = new Object();

    /**
     * Create a default suggestion provider based on the toString() method of the generic objects
     * @param possibleSuggestions All possible suggestions
     * @return
     */
    public static <T> SuggestionProvider<T> create(Collection<T> possibleSuggestions) {
        return create(null, possibleSuggestions);
    }

    /**
     * Create a default suggestion provider based on the toString() method of the generic objects
     * using the provided stringConverter
     *
     * @param stringConverter A stringConverter which converts generic T into a string
     * @param possibleSuggestions All possible suggestions
     * @return
     */
    public static <T> SuggestionProvider<T> create(Callback<T, String> stringConverter, Collection<T> possibleSuggestions) {
        SuggestionProviderString<T> suggestionProvider = new SuggestionProviderString<>(stringConverter);
        suggestionProvider.addPossibleSuggestions(possibleSuggestions);
        return suggestionProvider;
    }

    /**
     * Add the given new possible suggestions to this  SuggestionProvider
     * @param newPossible
     */
    public void addPossibleSuggestions(@SuppressWarnings("unchecked") T... newPossible) {
        addPossibleSuggestions(Arrays.asList(newPossible));
    }

    /**
     * Add the given new possible suggestions to this  SuggestionProvider
     * @param newPossible
     */
    public void addPossibleSuggestions(Collection<T> newPossible) {
        synchronized (possibleSuggestionsLock) {
            possibleSuggestions.addAll(newPossible);
        }
    }

    /**
     * Remove all current possible suggestions
     */
    public void clearSuggestions() {
        synchronized (possibleSuggestionsLock) {
            possibleSuggestions.clear();
        }
    }

    @Override
    public final Collection<T> call(final ISuggestionRequest request) {
        List<T> suggestions = new ArrayList<>();
        if (!request.getUserText().isEmpty()) {
            synchronized (possibleSuggestionsLock) {
                for (T possibleSuggestion : possibleSuggestions) {
                    if (isMatch(possibleSuggestion, request)) {
                        suggestions.add(possibleSuggestion);
                    }
                }
            }
            Collections.sort(suggestions, getComparator());
        }
        return suggestions;
    }


    /***************************************************************************
     *                                                                         *
     * Static methods                                                          *
     *                                                                         *
     **************************************************************************/

    /**
     * Get the comparator to order the suggestions
     * @return
     */
    protected abstract Comparator<T> getComparator();

    /**
     * Check the given possible suggestion is a match (is a valid suggestion)
     * @param suggestion
     * @param request
     * @return
     */
    protected abstract boolean isMatch(T suggestion, ISuggestionRequest request);


    /***************************************************************************
     *                                                                         *
     * Default implementations                                                 *
     *                                                                         *
     **************************************************************************/

    /**
     * This is a simple string based suggestion provider.
     * All generic suggestions T are turned into strings for processing.
     *
     */
    private static class SuggestionProviderString<T> extends SuggestionProvider<T> {

        private Callback<T, String> stringConverter;

        private final Comparator<T> stringComparator = new Comparator<T>() {
            @Override
            public int compare(T o1, T o2) {
                String o1str = stringConverter.call(o1);
                String o2str = stringConverter.call(o2);
                return o1str.compareTo(o2str);
            }
        };

        /**
         * Create a new SuggestionProviderString
         * @param stringConverter
         */
        public SuggestionProviderString(Callback<T, String> stringConverter) {
            this.stringConverter = stringConverter;

            // In case no stringConverter was provided, use the default strategy
            if (this.stringConverter == null) {
                this.stringConverter = new Callback<T, String>() {
                    @Override
                    public String call(T obj) {
                        return obj != null ? obj.toString() : ""; //$NON-NLS-1$
                    }
                };
            }
        }

        /**{@inheritDoc}*/
        @Override
        protected Comparator<T> getComparator() {
            return stringComparator;
        }

        /**{@inheritDoc}*/
        @Override
        protected boolean isMatch(T suggestion, ISuggestionRequest request) {
            String userTextLower = request.getUserText().toLowerCase();
            String suggestionStr = suggestion.toString().toLowerCase();
            return suggestionStr.contains(userTextLower);
        }
    }
}
