package org.jabref.model.pdf;

public record PdfSection(
        String name,
        String content,
        int startPage,
        int endPage
) {
    public PdfSection {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Section name cannot be null or blank");
        }
        if (content == null) {
            throw new IllegalArgumentException("Section content cannot be null");
        }
        if (startPage < 1) {
            throw new IllegalArgumentException("Start page must be >= 1");
        }
        if (endPage < startPage) {
            throw new IllegalArgumentException("End page must be >= start page");
        }
    }
}
