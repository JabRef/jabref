package org.jabref.logic.ocr.docling;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record DoclingBBox(double left, double top, double right, double bottom) {
}
