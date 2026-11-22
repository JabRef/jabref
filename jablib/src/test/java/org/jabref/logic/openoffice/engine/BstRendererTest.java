package org.jabref.logic.openoffice.engine;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.jabref.logic.l10n.Localization;
import org.jabref.logic.openoffice.bst.PandocLatexConverter;
import org.jabref.logic.openoffice.style.BstCitationFormat;
import org.jabref.logic.openoffice.style.BstStyle;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.openoffice.style.CitationType;

import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.parallel.ResourceLock;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;

import static org.jabref.logic.openoffice.engine.RendererTestData.SIMPLE_BST;
import static org.jabref.logic.openoffice.engine.RendererTestData.entry;
import static org.jabref.logic.openoffice.engine.RendererTestData.group;
import static org.jabref.logic.openoffice.engine.RendererTestData.markers;
import static org.jabref.logic.openoffice.engine.RendererTestData.request;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@NullMarked
@ResourceLock("Localization.lang")
class BstRendererTest {

    private static final String LABELED_BST = SIMPLE_BST.replace("\"\\bibitem{\" write$", "\"\\bibitem[\" write$ title write$ \"]{\" write$");

    @TempDir
    Path temporaryDirectory;

    @Test
    void numericNumbersRestartPerRenderAndReuseStableKeys() throws IOException, InterruptedException, RenderException {
        BstRenderer renderer = new BstRenderer(style(SIMPLE_BST), echoPandoc());
        RenderRequest request = request(List.of(group("b", CitationType.AUTHORYEAR_PAR, "b"), group("a", CitationType.AUTHORYEAR_PAR, "a"),
                group("repeat", CitationType.AUTHORYEAR_PAR, "b")), List.of(entry("a", "A"), entry("b", "B")));
        renderer.render(request(List.of(group("old", CitationType.AUTHORYEAR_PAR, "old")), List.of(entry("old", "Old"))));

        assertEquals(List.of("[1]", "[2]", "[1]"), markers(request, renderer.render(request)));
    }

    @Test
    void sortedNumbersAndBibliographyComeFromOneSetWideOrder() throws IOException, InterruptedException, RenderException {
        String sorted = SIMPLE_BST.replace("READ", "FUNCTION {sorter} { title 'sort.key$ := }\nREAD\nITERATE {sorter}\nSORT");
        RenderRequest request = request(List.of(group("z", CitationType.AUTHORYEAR_PAR, "z"), group("a", CitationType.AUTHORYEAR_PAR, "a")),
                List.of(entry("z", "Zebra"), entry("a", "Alpha")));
        RenderResult result = new BstRenderer(style(sorted), echoPandoc()).render(request);

        assertEquals(List.of(List.of("[2]", "[1]"), List.of("<p oo:ParaStyleName=\"Text body\"><span lang=\"zxx\">[1] Alpha</span></p>",
                        "<p oo:ParaStyleName=\"Text body\"><span lang=\"zxx\">[2] Zebra</span></p>")),
                List.of(markers(request, result), result.bibliography().stream().map(text -> text.toString()).toList()));
    }

    @Test
    void labelsAndBodiesKeepSetWideStateAndUseOnePandocCall() throws IOException, InterruptedException, RenderException {
        String source = """
                ENTRY { title } {} {}
                INTEGERS { number }
                FUNCTION {initialize} { #0 'number := }
                FUNCTION {article}
                { number #1 + 'number :=
                  "\\bibitem[Label" write$ number int.to.str$ write$ "]{" write$ cite$ write$ "}" write$ newline$
                  title write$ " set-" write$ number int.to.str$ write$ newline$
                }
                FUNCTION {misc} { article }
                FUNCTION {default.type} { article }
                READ
                EXECUTE {initialize}
                ITERATE {call.type$}
                """;
        PandocLatexConverter pandoc = echoPandoc();
        RenderRequest original = request(List.of(group("a", CitationType.AUTHORYEAR_PAR, "a"), group("b", CitationType.AUTHORYEAR_PAR, "b")),
                List.of(entry("a", "Alpha"), entry("b", "Beta")));
        RenderRequest request = withFormat(original, BstCitationFormat.STYLE_DEFINED);
        RenderResult result = new BstRenderer(style(source), pandoc).render(request);

        assertEquals(List.of(List.of("[Label1]", "[Label2]"), List.of("<p oo:ParaStyleName=\"Text body\"><span lang=\"zxx\">[Label1] Alpha set-1</span></p>",
                        "<p oo:ParaStyleName=\"Text body\"><span lang=\"zxx\">[Label2] Beta set-2</span></p>")),
                List.of(markers(request, result), result.bibliography().stream().map(text -> text.toString()).toList()));
        verify(pandoc, times(1)).latexToHtml(anyString());
    }

    @Test
    void missingStyleDefinedLabelIsCheckedBeforeConversion() throws IOException {
        PandocLatexConverter pandoc = mock(PandocLatexConverter.class);
        RenderRequest request = withFormat(request(List.of(group("a", CitationType.AUTHORYEAR_PAR, "a")), List.of(entry("a", "A"))), BstCitationFormat.STYLE_DEFINED);
        MissingCitationLabelException error = assertThrows(MissingCitationLabelException.class, () -> new BstRenderer(style(SIMPLE_BST), pandoc).render(request));

        assertEquals("a", error.citationKey());
        assertEquals(Optional.of(Localization.lang("The selected BST style does not define any citation format for style-defined citations.\nPlease choose either numeric or author-year.")), error.localizedMessage());
        verifyNoInteractions(pandoc);
    }

    @Test
    void unresolvedKeysHaveNoNumbersAndNoEntryIdFallback() throws IOException, InterruptedException, RenderException {
        BibEntry keyless = new BibEntry();
        RenderRequest request = request(List.of(group("g", CitationType.AUTHORYEAR_PAR, "a", "missing", "b", keyless.getId())),
                List.of(entry("a", "A"), keyless, entry("b", "B")));
        RenderResult result = new BstRenderer(style(SIMPLE_BST), echoPandoc()).render(request);

        assertEquals(List.of("[1, 2, Unresolved(missing), Unresolved(" + keyless.getId() + ")]"), markers(request, result));
        assertEquals(List.of("missing", keyless.getId()), result.unresolvedKeys());
    }

    @Test
    void preservesAuthorYearHelperAndApproximatesStoredUnsupportedType() throws IOException, InterruptedException, RenderException {
        RenderRequest request = withFormat(request(List.of(group("g", CitationType.AUTHOR_ONLY, "a")), List.of(entry("a", "A"))), BstCitationFormat.AUTHOR_YEAR);
        RenderResult result = new BstRenderer(style(SIMPLE_BST), echoPandoc()).render(request);

        assertEquals(List.of(List.of("(Smith, 2020)"), CitationType.AUTHOR_ONLY, Optional.empty()),
                List.of(markers(request, result), request.groupsInDocumentOrder().getFirst().citationType,
                        request.groupsInDocumentOrder().getFirst().citationsInStorageOrder.getFirst().getNumber()));
    }

    @Test
    void excludedBibliographyDoesNotInvokePandoc() throws IOException, RenderException {
        PandocLatexConverter pandoc = mock(PandocLatexConverter.class);
        RenderRequest original = request(List.of(group("g", CitationType.AUTHORYEAR_PAR, "a")), List.of(entry("a", "A")));
        RenderRequest request = new RenderRequest(original.groupsInDocumentOrder(), original.databases(),
                new RenderOptions(BstCitationFormat.NUMERIC, false, "", "", "", false));
        RenderResult result = new BstRenderer(style(SIMPLE_BST), pandoc).render(request);

        assertEquals(List.of(), result.bibliography());
        verifyNoInteractions(pandoc);
    }

    @Test
    void authorYearCitationOnlySkipsBstSourceButBibliographyStillRequiresIt() throws RenderException {
        BstStyle style = mock(BstStyle.class);
        PandocLatexConverter pandoc = mock(PandocLatexConverter.class);
        BstRenderer renderer = new BstRenderer(style, pandoc);
        RenderRequest original = request(List.of(group("g", CitationType.AUTHORYEAR_PAR, "a", "missing")), List.of(entry("a", "A")));
        RenderRequest citationOnly = new RenderRequest(original.groupsInDocumentOrder(), original.databases(),
                new RenderOptions(BstCitationFormat.AUTHOR_YEAR, false, "", "", "", false));

        RenderResult result = renderer.render(citationOnly);

        assertEquals(List.of("(Smith, 2020; Unresolved(missing))"), markers(citationOnly, result));
        assertEquals(List.of("missing"), result.unresolvedKeys());
        assertEquals(List.of(), result.bibliography());
        verifyNoInteractions(style, pandoc);

        IllegalArgumentException failure = new IllegalArgumentException("BST source unavailable");
        when(style.getSource()).thenThrow(failure);
        RenderException error = assertThrows(RenderException.class, () -> renderer.render(withFormat(original, BstCitationFormat.AUTHOR_YEAR)));

        assertSame(failure, error.getCause());
        verify(style).getSource();
        verifyNoInteractions(pandoc);
    }

    @Test
    void numericCitationOnlyStillUsesBstSortOrder() throws IOException, RenderException {
        String sorted = SIMPLE_BST.replace("READ", "FUNCTION {sorter} { title 'sort.key$ := }\nREAD\nITERATE {sorter}\nSORT");
        PandocLatexConverter pandoc = mock(PandocLatexConverter.class);
        RenderRequest original = request(List.of(group("z", CitationType.AUTHORYEAR_PAR, "z"), group("a", CitationType.AUTHORYEAR_PAR, "a")),
                List.of(entry("z", "Zebra"), entry("a", "Alpha")));
        RenderRequest citationOnly = new RenderRequest(original.groupsInDocumentOrder(), original.databases(),
                new RenderOptions(BstCitationFormat.NUMERIC, false, "", "", "", false));

        RenderResult result = new BstRenderer(style(sorted), pandoc).render(citationOnly);

        assertEquals(List.of("[2]", "[1]"), markers(citationOnly, result));
        assertEquals(List.of(), result.bibliography());
        verifyNoInteractions(pandoc);
    }

    @ParameterizedTest
    @EnumSource(value = BstCitationFormat.class, names = {"NUMERIC", "STYLE_DEFINED"})
    void citationOnlyStillRejectsOmittedBstItemsForOutputDependentFormats(BstCitationFormat format) throws IOException {
        PandocLatexConverter pandoc = mock(PandocLatexConverter.class);
        BstStyle style = style("ENTRY { title } {} {}\nREAD\n");
        RenderRequest original = request(List.of(group("g", CitationType.AUTHORYEAR_PAR, "a")), List.of(entry("a", "A")));
        RenderRequest citationOnly = new RenderRequest(original.groupsInDocumentOrder(), original.databases(),
                new RenderOptions(format, false, "", "", "", false));

        RenderException error = assertThrows(RenderException.class, () -> new BstRenderer(style, pandoc).render(citationOnly));

        assertEquals("The BST style omitted cited entry a", error.getMessage());
        verifyNoInteractions(pandoc);
    }

    @Test
    void pandocFailureIsCheckedAndLeavesInputsUntouched() throws IOException, InterruptedException {
        PandocLatexConverter pandoc = mock(PandocLatexConverter.class);
        IOException failure = new IOException("pandoc failed");
        when(pandoc.latexToHtml(anyString())).thenThrow(failure);
        BibEntry entry = entry("a", "A");
        RenderRequest request = request(List.of(group("g", CitationType.AUTHORYEAR_PAR, "a")), List.of(entry));
        PandocRenderException error = assertThrows(PandocRenderException.class, () -> new BstRenderer(style(SIMPLE_BST), pandoc).render(request));

        assertSame(failure, error.getCause());
        assertEquals(Optional.of(Localization.lang("Could not run Pandoc. Install Pandoc and set its path in Preferences > OpenOffice/LibreOffice.")), error.localizedMessage());
        assertEquals(Optional.empty(), request.groupsInDocumentOrder().getFirst().getCitationMarker());
        assertEquals(entry("a", "A").getFieldMap(), entry.getFieldMap());
    }

    @Test
    void missingPandocReportsTheConfiguredExecutableAndCorrectiveAction() {
        String path = temporaryDirectory.resolve("not-installed-pandoc.exe").toString();
        RenderRequest request = request(List.of(group("g", CitationType.AUTHORYEAR_PAR, "a")), List.of(entry("a", "A")));

        PandocRenderException error = assertThrows(PandocRenderException.class, () -> new BstRenderer(style(SIMPLE_BST), path).render(request));

        assertEquals(Optional.of(Localization.lang("Could not run Pandoc. Install Pandoc and set its path in Preferences > OpenOffice/LibreOffice.")
                + "\n" + Localization.lang("Pandoc path: %0", path)), error.localizedMessage());
    }

    @Test
    void malformedPandocOutputDoesNotExposeRawDiagnosticAsLocalizedGuidance() throws IOException, InterruptedException {
        PandocLatexConverter pandoc = mock(PandocLatexConverter.class);
        when(pandoc.latexToHtml(anyString())).thenReturn("unexpected output");
        RenderRequest request = request(List.of(group("g", CitationType.AUTHORYEAR_PAR, "a")), List.of(entry("a", "A")));

        RenderException error = assertThrows(RenderException.class, () -> new BstRenderer(style(SIMPLE_BST), pandoc).render(request));

        assertEquals(Optional.empty(), error.localizedMessage());
    }

    @Test
    void advertisesNormalAndNarrativeCitationSupport() throws RenderException {
        BstRenderer renderer = new BstRenderer(mock(BstStyle.class), mock(PandocLatexConverter.class));

        assertEquals(new StyleCapabilities(Set.of(CitationType.AUTHORYEAR_PAR, CitationType.AUTHORYEAR_INTEXT), true, false, false), renderer.capabilities());
        assertEquals(CitationType.AUTHORYEAR_INTEXT, renderer.capabilities().coerceForInsert(CitationType.AUTHORYEAR_INTEXT));
    }

    // [utest->req~openoffice.narrative-citations~1]
    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
            "NUMERIC | [1] | Smith [1]",
            "AUTHOR_YEAR | (Smith, 2020) | Smith (2020)",
            "STYLE_DEFINED | [Smi20] | Smith [Smi20]"
    })
    void rendersNormalAndNarrativeGroupsPerCitationType(BstCitationFormat format, String normal, String narrative) throws IOException, InterruptedException, RenderException {
        RenderRequest request = withFormat(request(List.of(group("normal", CitationType.AUTHORYEAR_PAR, "a"),
                group("narrative", CitationType.AUTHORYEAR_INTEXT, "a")), List.of(entry("a", "Smi20"))), format);

        RenderResult result = new BstRenderer(style(LABELED_BST), echoPandoc()).render(request);

        assertEquals(List.of(normal, narrative), markers(request, result));
    }

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
            "NUMERIC | Doe [2], Unresolved(missing), Smith [1], Doe [2], Roe [3]",
            "AUTHOR_YEAR | Doe (2021), Unresolved(missing), Smith (2020), Doe (2021), Roe (2022)",
            "STYLE_DEFINED | Doe [B21], Unresolved(missing), Smith [A20], Doe [B21], Roe [C22]"
    })
    void narrativePreservesStorageOrderAndPairsEveryAuthorWithItsMarker(BstCitationFormat format, String expected) throws IOException, InterruptedException, RenderException {
        String sorted = LABELED_BST.replace("READ", "FUNCTION {sorter} { title 'sort.key$ := }\nREAD\nITERATE {sorter}\nSORT");
        RenderRequest request = withFormat(request(List.of(group("g", CitationType.AUTHORYEAR_INTEXT, "b", "missing", "a", "b", "c")),
                List.of(entry("a", "A20"), entry("b", "B21").withField(StandardField.AUTHOR, "Doe, John").withField(StandardField.YEAR, "2021"),
                        entry("c", "C22").withField(StandardField.AUTHOR, "Roe, Richard").withField(StandardField.YEAR, "2022"))), format);

        RenderResult result = new BstRenderer(style(sorted), echoPandoc()).render(request);

        assertEquals(List.of(expected), markers(request, result));
        assertEquals(List.of("missing"), result.unresolvedKeys());
    }

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
            "NUMERIC | ? [1]",
            "AUTHOR_YEAR | ? (2020)",
            "STYLE_DEFINED | ? [Smi20]"
    })
    void narrativeMissingAuthorsUseExistingQuestionMarkFallback(BstCitationFormat format, String expected) throws IOException, InterruptedException, RenderException {
        BibEntry entry = new BibEntry().withCitationKey("a").withField(StandardField.TITLE, "Smi20").withField(StandardField.YEAR, "2020");
        RenderRequest request = withFormat(request(List.of(group("g", CitationType.AUTHORYEAR_INTEXT, "a")), List.of(entry)), format);

        RenderResult result = new BstRenderer(style(LABELED_BST), echoPandoc()).render(request);

        assertEquals(List.of(expected), markers(request, result));
    }

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
            "NUMERIC | A&amp;B [1]",
            "AUTHOR_YEAR | A&amp;B (2020)",
            "STYLE_DEFINED | A&amp;B [Smi&amp;20]"
    })
    void narrativeEscapesAuthorsAndStyleDefinedLabels(BstCitationFormat format, String expected) throws IOException, RenderException {
        PandocLatexConverter pandoc = mock(PandocLatexConverter.class);
        RenderRequest original = request(List.of(group("g", CitationType.AUTHORYEAR_INTEXT, "a")),
                List.of(entry("a", "Smi&20").withField(StandardField.AUTHOR, "A&B, Jane")));
        RenderRequest request = new RenderRequest(original.groupsInDocumentOrder(), original.databases(),
                new RenderOptions(format, false, "", "", "", false));

        RenderResult result = new BstRenderer(style(LABELED_BST), pandoc).render(request);

        assertEquals(List.of(expected), markers(request, result));
        verifyNoInteractions(pandoc);
    }

    @ParameterizedTest
    @EnumSource(BstCitationFormat.class)
    void unresolvedNarrativeKeysHaveNoAuthorPrefix(BstCitationFormat format) throws IOException, InterruptedException, RenderException {
        RenderRequest request = withFormat(request(List.of(group("g", CitationType.AUTHORYEAR_INTEXT, "missing&key")), List.of()), format);

        RenderResult result = new BstRenderer(style(LABELED_BST), echoPandoc()).render(request);

        assertEquals(List.of("Unresolved(missing&amp;key)"), markers(request, result));
        assertEquals(List.of("missing&key"), result.unresolvedKeys());
    }

    @ParameterizedTest
    @EnumSource(BstCitationFormat.class)
    void sameFormatRenderingPreservesStoredInTextType(BstCitationFormat format) throws IOException, InterruptedException, RenderException, JabRefMarkCodecException {
        String name = "JABREF_a IN_TEXT uid";
        JabRefMarkData stored = JabRefMarkCodec.decode(name);
        BstStyle style = style(LABELED_BST);
        BstRenderer renderer = new BstRenderer(style, echoPandoc());
        RenderRequest request = withFormat(request(List.of(group(stored.uniqueId(), stored.citationType(), stored.citationKeys().toArray(String[]::new))),
                List.of(entry("a", "Smi20"))), format);

        RenderResult first = renderer.render(request);
        RenderResult second = renderer.render(request);

        assertFalse(CitationFormatConverter.required(List.of(name), style, false));
        assertEquals(markers(request, first), markers(request, second));
        assertEquals(CitationType.AUTHORYEAR_INTEXT, request.groupsInDocumentOrder().getFirst().citationType);
        assertEquals(name, JabRefMarkCodec.encode(stored));
    }

    @Test
    void authorYearNarrativeWithoutBibliographySkipsBstAndPandoc() throws RenderException {
        BstStyle style = mock(BstStyle.class);
        PandocLatexConverter pandoc = mock(PandocLatexConverter.class);
        RenderRequest original = request(List.of(group("narrative", CitationType.AUTHORYEAR_INTEXT, "a", "missing"),
                group("normal", CitationType.AUTHORYEAR_PAR, "a")), List.of(entry("a", "A")));
        RenderRequest request = new RenderRequest(original.groupsInDocumentOrder(), original.databases(),
                new RenderOptions(BstCitationFormat.AUTHOR_YEAR, false, "", "", "", false));

        RenderResult result = new BstRenderer(style, pandoc).render(request);

        assertEquals(List.of("Smith (2020), Unresolved(missing)", "(Smith, 2020)"), markers(request, result));
        assertEquals(List.of("missing"), result.unresolvedKeys());
        assertEquals(List.of(), result.bibliography());
        verifyNoInteractions(style, pandoc);
    }

    @ParameterizedTest
    @EnumSource(value = BstCitationFormat.class, names = {"NUMERIC", "STYLE_DEFINED"})
    void narrativeCitationOnlyStillRejectsOmittedBstItems(BstCitationFormat format) throws IOException {
        PandocLatexConverter pandoc = mock(PandocLatexConverter.class);
        RenderRequest original = request(List.of(group("g", CitationType.AUTHORYEAR_INTEXT, "a")), List.of(entry("a", "A")));
        RenderRequest request = new RenderRequest(original.groupsInDocumentOrder(), original.databases(),
                new RenderOptions(format, false, "", "", "", false));
        BstRenderer renderer = new BstRenderer(style("ENTRY { title } {} {}\nREAD\n"), pandoc);

        RenderException error = assertThrows(RenderException.class, () -> renderer.render(request));

        assertEquals("The BST style omitted cited entry a", error.getMessage());
        verifyNoInteractions(pandoc);
    }

    @ParameterizedTest
    @EnumSource(value = BstCitationFormat.class, names = {"NUMERIC", "STYLE_DEFINED"})
    void narrativeCitationOnlyStillRequiresBstSource(BstCitationFormat format) {
        BstStyle style = mock(BstStyle.class);
        PandocLatexConverter pandoc = mock(PandocLatexConverter.class);
        IllegalArgumentException failure = new IllegalArgumentException("BST source unavailable");
        when(style.getSource()).thenThrow(failure);
        RenderRequest original = request(List.of(group("g", CitationType.AUTHORYEAR_INTEXT, "a")), List.of(entry("a", "A")));
        RenderRequest request = new RenderRequest(original.groupsInDocumentOrder(), original.databases(),
                new RenderOptions(format, false, "", "", "", false));

        RenderException error = assertThrows(RenderException.class, () -> new BstRenderer(style, pandoc).render(request));

        assertSame(failure, error.getCause());
        verify(style).getSource();
        verifyNoInteractions(pandoc);
    }

    @Test
    void narrativeMissingStyleDefinedLabelFailsBeforeConversion() throws IOException {
        PandocLatexConverter pandoc = mock(PandocLatexConverter.class);
        RenderRequest original = request(List.of(group("g", CitationType.AUTHORYEAR_INTEXT, "a")), List.of(entry("a", "A")));
        RenderRequest request = new RenderRequest(original.groupsInDocumentOrder(), original.databases(),
                new RenderOptions(BstCitationFormat.STYLE_DEFINED, false, "", "", "", false));
        BstRenderer renderer = new BstRenderer(style(SIMPLE_BST), pandoc);

        MissingCitationLabelException error = assertThrows(MissingCitationLabelException.class, () -> renderer.render(request));

        assertEquals("a", error.citationKey());
        verifyNoInteractions(pandoc);
    }

    @ParameterizedTest
    @EnumSource(value = CitationType.class, names = {"INVISIBLE_CIT", "AUTHORYEAR_NOPAR", "AUTHOR_ONLY", "YEAR_ONLY"})
    void storedUnsupportedTypesStillUseNormalOutput(CitationType type) throws IOException, InterruptedException, RenderException {
        RenderRequest request = withFormat(request(List.of(group("g", type, "a")), List.of(entry("a", "A"))), BstCitationFormat.AUTHOR_YEAR);

        RenderResult result = new BstRenderer(style(SIMPLE_BST), echoPandoc()).render(request);

        assertEquals(List.of("(Smith, 2020)"), markers(request, result));
        assertEquals(type, request.groupsInDocumentOrder().getFirst().citationType);
    }

    private BstStyle style(String source) throws IOException {
        Path path = temporaryDirectory.resolve("engine.bst");
        Files.writeString(path, source);
        return BstStyle.loadExternal(path);
    }

    private static RenderRequest withFormat(RenderRequest request, BstCitationFormat format) {
        return new RenderRequest(request.groupsInDocumentOrder(), request.databases(), new RenderOptions(format));
    }

    private static PandocLatexConverter echoPandoc() throws IOException, InterruptedException {
        PandocLatexConverter pandoc = mock(PandocLatexConverter.class);
        when(pandoc.latexToHtml(anyString())).thenAnswer(invocation -> Arrays.stream(invocation.<String>getArgument(0).split("\n\n"))
                                                                             .map(text -> "<p>" + text + "</p>")
                                                                             .collect(Collectors.joining("\n")));
        return pandoc;
    }
}
