package org.jabref.model.pdf;

import org.jspecify.annotations.NullMarked;

/// Position within the rendered page area in PDF points, counted from its top-left corner.
@NullMarked
public record AnnotationArea(float x, float y, float width, float height) {
}
