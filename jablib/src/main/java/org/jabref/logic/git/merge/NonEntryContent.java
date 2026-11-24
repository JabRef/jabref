package org.jabref.logic.git.merge;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.jabref.logic.citationkeypattern.GlobalCitationKeyPatterns;
import org.jabref.logic.exporter.MetaDataSerializer;
import org.jabref.logic.importer.ParserResult;
import org.jabref.model.database.BibDatabase;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.BibEntryType;
import org.jabref.model.entry.BibtexString;
import org.jabref.model.metadata.MetaData;

import org.jspecify.annotations.NullMarked;

/// Everything written to a `.bib` file that the merge takes from `current` as it is, because
/// [org.jabref.logic.git.merge.planning.SemanticMergeAnalyzer] plans entries with a citation key
/// only. Two versions with equal non-entry content can be merged without losing any of it.
///
/// Entries and strings compare by what the writer emits for them. Metadata is compared in its
/// serialized form, because [MetaData#equals] ignores items JabRef does not know. The shared
/// database ID and the encoding are written as file header, not as part of the metadata.
@NullMarked
record NonEntryContent(
        List<BibEntry> keylessEntries,
        Map<String, BibtexString> strings,
        String preamble,
        String epilog,
        Set<BibEntryType> customEntryTypes,
        String sharedDatabaseId,
        Charset encoding,
        boolean encodingExplicitlySupplied,
        Map<String, String> serializedMetaData) {

    static NonEntryContent of(ParserResult version, GlobalCitationKeyPatterns keyPatterns) {
        BibDatabase database = version.getDatabase();
        MetaData metaData = version.getMetaData();
        return new NonEntryContent(
                database.getEntries().stream().filter(entry -> entry.getCitationKey().isEmpty()).toList(),
                database.getStringValues().stream().collect(Collectors.toMap(BibtexString::getName, string -> string)),
                database.getPreamble().orElse(""),
                database.getEpilog(),
                version.getEntryTypes(),
                database.getSharedDatabaseID().orElse(""),
                metaData.getEncoding().orElse(StandardCharsets.UTF_8),
                metaData.getEncodingExplicitlySupplied(),
                MetaDataSerializer.getSerializedStringMap(metaData, keyPatterns));
    }
}
