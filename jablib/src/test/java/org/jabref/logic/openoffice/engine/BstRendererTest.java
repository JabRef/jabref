package org.jabref.logic.openoffice.engine;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.jabref.logic.l10n.Localization;
import org.jabref.logic.openoffice.bst.PandocLatexConverter;
import org.jabref.logic.openoffice.style.BstCitationFormat;
import org.jabref.logic.openoffice.style.BstStyle;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.openoffice.style.CitationType;

import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.parallel.ResourceLock;

import static org.jabref.logic.openoffice.engine.RendererTestData.SIMPLE_BST;
import static org.jabref.logic.openoffice.engine.RendererTestData.entry;
import static org.jabref.logic.openoffice.engine.RendererTestData.group;
import static org.jabref.logic.openoffice.engine.RendererTestData.markers;
import static org.jabref.logic.openoffice.engine.RendererTestData.request;
import static org.junit.jupiter.api.Assertions.assertEquals;
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
