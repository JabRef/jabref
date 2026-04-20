package org.jabref.http.dto;

import java.util.List;

import org.jspecify.annotations.NullMarked;

@NullMarked
public record LibraryQueryResponse(List<LibraryQueryMatch> matches) {
}
