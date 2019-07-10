package org.jabref.model.entry;

/**
 * Handling of bibtex fields.
 * All bibtex-field related stuff should be placed here!
 * Because we can export this information into additional
 * config files -> simple extension and definition of new fields
 *
 * TODO:
 * - handling of identically fields with different names (https://github.com/JabRef/jabref/issues/521)
 * e.g. LCCN = lib-congress, journaltitle = journal
 * - group id for each fields, e.g. standard, jurabib, bio, ...
 * - add a additional properties functionality into the BibField class
 */
public class InternalBibtexFields {

    private InternalBibtexFields() {
        // Singleton
    }
}
