package org.jabref.logic.openoffice.engine;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.jabref.logic.journals.JournalAbbreviationRepository;
import org.jabref.logic.layout.LayoutFormatterPreferences;
import org.jabref.logic.openoffice.style.BstCitationFormat;
import org.jabref.logic.openoffice.style.BstStyle;
import org.jabref.logic.openoffice.style.JStyle;
import org.jabref.logic.openoffice.style.JStyleLoader;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.BibEntryTypesManager;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.openoffice.style.CitationGroup;
import org.jabref.model.openoffice.style.CitationType;

import org.jspecify.annotations.NullMarked;
import org.mockito.Answers;

import static org.jabref.logic.openoffice.engine.RendererTestData.entry;
import static org.jabref.logic.openoffice.engine.RendererTestData.group;
import static org.jabref.logic.openoffice.engine.RendererTestData.numericStyle;
import static org.jabref.logic.openoffice.engine.RendererTestData.request;
import static org.mockito.Mockito.mock;

/// Standalone measurement, not a timing assertion in the unit suite. Supply a real pandoc executable
/// as the first argument. Measures complete pure renders; Writer visual ordering is intentionally absent.
@NullMarked
public final class RendererBenchmark {

    private RendererBenchmark() {
    }

    public static void main(String[] args) throws IOException, RenderException {
        if (args.length != 1) {
            System.err.println("Supply the path to a real pandoc executable");
            return;
        }
        List<CitationGroup> groups = new ArrayList<>();
        List<BibEntry> entries = new ArrayList<>();
        for (int i = 0; i < 300; i++) {
            String key = "key" + i;
            groups.add(group("group" + i, CitationType.AUTHORYEAR_PAR, key));
            entries.add(entry(key, "Title %03d".formatted(299 - i)).withField(StandardField.AUTHOR, "Author%d, Jane".formatted(i)));
        }
        RenderRequest request = request(groups, entries);
        measure("CSL sorted numeric", new CslRenderer(numericStyle(true, true), new BibEntryTypesManager()), request);
        measure("BST abbrv + real pandoc", new BstRenderer(BstStyle.createInternal(BstStyle.INTERNAL_ABBRV_PATH), args[0]), request);
        measure("JStyle numeric", new JStyleRenderer(new JStyle(JStyleLoader.DEFAULT_NUMERICAL_STYLE_PATH,
                mock(LayoutFormatterPreferences.class, Answers.RETURNS_DEEP_STUBS), mock(JournalAbbreviationRepository.class))), request);
        measure("JStyle author-year", new JStyleRenderer(new JStyle(JStyleLoader.DEFAULT_AUTHORYEAR_STYLE_PATH,
                mock(LayoutFormatterPreferences.class, Answers.RETURNS_DEEP_STUBS), mock(JournalAbbreviationRepository.class))), request);
    }

    private static void measure(String name, CitationRenderer renderer, RenderRequest request) throws RenderException {
        for (boolean bibliography : List.of(false, true)) {
            RenderOptions options = new RenderOptions(BstCitationFormat.NUMERIC, bibliography, "References", "Heading 1", "Text body", false);
            RenderRequest measured = new RenderRequest(request.groupsInDocumentOrder(), request.databases(), options);
            long[] milliseconds = new long[4];
            for (int iteration = 0; iteration < milliseconds.length; iteration++) {
                long start = System.nanoTime();
                RenderResult result = renderer.render(measured);
                milliseconds[iteration] = (System.nanoTime() - start) / 1_000_000;
                if (result.markers().size() != 300 || !result.unresolvedKeys().isEmpty()) {
                    throw new RenderException("Benchmark did not render all 300 resolved citation groups");
                }
            }
            long[] warm = Arrays.copyOfRange(milliseconds, 1, milliseconds.length);
            Arrays.sort(warm);
            System.out.printf("%s, bibliography=%s: first=%d ms, warm median=%d ms, samples=%s%n", name, bibliography,
                    milliseconds[0], warm[1], Arrays.toString(milliseconds));
        }
    }
}
