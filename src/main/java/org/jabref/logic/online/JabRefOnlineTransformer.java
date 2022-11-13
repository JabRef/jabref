package org.jabref.logic.online;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

import org.jabref.jabrefonline.UserChangesQuery.Author;
import org.jabref.jabrefonline.UserChangesQuery.Node;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;

public class JabRefOnlineTransformer {

    /**
     * Transforms a JabRef Online document to a BibEntry.
     */
    public BibEntry toBibEntry(Node document) {
        BibEntry entry = new BibEntry();

        // TODO: Add generation and hash
        entry.setRevision(new LocalRevision(document.id, null, null));

        if (!document.citationKeys.isEmpty()) {
            entry.setCitationKey(document.citationKeys.get(0));
            if (document.citationKeys.size() > 1) {
                entry.setField(StandardField.IDS, String.join(", ", document.citationKeys.subList(1, document.citationKeys.size())));
            }
        }
        entry.setField(StandardField.MODIFICATIONDATE, document.lastModified.format(DateTimeFormatter.ISO_ZONED_DATE_TIME));
        entry.setField(StandardField.CREATIONDATE, document.added.format(DateTimeFormatter.ISO_ZONED_DATE_TIME));
        entry.setField(StandardField.TITLE, document.title);
        entry.setField(StandardField.SUBTITLE, document.subtitle);
        entry.setField(StandardField.TITLEADDON, document.titleAddon);
        entry.setField(StandardField.ABSTRACT, document.abstract_);
        entry.setField(StandardField.AUTHOR, toBibAuthor(document.authors));
        entry.setField(StandardField.NOTE, document.note);
        entry.setField(StandardField.LANGUAGE, String.join(", ", document.languages));
        entry.setField(StandardField.PUBSTATE, document.publicationState);
        entry.setField(StandardField.DOI, document.doi);
        entry.setField(StandardField.KEYWORDS, String.join(", ", document.keywords));

        // TODO: Finish
        return entry;
    }

    private String toBibAuthor(List<Author> authors) {
        return authors.stream()
        .map(author -> author.allDetails.onPerson)
        .map(author -> author.family)
        .collect(Collectors.joining(" and "));
        // TODO: Handle onOrganization
    }

    public Node toDocument(BibEntry entry) {
        // TODO: Implement
        return null;
    }
}
