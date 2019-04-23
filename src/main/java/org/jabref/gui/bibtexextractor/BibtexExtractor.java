package org.jabref.gui.bibtexextractor;

import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.BiblatexEntryTypes;
import org.jabref.model.entry.FieldName;

public class BibtexExtractor {

    public BibEntry Extract(String input){
        BibEntry extractedEntity = new BibEntry(BiblatexEntryTypes.ARTICLE);
        extractedEntity.setField(FieldName.TITLE, "title");
        extractedEntity.setField(FieldName.ABSTRACT, "all the rest");
        extractedEntity.setField(FieldName.YEAR, "2020");
        return extractedEntity;
    }
}
