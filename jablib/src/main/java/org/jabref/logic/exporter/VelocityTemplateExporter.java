package org.jabref.logic.exporter;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;

import org.jabref.logic.layout.format.HTMLChars;
import org.jabref.logic.util.FileType;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.metadata.SaveOrder;
import org.jabref.model.metadata.SelfContainedSaveOrder;

import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.jspecify.annotations.NonNull;

/// Export format based on an Apache Velocity template — the designated successor of the
/// `.layout`-based [TemplateExporter] (see ADR-0039 and [#12418](https://github.com/JabRef/jabref/issues/12418)).
///
/// In contrast to the `.layout` format (separate begin/entry/end files, one per entry type),
/// a Velocity export is a single template iterating over `$entries`, e.g.
/// `#foreach( $entry in $entries )$entry.type: $html.format($entry.getField("title"))
/// #end`.
///
/// Context contract (see [TemplateBibEntry] for the entry view):
/// - `$entries` — the sorted entries to export
/// - `$html` — [HTMLChars], converts LaTeX markup to HTML (`$html.format(...)`)
public class VelocityTemplateExporter extends Exporter {

    static final String TEMPLATE_PREFIX = "/resource/template/";

    private static final VelocityEngine VELOCITY_ENGINE = new VelocityEngine();

    static {
        VELOCITY_ENGINE.init();
    }

    private final String templateFileName;
    private final SelfContainedSaveOrder saveOrder;

    /// @param templateFileName name of the template resource below `/resource/template/`, e.g. `simplehtml.vm`
    public VelocityTemplateExporter(String displayName,
                                    String consoleName,
                                    String templateFileName,
                                    FileType extension,
                                    SelfContainedSaveOrder saveOrder) {
        super(consoleName, displayName, extension);
        this.templateFileName = templateFileName;
        this.saveOrder = saveOrder == null ? SaveOrder.getDefaultSaveOrder() : saveOrder;
    }

    @Override
    public void export(@NonNull BibDatabaseContext databaseContext,
                       Path file,
                       @NonNull List<BibEntry> entries) throws IOException {
        if (entries.isEmpty()) { // Do not export if no entries to export -- avoids exports with only template text
            return;
        }

        List<TemplateBibEntry> templateEntries = BibDatabaseWriter.getSortedEntries(entries, saveOrder)
                                                                  .stream()
                                                                  .map(entry -> new TemplateBibEntry(entry, databaseContext.getDatabase()))
                                                                  .toList();

        VelocityContext context = new VelocityContext();
        context.put("entries", templateEntries);
        context.put("html", new HTMLChars());

        InputStream inputStream = VelocityTemplateExporter.class.getResourceAsStream(TEMPLATE_PREFIX + templateFileName);
        if (inputStream == null) {
            throw new IOException("Cannot find template file: '" + TEMPLATE_PREFIX + templateFileName + "'.");
        }
        try (AtomicFileWriter writer = new AtomicFileWriter(file, StandardCharsets.UTF_8);
             Reader template = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            VELOCITY_ENGINE.evaluate(context, writer, templateFileName, template);
        }
    }
}
