/// Real Writer acceptance tests for the canonical JStyle, CSL, and BST engine pipeline.
/// [org.jabref.logic.openoffice.integration.HeadlessLibreOffice] owns the isolated UNO process;
/// [org.jabref.logic.openoffice.integration.WriterIntegrationTest] checks golden documents, family switching, and failure atomicity.
/// [org.jabref.logic.openoffice.integration.ZoteroWriterTest] checks the separate Zotero-compatible strategy against real Writer documents.
/// [org.jabref.logic.openoffice.integration.OOTextWriterTest] checks literal entity decoding separately from formatting tags.
///
/// @see <a href="https://devdocs.jabref.org/code-howtos/testing.html#libreoffice-writer-integration-tests">Writer integration tests</a>
@NullMarked
package org.jabref.logic.openoffice.integration;

import org.jspecify.annotations.NullMarked;
