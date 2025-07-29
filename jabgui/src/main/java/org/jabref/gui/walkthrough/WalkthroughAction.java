package org.jabref.gui.walkthrough;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.ListView;
import javafx.stage.Stage;

import org.jabref.gui.actions.SimpleCommand;
import org.jabref.gui.fieldeditors.LinkedFilesEditor;
import org.jabref.gui.frame.JabRefFrame;
import org.jabref.gui.maintable.MainTable;
import org.jabref.gui.preferences.PreferencesDialogView;
import org.jabref.gui.walkthrough.declarative.NavigationPredicate;
import org.jabref.gui.walkthrough.declarative.NodeResolver;
import org.jabref.gui.walkthrough.declarative.WindowResolver;
import org.jabref.gui.walkthrough.declarative.effect.HighlightEffect;
import org.jabref.gui.walkthrough.declarative.effect.WalkthroughEffect;
import org.jabref.gui.walkthrough.declarative.effect.WindowEffect;
import org.jabref.gui.walkthrough.declarative.richtext.InfoBlock;
import org.jabref.gui.walkthrough.declarative.richtext.TextBlock;
import org.jabref.gui.walkthrough.declarative.sideeffect.OpenLibrarySideEffect;
import org.jabref.gui.walkthrough.declarative.step.PanelPosition;
import org.jabref.gui.walkthrough.declarative.step.QuitButtonPosition;
import org.jabref.gui.walkthrough.declarative.step.TooltipPosition;
import org.jabref.gui.walkthrough.declarative.step.WalkthroughStep;
import org.jabref.logic.l10n.Localization;

import com.airhacks.afterburner.injection.Injector;

public class WalkthroughAction extends SimpleCommand {
    public static final String PDF_LINK_WALKTHROUGH_NAME = "pdfLink";
    public static final String MAIN_FILE_DIRECTORY_WALKTHROUGH_NAME = "mainFileDirectory";
    private static final Map<String, Walkthrough> WALKTHROUGH_CACHE = new ConcurrentHashMap<>();

    private final Walkthrough walkthrough;
    private final JabRefFrame frame;
    private final Stage stage;

    public WalkthroughAction(JabRefFrame frame, String name) {
        this.stage = Injector.instantiateModelOrService(Stage.class);
        this.frame = frame;
        this.walkthrough = getWalkthrough(name);
    }

    @Override
    public void execute() {
        walkthrough.start(this.stage);
    }

    private Walkthrough getWalkthrough(String name) {
        return WALKTHROUGH_CACHE.computeIfAbsent(name, _ ->
                switch (name) {
                    case MAIN_FILE_DIRECTORY_WALKTHROUGH_NAME ->
                            createMainFileDirectoryWalkthrough();
                    case PDF_LINK_WALKTHROUGH_NAME -> createPdfLinkWalkthrough();
                    default ->
                            throw new IllegalArgumentException("Unknown walkthrough: " + name);
                }
        );
    }

    private NavigationPredicate createFileAddedNavigationPredicate() {
        return (node, beforeNavigate, onNavigate) -> {
            ListView<?> listView = (ListView<?>) node.lookup("#listView");

            if (listView != null) {
                ListChangeListener<Object> listener = change -> {
                    while (change.next()) {
                        if (change.wasAdded() && !change.getAddedSubList().isEmpty()) {
                            beforeNavigate.run();
                            onNavigate.run();
                            break;
                        }
                    }
                };

                @SuppressWarnings("unchecked")
                ObservableList<Object> items = (ObservableList<Object>) listView.getItems();
                items.addListener(listener);

                return () -> items.removeListener(listener);
            }

            return () -> {
            };
        };
    }

    private Walkthrough createPdfLinkWalkthrough() {
        WalkthroughStep step1 = WalkthroughStep.sideEffect(Localization.lang("Open Example Library"))
                                               .sideEffect(new OpenLibrarySideEffect(frame))
                                               .build();

        WalkthroughStep step2 = WalkthroughStep
                .panel(Localization.lang("Welcome to PDF linking walkthrough"))
                .content(new TextBlock(Localization.lang("This walkthrough will guide you through how to link your PDF files with JabRef. We've opened an example library so you can see how this feature works with actual bibliography entries.")))
                .resolver(NodeResolver.predicate(MainTable.class::isInstance))
                .continueButton(Localization.lang("Continue"))
                .highlight(HighlightEffect.BACKDROP_HIGHLIGHT)
                .position(PanelPosition.BOTTOM)
                .build();

        WalkthroughStep step3 = WalkthroughStep
                .tooltip(Localization.lang("Double click on the \"Ding_2006\" entry"))
                .content(new TextBlock(Localization.lang("Let's start by selecting an entry to work with. Double click on the entry titled \"Chocolate and Prevention of Cardiovascular Disease: A Systematic Review\" by Ding et al.")))
                .resolver(NodeResolver.selectorWithText(".table-row-cell",
                        text -> "Ding_2006".equals(text)
                                || "Ding et al.".equals(text)
                                || "Chocolate and Prevention of Cardiovascular Disease: A Systematic Review".equals(text)))
                .navigation(NavigationPredicate.onDoubleClick())
                .highlight(HighlightEffect.BACKDROP_HIGHLIGHT)
                .build();

        WalkthroughStep step4 = WalkthroughStep
                .tooltip(Localization.lang("Click on the \"General\" tab"))
                .content(new TextBlock(Localization.lang("Now we need to access the entry editor. Click on the \"General\" tab to view and edit the entry details.")))
                .resolver(NodeResolver.selectorWithText(".tab", text -> Localization.lang("General").equals(text)))
                .navigation(NavigationPredicate.onClick())
                .highlight(HighlightEffect.BACKDROP_HIGHLIGHT)
                .build();

        WalkthroughStep step5 = WalkthroughStep
                .panel(Localization.lang("PDF file management area"))
                .content(
                        new TextBlock(Localization.lang(
                                "This is where you manage PDF files for this entry. You have three buttons on the right side to link a PDF file:\n" +
                                        "1. **Add PDF file** - Link a file from your computer\n" +
                                        "2. **Find full text** - Use JabRef's online fetchers to find the PDF\n" +
                                        "3. **Download from URL** - Enter a link and JabRef will download it for you.\n" +
                                        "Without using those buttons, you can also drag and drop PDF files directly onto this area.\n" +
                                        "\n" +
                                        "Try one of the options above to link a file. You can download a PDF from [this URL](https://nutritionandmetabolism.biomedcentral.com/articles/10.1186/1743-7075-3-2) or use any PDF file of your choice."
                        )),
                        new InfoBlock(Localization.lang("For detailed information: [Adding PDFs](https://docs.jabref.org/collect/add-pdfs-to-an-entry), [Managing files](https://docs.jabref.org/finding-sorting-and-cleaning-entries/filelinks), [Finding unlinked files](https://docs.jabref.org/collect/findunlinkedfiles)."))
                )
                .resolver(NodeResolver.predicate(LinkedFilesEditor.class::isInstance))
                .navigation(createFileAddedNavigationPredicate())
                .position(PanelPosition.RIGHT)
                .highlight(HighlightEffect.BACKDROP_HIGHLIGHT)
                .showQuitButton(false)
                .build();

        WalkthroughStep step6 = WalkthroughStep
                .panel(Localization.lang("Perfect! PDF file linked successfully"))
                .content(new TextBlock(Localization.lang("Congratulations! You have successfully linked a PDF file to a bibliography entry. This makes it easy to access your research documents directly from JabRef. You can repeat this process for all your entries.")))
                .resolver(NodeResolver.predicate(LinkedFilesEditor.class::isInstance))
                .continueButton(Localization.lang("Finish"))
                .position(PanelPosition.RIGHT)
                .highlight(HighlightEffect.BACKDROP_HIGHLIGHT)
                .build();

        return new Walkthrough(step1, step2, step3, step4, step5, step6);
    }

    private Walkthrough createMainFileDirectoryWalkthrough() {
        WindowResolver mainResolver = () -> Optional.of(stage);

        WalkthroughStep step1 = WalkthroughStep
                .tooltip(Localization.lang("Click on \"File\" menu"))
                .resolver(NodeResolver.selector(".menu-bar .menu-button:first-child"))
                .navigation(NavigationPredicate.onClick())
                .position(TooltipPosition.BOTTOM)
                .highlight(HighlightEffect.BACKDROP_HIGHLIGHT)
                .build();

        WalkthroughStep step2 = WalkthroughStep
                .tooltip(Localization.lang("Click on \"Preferences\""))
                .resolver(NodeResolver.menuItem("Preferences"))
                .navigation(NavigationPredicate.onClick())
                .position(TooltipPosition.RIGHT)
                .activeWindow(WindowResolver.clazz(ContextMenu.class))
                .highlight(new WalkthroughEffect(
                        new WindowEffect(HighlightEffect.ANIMATED_PULSE),
                        new WindowEffect(mainResolver, HighlightEffect.FULL_SCREEN_DARKEN)
                ))
                .showQuitButton(false)
                .build();

        WalkthroughEffect preferenceHighlight = new WalkthroughEffect(
                new WindowEffect(HighlightEffect.BACKDROP_HIGHLIGHT),
                new WindowEffect(mainResolver, HighlightEffect.FULL_SCREEN_DARKEN)
        );

        WalkthroughStep step3 = WalkthroughStep
                .tooltip(Localization.lang("Select the \"Linked files\" tab"))
                .content(new TextBlock(Localization.lang("This section manages how JabRef handles your PDF files and other documents.")))
                .resolver(NodeResolver.selectorWithText(".list-cell", text -> Localization.lang("Linked files").equals(text)))
                .navigation(NavigationPredicate.onClick())
                .position(TooltipPosition.AUTO)
                .activeWindow(WindowResolver.title(PreferencesDialogView.DIALOG_TITLE))
                .highlight(preferenceHighlight)
                .build();

        WalkthroughStep step4 = WalkthroughStep
                .tooltip(Localization.lang("Enable \"Main file directory\" option"))
                .content(new TextBlock(Localization.lang("Choose this option to tell JabRef where your research files are stored. This makes it easy to attach PDFs and other documents to your bibliography entries. You can browse to select your preferred folder in the next step.")))
                .resolver(NodeResolver.fxId("useMainFileDirectory"))
                .navigation(NavigationPredicate.onClick())
                .position(TooltipPosition.AUTO)
                .highlight(preferenceHighlight)
                .activeWindow(WindowResolver.title(PreferencesDialogView.DIALOG_TITLE))
                .build();

        WalkthroughStep step5 = WalkthroughStep
                .panel(Localization.lang("Click \"Save\" to save changes"))
                .content(
                        new TextBlock(Localization.lang("Congratulations. Your main file directory is now configured. JabRef will use this location to automatically find and organize your research documents.")),
                        new InfoBlock(Localization.lang("Additional information on main file directory can be found in [help](https://docs.jabref.org/v5/finding-sorting-and-cleaning-entries/filelinks)"))
                )
                .resolver(NodeResolver.selectorWithText(".button", text -> Localization.lang("Save").equals(text)))
                .navigation(NavigationPredicate.onClick())
                .position(PanelPosition.TOP)
                .quitButtonPosition(QuitButtonPosition.BOTTOM_LEFT)
                .highlight(preferenceHighlight)
                .activeWindow(WindowResolver.title(PreferencesDialogView.DIALOG_TITLE))
                .build();

        return new Walkthrough(step1, step2, step3, step4, step5);
    }
}
