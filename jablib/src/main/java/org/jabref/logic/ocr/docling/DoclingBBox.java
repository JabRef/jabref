package org.jabref.logic.ocr.docling;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record DoclingBBox(double l, double t, double r, double b) {
}
