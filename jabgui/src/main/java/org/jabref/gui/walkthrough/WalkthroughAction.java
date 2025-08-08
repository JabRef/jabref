package org.jabref.gui.walkthrough;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import javafx.scene.control.ButtonType;
import javafx.scene.control.ContextMenu;
import javafx.stage.Stage;
import javafx.util.Duration;

import org.jabref.gui.LibraryTabContainer;
import org.jabref.gui.StateManager;
import org.jabref.gui.actions.SimpleCommand;
import org.jabref.gui.fieldeditors.LinkedFilesEditor;
import org.jabref.gui.icon.IconTheme;
import org.jabref.gui.maintable.MainTable;
import org.jabref.gui.preferences.PreferencesDialogView;
import org.jabref.gui.walkthrough.declarative.NodeResolver;
import org.jabref.gui.walkthrough.declarative.Trigger;
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
    public static final String CUSTOMIZE_ENTRY_TABLE_WALKTHROUGH_NAME = "customizeEntryTable";
    public static final String GROUP_WALKTHROUGH_NAME = "group";

    private static final Map<String, Walkthrough> WALKTHROUGH_CACHE = new ConcurrentHashMap<>();

    private final Walkthrough walkthrough;
    private final LibraryTabContainer frame;
    private final StateManager stateManager;
    private final Stage stage;

    public WalkthroughAction(Stage stage, LibraryTabContainer frame, StateManager stateManager, String name) {
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
                    case CUSTOMIZE_ENTRY_TABLE_WALKTHROUGH_NAME ->
                            createCustomizeEntryTableWalkthrough();
                    case GROUP_WALKTHROUGH_NAME -> createGroupWalkthrough();
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
                                new InfoBlock(Localization.lang("You can find more information about customizing JabRef at [documentation](https://docs.jabref.org/advanced/main-window)"))
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
                        .content(new TextBlock(Localization.lang("Enter the URL of the PDF file you want to download. You can try this example URL: https://nutritionandmetabolism.biomedcentral.com/counter/pdf/10.1186/1743-7075-3-2.pdf")))
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
                                new InfoBlock(Localization.lang("For detailed information: [Adding PDFs](https://docs.jabref.org/collect/add-pdfs-to-an-entry), [Managing files](https://docs.jabref.org/finding-sorting-and-cleaning-entries/filelinks), [Finding unlinked files](https://docs.jabref.org/collect/findunlinkedfiles)."))
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
        String groupName = Localization.lang("Research Papers");
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
                                new InfoBlock(Localization.lang("For more information about groups: [Groups documentation](https://docs.jabref.org/finding-sorting-and-cleaning-entries/groups)"))
                        )
                        .resolver(NodeResolver.predicate(node -> node.getClass().getName().contains("GroupsSidePaneComponent")))
                        .continueButton(Localization.lang("Finish"))
                        .position(PanelPosition.RIGHT)
                        .highlight(HighlightEffect.SPOT_LIGHT))
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
                                new InfoBlock(Localization.lang("Additional information on main file directory can be found in [help](https://docs.jabref.org/v5/finding-sorting-and-cleaning-entries/filelinks)"))
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
