package org.jabref.http.dto;

import java.util.List;

import org.jspecify.annotations.NullMarked;

@NullMarked
public record LibraryQueryRequest(List<String> dois, List<String> urls) {
    public LibraryQueryRequest {
        dois = dois != null ? dois : List.of();
        urls = urls != null ? urls : List.of();
    }
}
