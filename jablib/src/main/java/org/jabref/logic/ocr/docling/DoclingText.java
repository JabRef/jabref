package org.jabref.logic.ocr.docling;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record DoclingText(String text, List<DoclingProv> prov) {
}
