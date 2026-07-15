package org.jabref.logic.ocr.Docling;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record DoclingDocument(List<DoclingText> texts) {
}
