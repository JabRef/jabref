package org.jabref.model.ocr.docling;

import com.fasterxml.jackson.annotation.JsonProperty;

public record DoclingProv(@JsonProperty("page_no") int pageNo, DoclingBBox bbox) {
}
