package org.jabref.model.ocr.docling;

import java.util.List;

public record DoclingText(String text, List<DoclingProv> prov) {
}
