package org.jabref.gui.walkthrough;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.DialogPane;
import javafx.scene.control.ListView;
import javafx.stage.Stage;

import org.jabref.gui.StateManager;
import org.jabref.gui.actions.SimpleCommand;
import org.jabref.gui.fieldeditors.LinkedFilesEditor;
import org.jabref.gui.fieldeditors.LinkedFilesEditorViewModel;
import org.jabref.gui.frame.JabRefFrame;
import org.jabref.gui.icon.IconTheme;
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

public class WalkthroughAction extends SimpleCommand {
    public static final String PDF_LINK_WALKTHROUGH_NAME = "pdfLink";
    public static final String MAIN_FILE_DIRECTORY_WALKTHROUGH_NAME = "mainFileDirectory";
    private static final Map<String, Walkthrough> WALKTHROUGH_CACHE = new ConcurrentHashMap<>();

    private final Walkthrough walkthrough;
    private final JabRefFrame frame;
    private final StateManager stateManager;
    private final Stage stage;

    public WalkthroughAction(Stage stage, JabRefFrame frame, StateManager stateManager, String name) {
        this.stage = stage;
        this.frame = frame;
        this.stateManager = stateManager;
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

            if (listView == null) {
                return () -> {
                };
            }

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
        };
    }

    private NavigationPredicate createFetchFulltextNavigationPredicate() {
        return (node, beforeNavigate, onNavigate) -> {
            if (!(node instanceof LinkedFilesEditor linkedFilesEditor)) {
                throw new IllegalArgumentException("Node must be an instance of LinkedFilesEditor");
            }

            LinkedFilesEditorViewModel viewModel = linkedFilesEditor.getViewModel();
            AtomicBoolean hasTriggered = new AtomicBoolean(false);

            ChangeListener<Boolean> fulltextListener = (_, _, inProgress) -> {
                if (!inProgress && hasTriggered.compareAndSet(false, true)) {
                    Platform.runLater(() -> {
                        beforeNavigate.run();
                        onNavigate.run();
                    });
                }
            };

            viewModel.fulltextLookupInProgressProperty().addListener(fulltextListener);

            return () -> viewModel.fulltextLookupInProgressProperty().removeListener(fulltextListener);
        };
    }

    private Walkthrough createPdfLinkWalkthrough() {
        WalkthroughEffect pdfDialogEffect = new WalkthroughEffect(
                new WindowEffect(() -> Optional.of(stage), HighlightEffect.FULL_SCREEN_DARKEN),
                new WindowEffect(HighlightEffect.PING)
        );

        return WalkthroughBuilder.create(stateManager)
                // Setup: Open example library and welcome user
                .addStep(WalkthroughStep.sideEffect(Localization.lang("Open Example Library"))
                        .sideEffect(new OpenLibrarySideEffect(frame))
                        .build())
                .addStep(WalkthroughStep
                        .panel(Localization.lang("Welcome to PDF linking walkthrough"))
                        .content(new TextBlock(Localization.lang("This walkthrough will guide you through how to link your PDF files with JabRef. We've opened an example library so you can see how this feature works with actual bibliography entries.")))
                        .resolver(NodeResolver.predicate(MainTable.class::isInstance))
                        .continueButton(Localization.lang("Continue"))
                        .highlight(HighlightEffect.SPOT_LIGHT)
                        .position(PanelPosition.BOTTOM)
                        .build())
                // Navigate to entry editor
                .addStep(WalkthroughStep
                        .tooltip(Localization.lang("Double click on the \"Ding_2006\" entry"))
                        .content(new TextBlock(Localization.lang("Let's start by selecting an entry to work with. Double click on the entry titled \"Chocolate and Prevention of Cardiovascular Disease: A Systematic Review\" by Ding et al.")))
                        .resolver(NodeResolver.selectorWithText(".table-row-cell",
                                text -> "Ding_2006".equals(text)
                                        || "Ding et al.".equals(text)
                                        || "Chocolate and Prevention of Cardiovascular Disease: A Systematic Review".equals(text)))
                        .navigation(NavigationPredicate.onDoubleClick())
                        .highlight(HighlightEffect.SPOT_LIGHT)
                        .build())
                .addStep(WalkthroughStep
                        .tooltip(Localization.lang("Click on the \"General\" tab"))
                        .content(new TextBlock(Localization.lang("Now we need to access the entry editor. Click on the \"General\" tab to view and edit the entry details.")))
                        .resolver(NodeResolver.selectorWithText(".tab", text -> Localization.lang("General").equals(text)))
                        .navigation(NavigationPredicate.onClick())
                        .highlight(HighlightEffect.SPOT_LIGHT)
                        .build())
                // Introduce PDF management area and demonstrate drag & drop
                .addStep(WalkthroughStep
                        .panel(Localization.lang("PDF file management area"))
                        .content(
                                new TextBlock(Localization.lang("This is the PDF file management area where you can link files to your bibliography entries. Notice the three buttons on the right side—each offers a different way to add PDF files. Let's explore each method step by step."))
                        )
                        .resolver(NodeResolver.predicate(LinkedFilesEditor.class::isInstance))
                        .position(PanelPosition.RIGHT)
                        .quitButtonPosition(QuitButtonPosition.BOTTOM_LEFT)
                        .highlight(HighlightEffect.SPOT_LIGHT)
                        .showQuitButton(false)
                        .continueButton(Localization.lang("Continue"))
                        .build())
                .addStep(WalkthroughStep
                        .panel(Localization.lang("Drag and drop files"))
                        .content(
                                new TextBlock(Localization.lang("You can drag and drop PDF files directly onto the file list area. This is often the quickest way to link files that are already on your computer.")),
                                new InfoBlock(Localization.lang("Try dragging a PDF file here to continue the walkthrough."))
                        )
                        .resolver(NodeResolver.predicate(LinkedFilesEditor.class::isInstance))
                        .navigation(createFileAddedNavigationPredicate())
                        .position(PanelPosition.RIGHT)
                        .highlight(HighlightEffect.SPOT_LIGHT)
                        .showQuitButton(false)
                        .build())
                // Method 1: Add file from computer
                .addStep(WalkthroughStep
                        .tooltip(Localization.lang("Click \"Add\" to link a file from your computer"))
                        .content(new TextBlock(Localization.lang("Click the \"Add\" button (first button with a plus icon) to link a PDF file that you already have on your computer. This will open a dialog where you can browse and select the file.")))
                        .resolver(NodeResolver.buttonWithGraphic(IconTheme.JabRefIcons.LINKED_FILE_ADD))
                        .navigation(NavigationPredicate.onClick())
                        .position(TooltipPosition.LEFT)
                        .highlight(HighlightEffect.SPOT_LIGHT)
                        .build())
                .addStep(WalkthroughStep
                        .tooltip(Localization.lang("Browse for your PDF file"))
                        .content(new TextBlock(Localization.lang("Use the \"Browse\" button to select a PDF file from your computer. Click the folder icon next to the \"Link\" field to open the file browser.")))
                        .resolver(NodeResolver.fxId("browse"))
                        .navigation(NavigationPredicate.onClick())
                        .activeWindow(WindowResolver.title(Localization.lang("Add file link")))
                        .showQuitButton(false)
                        .highlight(pdfDialogEffect)
                        .build())
                .addStep(WalkthroughStep
                        .tooltip(Localization.lang("Add a description for the file"))
                        .content(new TextBlock(Localization.lang("Enter a meaningful description for this file in the \"Description\" field. This helps you identify the file later.")))
                        .resolver(NodeResolver.fxId("description"))
                        .navigation(NavigationPredicate.onTextInput())
                        .activeWindow(WindowResolver.title(Localization.lang("Add file link")))
                        .showQuitButton(false)
                        .highlight(pdfDialogEffect)
                        .build())
                .addStep(WalkthroughStep
                        .tooltip(Localization.lang("Select the file type"))
                        .content(new TextBlock(Localization.lang("Choose the appropriate file type from the \"Filetype\" dropdown. Usually \"PDF\" is the correct choice for research papers.")))
                        .resolver(NodeResolver.fxId("fileType"))
                        .navigation(NavigationPredicate.onClick())
                        .activeWindow(WindowResolver.title(Localization.lang("Add file link")))
                        .showQuitButton(false)
                        .highlight(pdfDialogEffect)
                        .build())
                .addStep(WalkthroughStep
                        .tooltip(Localization.lang("Optionally add source URL"))
                        .content(new TextBlock(Localization.lang("If you downloaded this file from a website, you can add the source URL in the \"Source URL\" field. This is optional but helpful for tracking where you found the file.")))
                        .resolver(NodeResolver.fxId("sourceUrl"))
                        .navigation(NavigationPredicate.onTextInput())
                        .activeWindow(WindowResolver.title(Localization.lang("Add file link")))
                        .highlight(pdfDialogEffect)
                        .showQuitButton(false)
                        .build())
                .addStep(WalkthroughStep
                        .tooltip(Localization.lang("Complete the file linking"))
                        .content(new TextBlock(Localization.lang("Now click \"Add\" to complete the file linking process, or \"Cancel\" to try another method.")))
                        .resolver(NodeResolver.selectorWithText(".button", text -> Localization.lang("Add").equals(text)))
                        .navigation(NavigationPredicate.onClick())
                        .activeWindow(WindowResolver.title(Localization.lang("Add file link")))
                        .highlight(pdfDialogEffect)
                        .showQuitButton(false)
                        .build())
                // Method 2: Get fulltext automatically
                .addStep(WalkthroughStep
                        .tooltip(Localization.lang("Click \"Get fulltext\" to find PDFs automatically"))
                        .content(new TextBlock(Localization.lang("Click the \"Get fulltext\" button (second button with a download icon) to let JabRef automatically search for and download the PDF using online fetchers. This works when your entry has proper metadata like DOI or title.")))
                        .resolver(NodeResolver.buttonWithGraphic(IconTheme.JabRefIcons.FETCH_FULLTEXT))
                        .navigation(NavigationPredicate.onClick())
                        .position(TooltipPosition.LEFT)
                        .highlight(HighlightEffect.SPOT_LIGHT)
                        .build())
                .addStep(WalkthroughStep
                        .tooltip(Localization.lang("Wait for fulltext search to complete"))
                        .content(new TextBlock(Localization.lang("JabRef is now searching for the full text of this paper using various online sources. Please wait for the search to complete.")))
                        .resolver(NodeResolver.predicate(LinkedFilesEditor.class::isInstance))
                        .navigation(createFetchFulltextNavigationPredicate())
                        .position(TooltipPosition.LEFT)
                        .highlight(HighlightEffect.SPOT_LIGHT)
                        .build())
                // Method 3: Download from URL
                .addStep(WalkthroughStep
                        .tooltip(Localization.lang("Click \"Download from URL\" to download from a web link"))
                        .content(new TextBlock(Localization.lang("Click the \"Download from URL\" button (third button with a download icon) to download a PDF directly from a web URL. JabRef will prompt you to enter the URL and then download the file automatically.")))
                        .resolver(NodeResolver.buttonWithGraphic(IconTheme.JabRefIcons.DOWNLOAD))
                        .navigation(NavigationPredicate.onClick())
                        .position(TooltipPosition.LEFT)
                        .highlight(HighlightEffect.SPOT_LIGHT)
                        .build())
                .addStep(WalkthroughStep
                        .tooltip(Localization.lang("Enter URL for download"))
                        .content(new TextBlock(Localization.lang("Enter the URL of the PDF file you want to download. You can try this example URL: https://nutritionandmetabolism.biomedcentral.com/articles/10.1186/1743-7075-3-2")))
                        .resolver(NodeResolver.selector(".text-input"))
                        .navigation(NavigationPredicate.onTextInput())
                        .activeWindow(WindowResolver.not(stage))
                        .showQuitButton(false)
                        .highlight(pdfDialogEffect)
                        .build())
                .addStep(WalkthroughStep
                        .tooltip(Localization.lang("Confirm URL download"))
                        .content(new TextBlock(Localization.lang("Click \"OK\" to start downloading the PDF from the entered URL.")))
                        .resolver(scene -> NodeResolver.predicate(DialogPane.class::isInstance)
                                .resolve(scene)
                                .map(node -> node instanceof DialogPane pane ? pane.lookupButton(ButtonType.OK) : null))
                        .navigation(NavigationPredicate.onClick())
                        .activeWindow(WindowResolver.not(stage))
                        .highlight(pdfDialogEffect)
                        .showQuitButton(false)
                        .build())
                // Completion
                .addStep(WalkthroughStep
                        .panel(Localization.lang("Perfect! PDF file linked successfully"))
                        .content(
                                new TextBlock(Localization.lang("Congratulations! You have successfully linked a PDF file to a bibliography entry. This makes it easy to access your research documents directly from JabRef. You can repeat this process for all your entries.")),
                                new InfoBlock(Localization.lang("For detailed information: [Adding PDFs](https://docs.jabref.org/collect/add-pdfs-to-an-entry), [Managing files](https://docs.jabref.org/finding-sorting-and-cleaning-entries/filelinks), [Finding unlinked files](https://docs.jabref.org/collect/findunlinkedfiles)."))
                        )
                        .resolver(NodeResolver.predicate(LinkedFilesEditor.class::isInstance))
                        .continueButton(Localization.lang("Finish"))
                        .position(PanelPosition.RIGHT)
                        .quitButtonPosition(QuitButtonPosition.BOTTOM_LEFT)
                        .highlight(HighlightEffect.SPOT_LIGHT)
                        .build())
                .build();
    }

    private Walkthrough createMainFileDirectoryWalkthrough() {
        WindowResolver mainResolver = () -> Optional.of(stage);
        WalkthroughEffect preferenceHighlight = new WalkthroughEffect(
                new WindowEffect(HighlightEffect.SPOT_LIGHT),
                new WindowEffect(mainResolver, HighlightEffect.FULL_SCREEN_DARKEN)
        );

        return WalkthroughBuilder.create(stateManager)
                // Navigate to preferences dialog
                .addStep(WalkthroughStep
                        .tooltip(Localization.lang("Click on \"File\" menu"))
                        .resolver(NodeResolver.selector(".menu-bar .menu-button:first-child"))
                        .navigation(NavigationPredicate.onClick())
                        .position(TooltipPosition.BOTTOM)
                        .highlight(HighlightEffect.SPOT_LIGHT)
                        .build())
                .addStep(WalkthroughStep
                        .tooltip(Localization.lang("Click on \"Preferences\""))
                        .resolver(NodeResolver.menuItem("Preferences"))
                        .navigation(NavigationPredicate.onClick())
                        .position(TooltipPosition.RIGHT)
                        .activeWindow(WindowResolver.clazz(ContextMenu.class))
                        .highlight(new WalkthroughEffect(
                                new WindowEffect(HighlightEffect.PING),
                                new WindowEffect(mainResolver, HighlightEffect.FULL_SCREEN_DARKEN)
                        ))
                        .showQuitButton(false)
                        .build())
                // Configure main file directory settings
                .addStep(WalkthroughStep
                        .tooltip(Localization.lang("Select the \"Linked files\" tab"))
                        .content(new TextBlock(Localization.lang("This section manages how JabRef handles your PDF files and other documents.")))
                        .resolver(NodeResolver.selectorWithText(".list-cell", text -> Localization.lang("Linked files").equals(text)))
                        .navigation(NavigationPredicate.onClick())
                        .position(TooltipPosition.AUTO)
                        .quitButtonPosition(QuitButtonPosition.BOTTOM_LEFT)
                        .activeWindow(WindowResolver.title(PreferencesDialogView.DIALOG_TITLE))
                        .highlight(preferenceHighlight)
                        .build())
                .addStep(WalkthroughStep
                        .tooltip(Localization.lang("Enable \"Main file directory\" option"))
                        .content(new TextBlock(Localization.lang("Choose this option to tell JabRef where your research files are stored. This makes it easy to attach PDFs and other documents to your bibliography entries. You can browse to select your preferred folder in the next step.")))
                        .resolver(NodeResolver.fxId("useMainFileDirectory"))
                        .navigation(NavigationPredicate.onClick())
                        .position(TooltipPosition.AUTO)
                        .highlight(preferenceHighlight)
                        .quitButtonPosition(QuitButtonPosition.BOTTOM_LEFT)
                        .activeWindow(WindowResolver.title(PreferencesDialogView.DIALOG_TITLE))
                        .build())
                .addStep(WalkthroughStep
                        .tooltip(Localization.lang("Specify \"Main file directory\" option"))
                        .resolver(scene -> Optional.ofNullable(scene.lookup("#useMainFileDirectory").getParent()))
                        .position(TooltipPosition.BOTTOM)
                        .highlight(preferenceHighlight)
                        .quitButtonPosition(QuitButtonPosition.BOTTOM_LEFT)
                        .activeWindow(WindowResolver.title(PreferencesDialogView.DIALOG_TITLE))
                        .continueButton(Localization.lang("Continue"))
                        .build())
                // Complete configuration
                .addStep(WalkthroughStep
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
                        .build())
                .build();
    }
}
