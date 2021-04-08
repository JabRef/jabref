package org.jabref.logic.openoffice;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.function.ToIntFunction;
import java.util.regex.Pattern;

import org.jabref.logic.layout.Layout;
import org.jabref.logic.layout.LayoutFormatter;
import org.jabref.logic.layout.LayoutFormatterPreferences;
import org.jabref.logic.layout.LayoutHelper;
import org.jabref.model.database.BibDatabase;
import org.jabref.model.entry.Author;
import org.jabref.model.entry.AuthorList;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.Field;
import org.jabref.model.entry.field.FieldFactory;
import org.jabref.model.entry.field.OrFields;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.types.EntryType;
import org.jabref.model.entry.types.EntryTypeFactory;
import org.jabref.model.strings.StringUtil;



import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CitationMarkerEntryImpl implements CitationMarkerEntry {
    final String citationKey;
    final BibEntry bibEntry;    // should allow null
    final BibDatabase database; // should allow null
    final String uniqueLetter;  // may be null
    final String pageInfo;  //  null for empty
    final boolean isFirstAppearanceOfSource;

    public CitationMarkerEntryImpl(String citationKey,
                                   BibEntry bibEntry,
                                   BibDatabase database,
                                   String uniqueLetter,
                                   String pageInfo,
                                   boolean isFirstAppearanceOfSource) {
        this.citationKey = citationKey;

        if (bibEntry == null && database != null) {
            throw new RuntimeException("CitationMarkerEntryImpl:"
                                       + " bibEntry == null, but database != null");
        }
        if (bibEntry != null && database == null) {
            throw new RuntimeException("CitationMarkerEntryImpl:"
                                       + " bibEntry != null, but database == null");
        }

        this.bibEntry = bibEntry;
        this.database = database;
        this.uniqueLetter = uniqueLetter;
        this.pageInfo = pageInfo;
        this.isFirstAppearanceOfSource = isFirstAppearanceOfSource;
    }

    @Override
    public String getCitationKey() {
        return citationKey;
    }

    @Override
    public BibEntry getBibEntryOrNull() {
        return bibEntry;
    }

    @Override
    public BibDatabase getDatabaseOrNull() {
        return database;
    }

    @Override
    public String getUniqueLetterOrNull() {
        return uniqueLetter;
    }

    @Override
    public String getPageInfoOrNull() {
        return pageInfo;
    }

    @Override
    public boolean getIsFirstAppearanceOfSource() {
        return isFirstAppearanceOfSource;
    }
}
