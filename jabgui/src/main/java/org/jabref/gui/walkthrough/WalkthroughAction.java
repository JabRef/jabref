package org.jabref.gui.walkthrough;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import javafx.scene.control.ButtonType;
import javafx.scene.control.ContextMenu;
import javafx.scene.input.KeyCombination;
import javafx.stage.Stage;
import javafx.util.Duration;

import org.jabref.gui.LibraryTabContainer;
import org.jabref.gui.StateManager;
import org.jabref.gui.actions.SimpleCommand;
import org.jabref.gui.fieldeditors.LinkedFilesEditor;
import org.jabref.gui.icon.IconTheme;
import org.jabref.gui.keyboard.KeyBinding;
import org.jabref.gui.maintable.MainTable;
import org.jabref.gui.preferences.GuiPreferences;
import org.jabref.gui.preferences.PreferencesDialogView;
import org.jabref.gui.search.GlobalSearchBar;
import org.jabref.gui.util.URLs;
import org.jabref.gui.walkthrough.declarative.NodeResolver;
import org.jabref.gui.walkthrough.declarative.Trigger;
import org.jabref.gui.walkthrough.declarative.WindowResolver;
import org.jabref.gui.walkthrough.declarative.effect.HighlightEffect;
import org.jabref.gui.walkthrough.declarative.effect.WalkthroughEffect;
import org.jabref.gui.walkthrough.declarative.effect.WindowEffect;
import org.jabref.gui.walkthrough.declarative.richtext.InfoBlock;
import org.jabref.gui.walkthrough.declarative.richtext.TextBlock;
import org.jabref.gui.walkthrough.declarative.sideeffect.EnsureSearchSettingsSideEffect;
import org.jabref.gui.walkthrough.declarative.sideeffect.OpenLibrarySideEffect;
import org.jabref.gui.walkthrough.declarative.step.PanelPosition;
import org.jabref.gui.walkthrough.declarative.step.QuitButtonPosition;
import org.jabref.gui.walkthrough.declarative.step.TooltipPosition;
import org.jabref.gui.walkthrough.declarative.step.WalkthroughStep;
import org.jabref.logic.l10n.Localization;

import org.controlsfx.control.textfield.CustomTextField;

public class WalkthroughAction extends SimpleCommand {
    public static final String PDF_LINK_WALKTHROUGH_NAME = "pdfLink";
    public static final String MAIN_FILE_DIRECTORY_WALKTHROUGH_NAME = "mainFileDirectory";
    public static final String CUSTOMIZE_ENTRY_TABLE_WALKTHROUGH_NAME = "customizeEntryTable";
    public static final String GROUP_WALKTHROUGH_NAME = "group";
    public static final String SEARCH_WALKTHROUGH_NAME = "search";

    private static final Map<String, Walkthrough> WALKTHROUGH_CACHE = new ConcurrentHashMap<>();

    private final Walkthrough walkthrough;
    private final LibraryTabContainer frame;
    private final StateManager stateManager;
    private final Stage stage;
    private final GuiPreferences preferences;

    public WalkthroughAction(Stage stage, LibraryTabContainer frame, StateManager stateManager, GuiPreferences preferences, String name) {
        this.stage = stage;
        this.frame = frame;
        this.stateManager = stateManager;
        this.preferences = preferences;
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
                    case PDF_LINK_WALKTHROUGH_NAME ->
                            createPdfLinkWalkthrough();
                    case CUSTOMIZE_ENTRY_TABLE_WALKTHROUGH_NAME ->
                            createCustomizeEntryTableWalkthrough();
                    case GROUP_WALKTHROUGH_NAME ->
                            createGroupWalkthrough();
                    case SEARCH_WALKTHROUGH_NAME ->
                            createSearchWalkthrough();
                    default ->
                            throw new IllegalArgumentException("Unknown walkthrough: " + name);
                }
        );
    }

    private Walkthrough createCustomizeEntryTableWalkthrough() {
        WindowResolver mainResolver = () -> Optional.of(stage);
        WalkthroughEffect preferenceHighlight = new WalkthroughEffect(
                new WindowEffect(HighlightEffect.SPOT_LIGHT),
                new WindowEffect(mainResolver, HighlightEffect.FULL_SCREEN_DARKEN)
        );

        return Walkthrough
                .create(stateManager)
                // Navigate to preferences dialog
                .addStep(WalkthroughStep
                        .tooltip(Localization.lang("Click on \"File\" menu"))
                        .resolver(NodeResolver.selector(".menu-bar .menu-button:first-child"))
                        .trigger(Trigger.onClick())
                        .position(TooltipPosition.BOTTOM)
                        .highlight(HighlightEffect.SPOT_LIGHT)
                )
                .addStep(WalkthroughStep
                        .tooltip(Localization.lang("Click on \"Preferences\""))
                        .resolver(NodeResolver.menuItem("Preferences"))
                        .trigger(Trigger.create().withWindowChangeListener().onClick())
                        .position(TooltipPosition.RIGHT)
                        .activeWindow(WindowResolver.clazz(ContextMenu.class))
                        .highlight(new WalkthroughEffect(
                                new WindowEffect(HighlightEffect.PING),
                                new WindowEffect(mainResolver, HighlightEffect.FULL_SCREEN_DARKEN)
                        ))
                        .showQuitButton(false)
                )
                // Configure entry table settings
                .addStep(WalkthroughStep
                        .tooltip(Localization.lang("Select the \"Entry table\" tab"))
                        .content(new TextBlock(Localization.lang("This section allows you to customize the columns displayed in the entry table when viewing your bibliography.")))
                        .resolver(NodeResolver.selectorWithText(".list-cell", text -> Localization.lang("Entry table").equals(text)))
                        .trigger(Trigger.onClick())
                        .position(TooltipPosition.AUTO)
                        .quitButtonPosition(QuitButtonPosition.BOTTOM_LEFT)
                        .activeWindow(WindowResolver.title(PreferencesDialogView.DIALOG_TITLE))
                        .highlight(preferenceHighlight)
                )
                .addStep(WalkthroughStep
                        .panel(Localization.lang("Customize your entry table columns"))
                        .content(
                                new TextBlock(Localization.lang("Here you can customize which columns appear in your entry table. You can add, remove, or reorder columns such as citation key, title, author, year, and journal. This helps you see the most relevant information for your research at a glance.")),
                                new InfoBlock(Localization.lang("The columns you configure here will be displayed whenever you open a library in JabRef. You can always return to this settings page to modify your column preferences."))
                        )
                        .continueButton(Localization.lang("Next"))
                        .resolver(NodeResolver.fxId("columnsList"))
                        .position(PanelPosition.RIGHT)
                        .quitButtonPosition(QuitButtonPosition.BOTTOM_LEFT)
                        .highlight(preferenceHighlight)
                        .activeWindow(WindowResolver.title(PreferencesDialogView.DIALOG_TITLE))
                )
                // Complete configuration
                .addStep(WalkthroughStep
                        .panel(Localization.lang("Click \"Save\" to save changes"))
                        .content(
                                new TextBlock(Localization.lang("Your entry table columns are now configured. These settings will be applied to all your libraries in JabRef.")),
                                new InfoBlock(Localization.lang("You can find more information about customizing JabRef at [documentation](%0)", URLs.ENTRY_TABLE_COLUMNS_DOC))
                        )
                        .resolver(NodeResolver.selectorWithText(".button", text -> Localization.lang("Save").equals(text)))
                        .trigger(Trigger.onClick())
                        .position(PanelPosition.TOP)
                        .quitButtonPosition(QuitButtonPosition.BOTTOM_LEFT)
                        .highlight(preferenceHighlight)
                        .activeWindow(WindowResolver.title(PreferencesDialogView.DIALOG_TITLE))
                )
                .build();
    }

    private Walkthrough createPdfLinkWalkthrough() {
        WalkthroughEffect pdfDialogEffect = new WalkthroughEffect(
                new WindowEffect(() -> Optional.of(stage), HighlightEffect.FULL_SCREEN_DARKEN),
                new WindowEffect(HighlightEffect.PING)
        );

        return Walkthrough
                .create(stateManager)
                // Setup: Open example library and welcome user
                .addStep(WalkthroughStep.sideEffect(Localization.lang("Open Example Library"))
                                        .sideEffect(new OpenLibrarySideEffect(frame)))
                .addStep(WalkthroughStep
                        .panel(Localization.lang("Welcome to PDF linking walkthrough"))
                        .content(new TextBlock(Localization.lang("This walkthrough will guide you through how to link your PDF files with JabRef. We've opened an example library so you can see how this feature works with actual bibliography entries.")))
                        .resolver(NodeResolver.predicate(MainTable.class::isInstance))
                        .continueButton(Localization.lang("Continue"))
                        .highlight(HighlightEffect.SPOT_LIGHT)
                        .position(PanelPosition.BOTTOM))
                // Navigate to entry editor
                .addStep(WalkthroughStep
                        .tooltip(Localization.lang("Double click on the \"Ding_2006\" entry"))
                        .content(new TextBlock(Localization.lang("Let's start by selecting an entry to work with. Double click on the entry titled \"Chocolate and Prevention of Cardiovascular Disease: A Systematic Review\" by Ding et al.")))
                        .resolver(NodeResolver.selectorWithText(".table-row-cell",
                                text -> "Ding_2006".equals(text)
                                        || "Ding et al.".equals(text)
                                        || "Chocolate and Prevention of Cardiovascular Disease: A Systematic Review".equals(text)))
                        .trigger(Trigger.onDoubleClick())
                        .highlight(HighlightEffect.SPOT_LIGHT))
                .addStep(WalkthroughStep
                        .tooltip(Localization.lang("Click on the \"General\" tab"))
                        .content(new TextBlock(Localization.lang("Now we need to access the entry editor. Click on the \"General\" tab to view and edit the entry details.")))
                        .resolver(NodeResolver.selectorWithText(".tab", text -> Localization.lang("General").equals(text)))
                        .trigger(Trigger.onClick())
                        .highlight(HighlightEffect.SPOT_LIGHT))
                // Introduce PDF management area and demonstrate drag & drop
                .addStep(WalkthroughStep
                        .panel(Localization.lang("PDF file management area"))
                        .content(new TextBlock(Localization.lang("This is the PDF file management area where you can link files to your bibliography entries. Notice the three buttons on the right side—each offers a different way to add PDF files. Let's explore each method step by step.")))
                        .resolver(NodeResolver.predicate(LinkedFilesEditor.class::isInstance))
                        .position(PanelPosition.RIGHT)
                        .quitButtonPosition(QuitButtonPosition.BOTTOM_LEFT)
                        .highlight(HighlightEffect.SPOT_LIGHT)
                        .showQuitButton(false)
                        .continueButton(Localization.lang("Continue")))
                .addStep(WalkthroughStep
                        .panel(Localization.lang("Drag and drop files"))
                        .content(
                                new TextBlock(Localization.lang("You can drag and drop PDF files directly onto the file list area. This is often the quickest way to link files that are already on your computer.")),
                                new InfoBlock(Localization.lang("Try dragging a PDF file here to continue the walkthrough."))
                        )
                        .resolver(NodeResolver.predicate(LinkedFilesEditor.class::isInstance))
                        .trigger(Trigger.onFileAddedToListView())
                        .position(PanelPosition.RIGHT)
                        .highlight(HighlightEffect.SPOT_LIGHT)
                        .showQuitButton(false))
                // Method 1: Add file from computer
                .addStep(WalkthroughStep
                        .tooltip(Localization.lang("Click \"Add\" to link a file from your computer"))
                        .content(new TextBlock(Localization.lang("Click the \"Add\" button (first button with a plus icon) to link a PDF file that you already have on your computer. This will open a dialog where you can browse and select the file.")))
                        .resolver(NodeResolver.buttonWithGraphic(IconTheme.JabRefIcons.LINKED_FILE_ADD))
                        .trigger(Trigger.create().withWindowChangeListener().onClick().build())
                        .position(TooltipPosition.LEFT)
                        .highlight(HighlightEffect.SPOT_LIGHT))
                .addStep(WalkthroughStep
                        .tooltip(Localization.lang("Browse for your PDF file"))
                        .content(new TextBlock(Localization.lang("Use the \"Browse\" button to select a PDF file from your computer. Click the folder icon next to the \"Link\" field to open the file browser.")))
                        .resolver(NodeResolver.fxId("browse"))
                        .trigger(Trigger.create().withTimeout(Duration.INDEFINITE).onClick())
                        .activeWindow(WindowResolver.title(Localization.lang("Add file link")))
                        .showQuitButton(false)
                        .highlight(pdfDialogEffect))
                .addStep(WalkthroughStep
                        .tooltip(Localization.lang("Add a description for the file"))
                        .content(new TextBlock(Localization.lang("Enter a meaningful description for this file in the \"Description\" field. This helps you identify the file later.")))
                        .resolver(NodeResolver.fxId("description"))
                        .trigger(Trigger.onTextInput())
                        .activeWindow(WindowResolver.title(Localization.lang("Add file link")))
                        .showQuitButton(false)
                        .highlight(pdfDialogEffect))
                .addStep(WalkthroughStep
                        .tooltip(Localization.lang("Select the file type"))
                        .content(new TextBlock(Localization.lang("Choose the appropriate file type from the \"Filetype\" dropdown. Usually \"PDF\" is the correct choice for research papers.")))
                        .resolver(NodeResolver.fxId("fileType"))
                        .trigger(Trigger.onClick())
                        .activeWindow(WindowResolver.title(Localization.lang("Add file link")))
                        .showQuitButton(false)
                        .highlight(pdfDialogEffect))
                .addStep(WalkthroughStep
                        .tooltip(Localization.lang("Optionally add source URL"))
                        .content(new TextBlock(Localization.lang("If you downloaded this file from a website, you can add the source URL in the \"Source URL\" field. This is optional but helpful for tracking where you found the file.")))
                        .resolver(NodeResolver.fxId("sourceUrl"))
                        .trigger(Trigger.onTextInput())
                        .activeWindow(WindowResolver.title(Localization.lang("Add file link")))
                        .highlight(pdfDialogEffect)
                        .showQuitButton(false))
                .addStep(WalkthroughStep
                        .tooltip(Localization.lang("Complete the file linking"))
                        .content(new TextBlock(Localization.lang("Now click \"Add\" to complete the file linking process, or \"Cancel\" to try another method.")))
                        .resolver(NodeResolver.selectorWithText(".button", text -> Localization.lang("Add").equals(text)))
                        .trigger(Trigger.onClick())
                        .activeWindow(WindowResolver.title(Localization.lang("Add file link")))
                        .highlight(pdfDialogEffect)
                        .showQuitButton(false))
                // Method 2: Get fulltext automatically
                .addStep(WalkthroughStep
                        .tooltip(Localization.lang("Click \"Get fulltext\" to find PDFs automatically"))
                        .content(new TextBlock(Localization.lang("Click the \"Get fulltext\" button (second button with a download icon) to let JabRef automatically search for and download the PDF using online fetchers. This works when your entry has proper metadata like DOI or title.")))
                        .resolver(NodeResolver.buttonWithGraphic(IconTheme.JabRefIcons.FETCH_FULLTEXT))
                        .trigger(Trigger.onClick())
                        .position(TooltipPosition.LEFT)
                        .highlight(HighlightEffect.SPOT_LIGHT))
                .addStep(WalkthroughStep
                        .tooltip(Localization.lang("Wait for fulltext search to complete"))
                        .content(new TextBlock(Localization.lang("JabRef is now searching for the full text of this paper using various online sources. Please wait for the search to complete.")))
                        .resolver(NodeResolver.predicate(LinkedFilesEditor.class::isInstance))
                        .trigger(Trigger.onFetchFulltextCompleted())
                        .position(TooltipPosition.LEFT)
                        .highlight(HighlightEffect.SPOT_LIGHT))
                // Method 3: Download from URL
                .addStep(WalkthroughStep
                        .tooltip(Localization.lang("Click \"Download from URL\" to download from a web link"))
                        .content(new TextBlock(Localization.lang("Click the \"Download from URL\" button (third button with a download icon) to download a PDF directly from a web URL. JabRef will prompt you to enter the URL and then download the file automatically.")))
                        .resolver(NodeResolver.buttonWithGraphic(IconTheme.JabRefIcons.DOWNLOAD))
                        .trigger(Trigger.create().withWindowChangeListener().onClick().build())
                        .position(TooltipPosition.LEFT)
                        .highlight(HighlightEffect.SPOT_LIGHT))
                .addStep(WalkthroughStep
                        .tooltip(Localization.lang("Enter URL for download"))
                        .content(new TextBlock(Localization.lang("Enter the URL of the PDF file you want to download. You can try this example URL: %0", URLs.LINK_EXTERNAL_FILE_WALKTHROUGH_EXAMPLE_PDF)))
                        .resolver(NodeResolver.selector(".text-input"))
                        .trigger(Trigger.onTextInput())
                        .activeWindow(WindowResolver.not(stage))
                        .showQuitButton(false)
                        .highlight(pdfDialogEffect))
                .addStep(WalkthroughStep
                        .tooltip(Localization.lang("Confirm URL download"))
                        .content(new TextBlock(Localization.lang("Click \"OK\" to start downloading the PDF from the entered URL.")))
                        .resolver(NodeResolver.buttonType(ButtonType.OK))
                        .trigger(Trigger.onClick())
                        .activeWindow(WindowResolver.not(stage))
                        .highlight(pdfDialogEffect)
                        .showQuitButton(false))
                // Completion
                .addStep(WalkthroughStep
                        .panel(Localization.lang("PDF file linked successfully"))
                        .content(
                                new TextBlock(Localization.lang("Congratulations. You have successfully linked a PDF file to a bibliography entry. This makes it easy to access your research documents directly from JabRef. You can repeat this process for all your entries.")),
                                new InfoBlock(Localization.lang("For detailed information: [Adding PDFs](%0), [Managing files](%1), [Finding unlinked files](%2).", URLs.ADD_PDF_DOC, URLs.MANAGE_ASSOCIATED_FILES_DOC, URLs.FIND_UNLINKED_FILES_DOC))
                        )
                        .resolver(NodeResolver.predicate(LinkedFilesEditor.class::isInstance))
                        .continueButton(Localization.lang("Finish"))
                        .position(PanelPosition.RIGHT)
                        .quitButtonPosition(QuitButtonPosition.BOTTOM_LEFT)
                        .highlight(HighlightEffect.SPOT_LIGHT))
                .build();
    }

    private Walkthrough createGroupWalkthrough() {
        WindowResolver mainResolver = () -> Optional.of(stage);
        WalkthroughEffect groupHighlight = new WalkthroughEffect(
                new WindowEffect(HighlightEffect.PING),
                new WindowEffect(mainResolver, HighlightEffect.FULL_SCREEN_DARKEN)
        );
        String groupName = Localization.lang("Research");
        String addGroup = Localization.lang("Add group");
        String addSelectedEntries = Localization.lang("Add selected entries to this group");

        return Walkthrough
                .create(stateManager)
                // Step 1: Open example library
                .addStep(WalkthroughStep.sideEffect(Localization.lang("Open Example Library"))
                                        .sideEffect(new OpenLibrarySideEffect(frame)))
                // Step 2: Highlight groups sidepane
                .addStep(WalkthroughStep
                        .panel(Localization.lang("Welcome to groups walkthrough"))
                        .content(
                                new TextBlock(Localization.lang("This walkthrough will guide you through creating and managing groups in JabRef. Groups help you organize your bibliography entries into collections. We've opened an example library so you can practice with real entries.")),
                                new InfoBlock(Localization.lang("The groups panel on the left side shows all your groups in a tree structure. You can create groups, add entries to them, and organize them hierarchically."))
                        )
                        .resolver(NodeResolver.predicate(node -> node.getClass().getName().contains("GroupsSidePaneComponent")))
                        .continueButton(Localization.lang("Continue"))
                        .position(PanelPosition.RIGHT)
                        .highlight(HighlightEffect.SPOT_LIGHT))
                // Step 3: Click "Add group" button
                .addStep(WalkthroughStep
                        .tooltip(Localization.lang("Click on \"%0\" button", addGroup))
                        .content(new TextBlock(Localization.lang("Let's create your first group. Click the \"%0\" button at the bottom of the groups panel to open the group creation dialog.", addGroup)))
                        .resolver(NodeResolver.selectorWithText(".button", addGroup::equals))
                        .trigger(Trigger.create().withWindowChangeListener().onClick())
                        .position(TooltipPosition.TOP)
                        .highlight(HighlightEffect.SPOT_LIGHT))
                // Step 4: Fill group creation dialog
                .addStep(WalkthroughStep
                        .tooltip(Localization.lang("Enter group name"))
                        .content(new TextBlock(Localization.lang("Type \"%0\" as the name for your group.", groupName)))
                        .resolver(NodeResolver.fxId("nameField"))
                        .trigger(Trigger.onTextEquals(groupName))
                        .position(TooltipPosition.RIGHT)
                        .activeWindow(WindowResolver.title(Localization.lang("Add group")))
                        .showQuitButton(false)
                        .highlight(groupHighlight))
                .addStep(WalkthroughStep
                        .tooltip(Localization.lang("Add a description (optional)"))
                        .content(new TextBlock(Localization.lang("You can add a description to help remember what this group is for. For example, type \"Important research papers for my project\".")))
                        .resolver(NodeResolver.fxId("descriptionField"))
                        .trigger(Trigger.onTextInput())
                        .position(TooltipPosition.RIGHT)
                        .continueButton(Localization.lang("Continue"))
                        .activeWindow(WindowResolver.title(Localization.lang("Add group")))
                        .showQuitButton(false)
                        .highlight(groupHighlight))
                .addStep(WalkthroughStep
                        .tooltip(Localization.lang("Select \"Explicit selection\""))
                        .content(new TextBlock(Localization.lang("This allows you to manually choose which entries belong to this group.")))
                        .resolver(NodeResolver.fxId("explicitRadioButton"))
                        .trigger(Trigger.onClick())
                        .position(TooltipPosition.RIGHT)
                        .activeWindow(WindowResolver.title(Localization.lang("Add group")))
                        .showQuitButton(false)
                        .highlight(groupHighlight))
                .addStep(WalkthroughStep
                        .tooltip(Localization.lang("Click \"OK\" to create the group"))
                        .content(new TextBlock(Localization.lang("Now click \"OK\" to create your new group.")))
                        .resolver(NodeResolver.buttonType(ButtonType.OK))
                        .trigger(Trigger.onClick())
                        .activeWindow(WindowResolver.title(Localization.lang("Add group")))
                        .showQuitButton(false)
                        .highlight(groupHighlight))
                .addStep(WalkthroughStep
                        .tooltip(Localization.lang("Click on \"All entries\" group"))
                        .content(new TextBlock(Localization.lang("Select \"All entries\" to show all entries in the main table before dragging items to your new group.")))
                        .resolver(NodeResolver.selectorWithText(
                                ".tree-table-row-cell",
                                text -> Localization.lang("All entries").equals(text)
                                        || (text != null && text.contains(Localization.lang("All entries")))))
                        .trigger(Trigger.onClick())
                        .position(TooltipPosition.RIGHT)
                        .highlight(HighlightEffect.SPOT_LIGHT))
                // Step 5: Drag and drop entry to group
                .addStep(WalkthroughStep
                        .panel(Localization.lang("Add entries to your group"))
                        .content(
                                new TextBlock(Localization.lang("Your \"%0\" group has been created. Now let's add some entries to it.", groupName)),
                                new InfoBlock(Localization.lang("You can add entries to a group by dragging and dropping them. Try selecting the \"Ding_2006\" entry from the main table and drag it to your new group."))
                        )
                        .resolver(NodeResolver.selectorWithText(".table-row-cell",
                                text -> "Ding_2006".equals(text)
                                        || "Ding et al.".equals(text)
                                        || "Chocolate and Prevention of Cardiovascular Disease: A Systematic Review".equals(text)))
                        .continueButton(Localization.lang("Continue"))
                        .position(PanelPosition.RIGHT)
                        .highlight(HighlightEffect.PING))
                // Step 6: Right-click to add entry
                .addStep(WalkthroughStep
                        .tooltip(Localization.lang("Select the Corti_2009 entry"))
                        .content(new TextBlock(Localization.lang("You can also add entries using the context menu. First, select the \"Corti_2009\" entry (Cocoa and Cardiovascular Health by Corti et al.) from the main table.")))
                        .resolver(NodeResolver.selectorWithText(".table-row-cell",
                                text -> "Corti_2009".equals(text)
                                        || "Corti et al.".equals(text)
                                        || "Cocoa and Cardiovascular Health".equals(text)))
                        .trigger(Trigger.onClick())
                        .position(TooltipPosition.RIGHT)
                        .highlight(HighlightEffect.SPOT_LIGHT))
                .addStep(WalkthroughStep
                        .tooltip(Localization.lang("Right-click on your group"))
                        .content(new TextBlock(Localization.lang("Now right-click on your \"%0\" group to open the context menu.", groupName)))
                        .resolver(NodeResolver.selectorWithText(".tree-table-row-cell",
                                text -> text != null && text.contains(groupName)))
                        .trigger(Trigger.create().withWindowChangeListener().onRightClick())
                        .position(TooltipPosition.RIGHT)
                        .highlight(HighlightEffect.SPOT_LIGHT))
                .addStep(WalkthroughStep
                        .tooltip(Localization.lang("Click \"%0\"", addSelectedEntries))
                        .content(new TextBlock(Localization.lang("Click on \"%0\" to add the Corti_2009 entry to your group.", addSelectedEntries)))
                        .resolver(NodeResolver.menuItem(addSelectedEntries))
                        .trigger(Trigger.onClick())
                        .activeWindow(WindowResolver.clazz(ContextMenu.class))
                        .showQuitButton(false)
                        .highlight(groupHighlight))
                // Completion
                .addStep(WalkthroughStep
                        .panel(Localization.lang("Groups walkthrough completed"))
                        .content(
                                new TextBlock(Localization.lang("You've learned how to create groups and add entries to them. Groups are a powerful way to organize your bibliography and can be nested to create hierarchical structures.")),
                                new InfoBlock(Localization.lang("For more information about groups: [Groups documentation](%0)", URLs.GROUPS_DOC))
                        )
                        .resolver(NodeResolver.predicate(node -> node.getClass().getName().contains("GroupsSidePaneComponent")))
                        .continueButton(Localization.lang("Finish"))
                        .position(PanelPosition.RIGHT)
                        .highlight(HighlightEffect.SPOT_LIGHT))
                .build();
    }

    private Walkthrough createSearchWalkthrough() {
        NodeResolver searchFieldResolver = scene -> NodeResolver
                .predicate(GlobalSearchBar.class::isInstance)
                .resolve(scene)
                .flatMap(node -> node instanceof GlobalSearchBar bar ?
                                 bar.getChildren().stream().filter(CustomTextField.class::isInstance).findAny() :
                                 Optional.empty());

        return Walkthrough
                .create(stateManager)
                // Step 1: Open example library
                .addStep(WalkthroughStep.sideEffect(Localization.lang("Open Example Library"))
                                        .sideEffect(new OpenLibrarySideEffect(frame, "SearchExamples.bib")))
                // Step 1b: Ensure initial search settings
                .addStep(WalkthroughStep.sideEffect(Localization.lang("Prepare search settings"))
                                        .sideEffect(new EnsureSearchSettingsSideEffect(preferences.getSearchPreferences())))
                // Step 2: Introduction
                .addStep(WalkthroughStep
                        .panel(Localization.lang("Welcome to the search walkthrough"))
                        .content(
                                new TextBlock(Localization.lang("This walkthrough will guide you through JabRef's search capabilities. We've loaded a sample library to demonstrate various search techniques."))
                        )
                        .resolver(NodeResolver.predicate(node -> node.getClass().getName().contains("MainTable")))
                        .continueButton(Localization.lang("Continue"))
                        .position(PanelPosition.BOTTOM)
                        .quitButtonPosition(QuitButtonPosition.BOTTOM_LEFT)
                        .highlight(HighlightEffect.SPOT_LIGHT))
                // Step 3: Focus on GlobalSearchBar
                .addStep(WalkthroughStep
                        .tooltip(Localization.lang("Click in the search field to begin"))
                        .content(new InfoBlock(Localization.lang("You can also use the shortcut for focusing the search field (%0).",
                                preferences.getKeyBindingRepository().getKeyCombination(KeyBinding.SEARCH).map(KeyCombination::getDisplayText).orElse(""))))
                        .resolver(searchFieldResolver)
                        .trigger(Trigger.onClick())
                        .position(TooltipPosition.BOTTOM)
                        .highlight(HighlightEffect.SPOT_LIGHT))
                // Step 4-7: Simple text search
                .addStep(WalkthroughStep
                        .tooltip(Localization.lang("Type `machine learning`"))
                        .content(new TextBlock(Localization.lang("As you type, JabRef instantly dims all non-matching entries. By default, searches are case-insensitive and look through all fields like title, author, abstract, and keywords.")))
                        .resolver(searchFieldResolver)
                        .trigger(Trigger.onTextEquals("machine learning"))
                        .position(TooltipPosition.BOTTOM)
                        .highlight(HighlightEffect.SPOT_LIGHT))
                .addStep(WalkthroughStep
                        .panel(Localization.lang("Understanding search results"))
                        .content(
                                new TextBlock(Localization.lang("Notice how entries not containing \"machine learning\" are dimmed.")),
                                new InfoBlock(Localization.lang("This found entries with at least a field in their metadata (*e.g.,* title, author, abstract, *etc.*) containing \"machine learning\"."))
                        )
                        .resolver(NodeResolver.predicate(node -> node.getClass().getName().contains("MainTable")))
                        .continueButton(Localization.lang("Continue"))
                        .position(PanelPosition.RIGHT)
                        .quitButtonPosition(QuitButtonPosition.BOTTOM_LEFT)
                        .highlight(HighlightEffect.NONE))
                .addStep(WalkthroughStep
                        .tooltip(Localization.lang("Toggle filter mode to hide non-matching entries"))
                        .content(new TextBlock(Localization.lang("Filter mode shows only matching entries, hiding everything else.")))
                        .resolver(NodeResolver.buttonWithGraphic(IconTheme.JabRefIcons.FILTER))
                        .trigger(Trigger.onClick())
                        .position(TooltipPosition.BOTTOM)
                        .highlight(HighlightEffect.SPOT_LIGHT))
                .addStep(WalkthroughStep
                        .tooltip(Localization.lang("Clear the search to see all entries again"))
                        .resolver(searchFieldResolver)
                        .trigger(Trigger.onTextEquals(""))
                        .position(TooltipPosition.BOTTOM)
                        .highlight(HighlightEffect.PING))
                // Step 8: Field-specific search
                .addStep(WalkthroughStep
                        .tooltip(Localization.lang("Type `author = Son`"))
                        .content(
                                new TextBlock(Localization.lang("The equals sign (`=`) means \"contains\" and is case-insensitive by default.")),
                                new InfoBlock(Localization.lang("This query finds all papers where any author's name contains \"Son,\" including \"Maddison,\" \"Watson, J.,\" or \"Matson, Robert P.\". It won't match \"Son\" appearing in titles or abstracts."))
                        )
                        .resolver(searchFieldResolver)
                        .trigger(Trigger.onTextMatchesRegex("(?i)\\s*author\\s*=\\s*son\\s*"))
                        .position(TooltipPosition.BOTTOM)
                        .highlight(HighlightEffect.PING))
                // Step 9: Exact field matching
                .addStep(WalkthroughStep
                        .tooltip(Localization.lang("Type `date == 2023`"))
                        .content(new TextBlock(Localization.lang("The double equals (`==`) means exact match, not just contains. This query finds only papers with field \"date\" exactly in 2023.")),
                                new InfoBlock(Localization.lang("With single equals (*e.g.,* `date = 23`), you'd also match dates like 1923 or 2230.")))
                        .resolver(searchFieldResolver)
                        .trigger(Trigger.onTextMatchesRegex("(?i)\\s*date\\s*==\\s*2023\\s*"))
                        .position(TooltipPosition.BOTTOM)
                        .highlight(HighlightEffect.PING))
                // Step 10: Combining search terms with AND
                .addStep(WalkthroughStep
                        .tooltip(Localization.lang("Type `shortauthor = WHO AND date == 2023`"))
                        .content(new TextBlock(Localization.lang("This query finds papers by WHO from 2023. `AND` requires both conditions to be true.")))
                        .resolver(searchFieldResolver)
                        .trigger(Trigger.onTextMatchesRegex("(?i)shortauthor\\s*=\\s*WHO\\s+AND\\s+date\\s*==\\s*2023|date\\s*==\\s*2023\\s+AND\\s+shortauthor\\s*=\\s*WHO"))
                        .position(TooltipPosition.BOTTOM)
                        .highlight(HighlightEffect.PING))
                // Step 11: Using OR for alternatives
                .addStep(WalkthroughStep
                        .tooltip(Localization.lang("Type `title = covid OR title = pandemic`"))
                        .content(new TextBlock(Localization.lang("This query finds paper with title containing either COVID or pandemics in general.")),
                                new InfoBlock(Localization.lang("`OR` can be satisified with either of the conditions being true.")))
                        .resolver(searchFieldResolver)
                        .trigger(Trigger.onTextMatchesRegex("(?i)title\\s*=\\s*covid\\s+OR\\s+title\\s*=\\s*pandemic|title\\s*=\\s*pandemic\\s+OR\\s+title\\s*=\\s*covid"))
                        .position(TooltipPosition.BOTTOM)
                        .highlight(HighlightEffect.PING))
                // Step 12: Negation with NOT
                .addStep(WalkthroughStep
                        .tooltip(Localization.lang("Type `deep learning NOT title = survey`"))
                        .content(new TextBlock(Localization.lang("This query finds entries with \"deep learning\" in its metadata, but not \"survey\" in the title.")),
                                new InfoBlock(Localization.lang("NOT helps you filter out unwanted results.")))
                        .resolver(searchFieldResolver)
                        .trigger(Trigger.onTextMatchesRegex("(?i)deep\\s+learning\\s+NOT\\s+title\\s*=\\s*survey"))
                        .position(TooltipPosition.BOTTOM)
                        .highlight(HighlightEffect.PING))
                // Step 13: Regex
                .addStep(WalkthroughStep
                        .tooltip(Localization.lang("Enable regular expressions for pattern matching"))
                        .content(new TextBlock(Localization.lang("Regular expressions allow sophisticated pattern matching. Click this button to enable regex mode for advanced searches.")))
                        .resolver(NodeResolver.buttonWithGraphic(IconTheme.JabRefIcons.REG_EX))
                        .trigger(Trigger.onClick())
                        .position(TooltipPosition.BOTTOM)
                        .highlight(HighlightEffect.SPOT_LIGHT))
                // Step 14: Using regex patterns
                .addStep(WalkthroughStep
                        .tooltip(Localization.lang("Type `title =~ \\(deep|machine\\) learning`"))
                        .content(new TextBlock(Localization.lang("The `=~` operator explicitly uses regex. The pipe operator `|` allows `(deep|machine)` matches either word (deep or machine). This finds \"deep learning\" and \"machine learning\" but not \"learning\" alone.")),
                                new InfoBlock(Localization.lang("Click on the regex button on previous step enables regex mode, allowing you to type `\\(deep|machine\\) learning` directly to search for any entry with metadata matching the regex. If you use `=~`, regardless of whether regex mode is enabled, it will always treat the search as a regex.")))
                        .resolver(searchFieldResolver)
                        .trigger(Trigger.onTextMatchesRegex("(?i)title\\s*=~\\s*\\\\\\(deep\\|machine\\\\\\)\\s*learning"))
                        .position(TooltipPosition.BOTTOM)
                        .highlight(HighlightEffect.PING))
                // Step 15-16: Case sensitivity
                .addStep(WalkthroughStep
                        .tooltip(Localization.lang("Enable case-sensitive searching"))
                        .content(new TextBlock(Localization.lang("This is useful when case matters, like distinguishing \"WHO\" (World Health Organization) from \"who\".")))
                        .resolver(NodeResolver.buttonWithGraphic(IconTheme.JabRefIcons.CASE_SENSITIVE))
                        .trigger(Trigger.onClick())
                        .position(TooltipPosition.BOTTOM)
                        .highlight(HighlightEffect.SPOT_LIGHT))
                .addStep(WalkthroughStep
                        .tooltip(Localization.lang("Type `WHO`"))
                        .content(new TextBlock(Localization.lang("This query finds entries with capital WHO only. With case sensitivity on, this won't match \"who\" or \"Who\".")))
                        .resolver(searchFieldResolver)
                        .trigger(Trigger.onTextEquals("WHO"))
                        .position(TooltipPosition.BOTTOM)
                        .highlight(HighlightEffect.PING))
                // Step 17: Completion
                .addStep(WalkthroughStep
                        .panel(Localization.lang("Search walkthrough completed"))
                        .content(new TextBlock(Localization.lang("**Quick reference:**\n- Press %0 to jump to search\n- Use **field = value** for field searches\n- Combine with **AND**, **OR**, **NOT**\n- Enable **regex** for pattern matching",
                                        preferences.getKeyBindingRepository().getKeyCombination(KeyBinding.SEARCH).map(KeyCombination::getDisplayText).orElse(""))),
                                new InfoBlock(Localization.lang("For complete search documentation and more examples, visit [Search documentation](%0)", URLs.SEARCH_WITH_IN_LIBRARY_DOC)))
                        .continueButton(Localization.lang("Finish"))
                        .position(PanelPosition.RIGHT)
                        .quitButtonPosition(QuitButtonPosition.BOTTOM_LEFT))
                .build();
    }

    private Walkthrough createMainFileDirectoryWalkthrough() {
        WindowResolver mainResolver = () -> Optional.of(stage);
        WalkthroughEffect preferenceHighlight = new WalkthroughEffect(
                new WindowEffect(HighlightEffect.SPOT_LIGHT),
                new WindowEffect(mainResolver, HighlightEffect.FULL_SCREEN_DARKEN)
        );

        return Walkthrough
                .create(stateManager)
                // Navigate to preferences dialog
                .addStep(WalkthroughStep
                        .tooltip(Localization.lang("Click on \"File\" menu"))
                        .resolver(NodeResolver.selector(".menu-bar .menu-button:first-child"))
                        .trigger(Trigger.onClick())
                        .position(TooltipPosition.BOTTOM)
                        .highlight(HighlightEffect.SPOT_LIGHT)
                )
                .addStep(WalkthroughStep
                        .tooltip(Localization.lang("Click on \"Preferences\""))
                        .resolver(NodeResolver.menuItem("Preferences"))
                        .trigger(Trigger.create().withWindowChangeListener().onClick())
                        .position(TooltipPosition.RIGHT)
                        .activeWindow(WindowResolver.clazz(ContextMenu.class))
                        .highlight(new WalkthroughEffect(
                                new WindowEffect(HighlightEffect.PING),
                                new WindowEffect(mainResolver, HighlightEffect.FULL_SCREEN_DARKEN)
                        ))
                        .showQuitButton(false)
                )
                // Configure main file directory settings
                .addStep(WalkthroughStep
                        .tooltip(Localization.lang("Select the \"Linked files\" tab"))
                        .content(new TextBlock(Localization.lang("This section manages how JabRef handles your PDF files and other documents.")))
                        .resolver(NodeResolver.selectorWithText(".list-cell", text -> Localization.lang("Linked files").equals(text)))
                        .trigger(Trigger.onClick())
                        .position(TooltipPosition.AUTO)
                        .quitButtonPosition(QuitButtonPosition.BOTTOM_LEFT)
                        .activeWindow(WindowResolver.title(PreferencesDialogView.DIALOG_TITLE))
                        .highlight(preferenceHighlight)
                )
                .addStep(WalkthroughStep
                        .tooltip(Localization.lang("Enable \"Main file directory\" option"))
                        .content(new TextBlock(Localization.lang("Choose this option to tell JabRef where your research files are stored. This makes it easy to attach PDFs and other documents to your bibliography entries. You can browse to select your preferred folder in the next step.")))
                        .resolver(NodeResolver.fxId("useMainFileDirectory"))
                        .trigger(Trigger.onClick())
                        .position(TooltipPosition.AUTO)
                        .highlight(preferenceHighlight)
                        .quitButtonPosition(QuitButtonPosition.BOTTOM_LEFT)
                        .activeWindow(WindowResolver.title(PreferencesDialogView.DIALOG_TITLE))
                )
                .addStep(WalkthroughStep
                        .tooltip(Localization.lang("Specify \"Main file directory\" option"))
                        .resolver(scene -> Optional.ofNullable(scene.lookup("#useMainFileDirectory").getParent()))
                        .position(TooltipPosition.BOTTOM)
                        .highlight(preferenceHighlight)
                        .quitButtonPosition(QuitButtonPosition.BOTTOM_LEFT)
                        .activeWindow(WindowResolver.title(PreferencesDialogView.DIALOG_TITLE))
                        .continueButton(Localization.lang("Continue"))
                )
                // Complete configuration
                .addStep(WalkthroughStep
                        .panel(Localization.lang("Click \"Save\" to save changes"))
                        .content(
                                new TextBlock(Localization.lang("Congratulations. Your main file directory is now configured. JabRef will use this location to automatically find and organize your research documents.")),
                                new InfoBlock(Localization.lang("Additional information on main file directory can be found in [Manage Associated Files Documentation](%0)", URLs.MANAGE_ASSOCIATED_FILES_DOC))
                        )
                        .resolver(NodeResolver.selectorWithText(".button", text -> Localization.lang("Save").equals(text)))
                        .trigger(Trigger.onClick())
                        .position(PanelPosition.TOP)
                        .quitButtonPosition(QuitButtonPosition.BOTTOM_LEFT)
                        .highlight(preferenceHighlight)
                        .activeWindow(WindowResolver.title(PreferencesDialogView.DIALOG_TITLE))
                )
                .build();
    }
}
