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

import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.google.common.base.Equivalence;
import org.controlsfx.control.textfield.AutoCompletionBinding.ISuggestionRequest;

/**
 * This is a simple implementation of a generic suggestion provider callback.
 *
 * @param <T> Type of suggestions
 */
public abstract class SuggestionProvider<T> {

    public final Collection<T> provideSuggestions(ISuggestionRequest request) {
        if (!request.getUserText().isEmpty()) {
            Comparator<T> comparator = getComparator();
            Equivalence<T> equivalence = getEquivalence();
            return getSource().filter(candidate -> isMatch(candidate, request))
                              .map(equivalence::wrap) // Need to do a bit of acrobatic as there is no distinctBy method
                              .distinct()
                              .limit(10)
                              .map(Equivalence.Wrapper::get)
                              .sorted(comparator)
                              .collect(Collectors.toList());
        } else {
            return Collections.emptyList();
        }
    }

    protected abstract Equivalence<T> getEquivalence();

    public Collection<T> getPossibleSuggestions() {
        Comparator<T> comparator = getComparator().reversed();
        Equivalence<T> equivalence = getEquivalence();
        return getSource().map(equivalence::wrap) // Need to do a bit of acrobatic as there is no distinctBy method
                          .distinct()
                          .map(Equivalence.Wrapper::get)
                          .sorted(comparator)
                          .collect(Collectors.toList());
    }

    /**
     * Get the comparator to order the suggestions
     */
    protected abstract Comparator<T> getComparator();

    /**
     * Check the given candidate is a match (ie a valid suggestion)
     */
    protected abstract boolean isMatch(T candidate, ISuggestionRequest request);

    public abstract Stream<T> getSource();
}
