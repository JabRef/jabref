package org.jabref.gui.preferences;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Control;
import javafx.scene.control.Labeled;

import org.jabref.gui.DialogService;
import org.jabref.gui.StateManager;
import org.jabref.gui.clipboard.ClipBoardManager;
import org.jabref.gui.preferences.ai.AiTab;
import org.jabref.gui.preferences.autocompletion.AutoCompletionTab;
import org.jabref.gui.preferences.citationkeypattern.CitationKeyPatternTab;
import org.jabref.gui.preferences.customentrytypes.CustomEntryTypesTab;
import org.jabref.gui.preferences.customexporter.CustomExporterTab;
import org.jabref.gui.preferences.customimporter.CustomImporterTab;
import org.jabref.gui.preferences.entry.EntryTab;
import org.jabref.gui.preferences.entryeditor.EntryEditorTab;
import org.jabref.gui.preferences.export.ExportTab;
import org.jabref.gui.preferences.external.ExternalTab;
import org.jabref.gui.preferences.externalfiletypes.ExternalFileTypesTab;
import org.jabref.gui.preferences.general.GeneralTab;
import org.jabref.gui.preferences.groups.GroupsTab;
import org.jabref.gui.preferences.journals.JournalAbbreviationsTab;
import org.jabref.gui.preferences.keybindings.KeyBindingsTab;
import org.jabref.gui.preferences.linkedfiles.LinkedFilesTab;
import org.jabref.gui.preferences.nameformatter.NameFormatterTab;
import org.jabref.gui.preferences.network.NetworkTab;
import org.jabref.gui.preferences.ocr.OcrTab;
import org.jabref.gui.preferences.preview.PreviewTab;
import org.jabref.gui.preferences.protectedterms.ProtectedTermsTab;
import org.jabref.gui.preferences.table.TableTab;
import org.jabref.gui.preferences.websearch.WebSearchTab;
import org.jabref.gui.preferences.xmp.XmpPrivacyTab;
import org.jabref.languageserver.controller.LanguageServerController;
import org.jabref.logic.UiMessageHandler;
import org.jabref.logic.ai.AiService;
import org.jabref.logic.ai.preferences.AiPreferences;
import org.jabref.logic.preferences.CliPreferences;
import org.jabref.logic.protectedterms.ProtectedTermsLoader;
import org.jabref.logic.util.CurrentThreadTaskExecutor;
import org.jabref.logic.util.TaskExecutor;
import org.jabref.logic.util.strings.StringUtil;

import com.airhacks.afterburner.injection.Injector;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Named;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Answers;
import org.testfx.framework.junit5.ApplicationExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

/// Guards the invariant the preferences search rests on: every visible text of a tab is registered
/// as a [SearchableElement], whether the form builder placed it or a `custom(...)` node brought it.
/// Without this test, a custom node whose texts nobody registered is a hole in the search that
/// nothing else reports.
@ExtendWith(ApplicationExtension.class)
class PreferencesSearchCoverageTest {

    /// The dialog-scoped working copy the AI and web search tabs share.
    private static final AiPreferences AI_PREFERENCES = mock(AiPreferences.class, Answers.RETURNS_DEEP_STUBS);

    @BeforeAll
    static void bindServices() {
        GuiPreferences preferences = mock(GuiPreferences.class, Answers.RETURNS_DEEP_STUBS);
        Injector.setModelOrService(GuiPreferences.class, preferences);
        // The panels a tab hands to custom(...) are afterburner views, which inject the preferences
        // under the interface they were written against.
        Injector.setModelOrService(CliPreferences.class, preferences);
        Injector.setModelOrService(DialogService.class, mock(DialogService.class));
        Injector.setModelOrService(TaskExecutor.class, new CurrentThreadTaskExecutor());
        Injector.setModelOrService(StateManager.class, mock(StateManager.class, Answers.RETURNS_DEEP_STUBS));
        Injector.setModelOrService(AiService.class, mock(AiService.class, Answers.RETURNS_DEEP_STUBS));
        Injector.setModelOrService(ProtectedTermsLoader.class, mock(ProtectedTermsLoader.class, Answers.RETURNS_DEEP_STUBS));
        Injector.setModelOrService(LanguageServerController.class, mock(LanguageServerController.class));
        Injector.setModelOrService(UiMessageHandler.class, mock(UiMessageHandler.class));
        Injector.setModelOrService(ClipBoardManager.class, mock(ClipBoardManager.class));
    }

    /// The tabs are created inside the test rather than here, because a parameter source runs
    /// before the JavaFX toolkit is up and a tab builds controls.
    private static Stream<Named<Supplier<PreferencesTab>>> tabs() {
        return Stream.of(
                Named.of("General", GeneralTab::new),
                Named.of("Keyboard shortcuts", KeyBindingsTab::new),
                Named.of("Groups", GroupsTab::new),
                Named.of("Entry", EntryTab::new),
                Named.of("Entry table", TableTab::new),
                Named.of("Entry preview", PreviewTab::new),
                Named.of("Entry editor", EntryEditorTab::new),
                Named.of("Custom entry types", CustomEntryTypesTab::new),
                Named.of("Citation key generator", CitationKeyPatternTab::new),
                Named.of("Linked files", LinkedFilesTab::new),
                Named.of("OCR", OcrTab::new),
                Named.of("Export", ExportTab::new),
                Named.of("Autocompletion", AutoCompletionTab::new),
                Named.of("Protected terms files", ProtectedTermsTab::new),
                Named.of("External programs", ExternalTab::new),
                Named.of("External file types", ExternalFileTypesTab::new),
                Named.of("Journal abbreviations", JournalAbbreviationsTab::new),
                Named.of("Name formatter", NameFormatterTab::new),
                Named.of("XMP metadata", XmpPrivacyTab::new),
                Named.of("Custom import formats", CustomImporterTab::new),
                Named.of("Custom export formats", CustomExporterTab::new),
                Named.of("Network", NetworkTab::new),
                Named.of("Web search", () -> new WebSearchTab(AI_PREFERENCES)),
                Named.of("AI", () -> new AiTab(AI_PREFERENCES)));
    }

    @ParameterizedTest
    @MethodSource("tabs")
    void everyVisibleTextIsSearchable(Supplier<PreferencesTab> tabFactory) {
        PreferencesTab tab = tabFactory.get();

        Set<String> registered = tab.getSearchableElements().stream()
                                    .map(SearchableElement::text)
                                    .collect(Collectors.toSet());

        List<String> unregistered = visibleTexts(tab.getContent()).stream()
                                                                  .filter(text -> !registered.contains(text))
                                                                  .toList();

        assertEquals(List.of(), unregistered,
                "these texts of tab \"" + tab.getTabName() + "\" cannot be found by the preferences search");
    }

    /// The texts a user reads on the tab, collected the way the builder's escape hatch collects
    /// them: the descent stops at a control, whose insides belong to its skin. Buttons are left
    /// out — they carry the actions of a list ("Add", "Remove", "Reset to default"), not the name
    /// of a setting, and are deliberately not searchable.
    private static List<String> visibleTexts(Node node) {
        List<String> texts = new ArrayList<>();
        collect(node, texts);
        return texts;
    }

    private static void collect(Node current, List<String> texts) {
        if (current instanceof Button) {
            return;
        }
        if (current instanceof Labeled labeled && StringUtil.isNotBlank(labeled.getText())) {
            texts.add(labeled.getText());
        } else if (current instanceof Parent parent && !(current instanceof Control)) {
            parent.getChildrenUnmodifiable().forEach(child -> collect(child, texts));
        }
    }
}
