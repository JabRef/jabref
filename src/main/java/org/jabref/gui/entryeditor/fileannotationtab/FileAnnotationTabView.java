package org.jabref.gui.entryeditor.fileannotationtab;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import org.jabref.gui.AbstractView;
import org.jabref.logic.pdf.FileAnnotationCache;
import org.jabref.model.entry.BibEntry;

public class FileAnnotationTabView extends AbstractView {

    public FileAnnotationTabView(BibEntry entry, FileAnnotationCache fileAnnotationCache) {
        super(createContext(entry, fileAnnotationCache));
    }

    private static Function<String, Object> createContext(BibEntry entry, FileAnnotationCache fileAnnotationCache) {
        Map<String, Object> context = new HashMap<>();
        context.put("entry", entry);
        context.put("fileAnnotationCache", fileAnnotationCache);
        return context::get;
    }
}
