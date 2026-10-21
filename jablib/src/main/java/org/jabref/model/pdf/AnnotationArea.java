package org.jabref.model.pdf;

import org.jspecify.annotations.NullMarked;

/// Position on the page in PDF points, counted from the top-left corner.
@NullMarked
public record AnnotationArea(float x, float y, float width, float height) {
}
