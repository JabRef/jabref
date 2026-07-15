package org.jabref.logic.ocr.Docling;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record DoclingProv(@JsonProperty("page_no") int pageNo, DoclingBBox bbox) {
}
