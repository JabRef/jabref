package org.jabref.model.pdf;

import org.jspecify.annotations.NullMarked;

@NullMarked
public record AnnotationArea(float x, float y, float width, float height) {
}
