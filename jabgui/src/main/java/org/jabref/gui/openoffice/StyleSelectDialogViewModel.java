package org.jabref.gui.openoffice;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.function.BooleanSupplier;
import java.util.function.Function;
import java.util.regex.Pattern;

import javafx.beans.property.ListProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.scene.control.Tab;

import org.jabref.gui.DialogService;
import org.jabref.gui.desktop.os.NativeDesktop;
import org.jabref.gui.externalfiletype.ExternalFileType;
import org.jabref.gui.externalfiletype.ExternalFileTypes;
import org.jabref.gui.frame.ExternalApplicationsPreferences;
import org.jabref.gui.preferences.GuiPreferences;
import org.jabref.gui.util.FileDialogConfiguration;
import org.jabref.logic.FilePreferences;
import org.jabref.logic.citationstyle.CSLStyleLoader;
import org.jabref.logic.citationstyle.CSLStyleUtils;
import org.jabref.logic.citationstyle.CitationStyle;
import org.jabref.logic.journals.JournalAbbreviationRepository;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.openoffice.OpenOfficePreferences;
import org.jabref.logic.openoffice.style.BstCitationFormat;
import org.jabref.logic.openoffice.style.BstStyle;
import org.jabref.logic.openoffice.style.BstStyleLoader;
import org.jabref.logic.openoffice.style.JStyle;
import org.jabref.logic.openoffice.style.JStyleLoader;
import org.jabref.logic.openoffice.style.OOStyle;
import org.jabref.logic.preview.CitationStylePreviewLayout;
import org.jabref.logic.util.BackgroundTask;
import org.jabref.logic.util.StandardFileType;
import org.jabref.logic.util.TaskExecutor;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntryTypesManager;

import org.jspecify.annotations.Nullable;

public class StyleSelectDialogViewModel {

    private static final Pattern STYLE_SEARCH_SEPARATORS = Pattern.compile("[\\p{P}\\s]+");

    private final DialogService dialogService;

    private final CSLStyleLoader cslStyleLoader;
    private final JStyleLoader jStyleLoader;
    private final BstStyleLoader bstStyleLoader;

    private final ExternalApplicationsPreferences externalApplicationsPreferences;
    private final FilePreferences filePreferences;
    private final OpenOfficePreferences openOfficePreferences;

    private final BibEntryTypesManager bibEntryTypesManager;

    private final ObjectProperty<Tab> selectedTab = new SimpleObjectProperty<>();

    private final ListProperty<JStyleSelectViewModel> jStyles = new SimpleListProperty<>(FXCollections.observableArrayList());
    private final ObjectProperty<@Nullable JStyleSelectViewModel> selectedJStyle = new SimpleObjectProperty<>();

    private final ListProperty<BstStyleSelectViewModel> bstStyles = new SimpleListProperty<>(FXCollections.observableArrayList());
    private final ObjectProperty<@Nullable BstStyleSelectViewModel> selectedBstStyle = new SimpleObjectProperty<>();
    private final ObjectProperty<BstCitationFormat> bstCitationFormat = new SimpleObjectProperty<>(BstCitationFormat.NUMERIC);

    private final ObservableList<CitationStylePreviewLayout> availableCslLayouts = FXCollections.observableArrayList();
    private final ObjectProperty<@Nullable CitationStylePreviewLayout> selectedCslLayoutProperty = new SimpleObjectProperty<>();
    private final FilteredList<CitationStylePreviewLayout> filteredAvailableCslLayouts = new FilteredList<>(availableCslLayouts);

    public StyleSelectDialogViewModel(DialogService dialogService,
                                      CSLStyleLoader cslStyleLoader,
                                      JStyleLoader jStyleLoader,
                                      BstStyleLoader bstStyleLoader,
                                      GuiPreferences preferences,
                                      JournalAbbreviationRepository journalAbbreviationRepository,
                                      TaskExecutor taskExecutor,
                                      BibEntryTypesManager bibEntryTypesManager) {
        this.dialogService = dialogService;

        this.cslStyleLoader = cslStyleLoader;
        this.jStyleLoader = jStyleLoader;
        this.bstStyleLoader = bstStyleLoader;

        this.externalApplicationsPreferences = preferences.getExternalApplicationsPreferences();
        this.filePreferences = preferences.getFilePreferences();
        this.openOfficePreferences = preferences.getOpenOfficePreferences(journalAbbreviationRepository);

        this.bibEntryTypesManager = bibEntryTypesManager;

        jStyles.addAll(loadJStyles());
        bstStyles.addAll(loadBstStyles());

        bstCitationFormat.set(openOfficePreferences.getBstCitationFormat());

        OOStyle currentStyle = openOfficePreferences.getCurrentStyle();

        jStyles.stream().filter(viewModel -> viewModel.getJStyle().equals(currentStyle)).findFirst()
               .or(() -> jStyles.stream().findFirst()).ifPresent(selectedJStyle::setValue);
        bstStyles.stream().filter(viewModel -> viewModel.getBstStyle().equals(currentStyle)).findFirst()
                 .or(() -> bstStyles.stream().findFirst()).ifPresent(selectedBstStyle::setValue);

        BackgroundTask.wrap(CSLStyleLoader::getStyles)
                      .onSuccess(styles -> {
                          List<CitationStylePreviewLayout> layouts = styles.stream()
                                                                           .map(style -> new CitationStylePreviewLayout(style, bibEntryTypesManager))
                                                                           .toList();
                          availableCslLayouts.setAll(layouts);

                          availableCslLayouts.stream().filter(layout -> layout.citationStyle().equals(currentStyle)).findFirst()
                                             .or(() -> availableCslLayouts.stream().findFirst()).ifPresent(selectedCslLayoutProperty::set);
                      })
                      .onFailure(ex -> dialogService.showErrorDialogAndWait("Error discovering citation styles", ex))
                      .executeWith(taskExecutor);
    }

    // region - general methods

    public Tab getSelectedTab() {
        return selectedTab.get();
    }

    public void setSelectedTab(Tab tab) {
        if (tab != null) {
            selectedTab.set(tab);
        }
    }

    public OOStyle getSetStyle() {
        return openOfficePreferences.getCurrentStyle();
    }

    public OOStyle getSelectedStyle() {
        Tab currentTab = getSelectedTab();
        if (currentTab != null) {
            String tabText = currentTab.getText();
            if ("JStyles".equals(tabText)) {
                if (selectedJStyle.get() != null) {
                    return selectedJStyle.get().getJStyle();
                }
            } else if ("CSL Styles".equals(tabText)) {
                if (selectedCslLayoutProperty.get() != null) {
                    return selectedCslLayoutProperty.get().citationStyle();
                }
            } else if ("BST Styles".equals(tabText)) {
                if (selectedBstStyle.get() != null) {
                    return selectedBstStyle.get().getBstStyle();
                }
            }
        }
        return openOfficePreferences.getCurrentStyle();
    }

    public void storeStylePreferences() {
        // save external jstyles
        List<String> externalJStyles = jStyles.stream()
                                              .map(this::toJStyle)
                                              .filter(style -> !style.isInternalStyle())
                                              .map(JStyle::getPath)
                                              .toList();

        openOfficePreferences.setExternalJStyles(externalJStyles);

        // save external bst styles
        List<String> externalBstStyles = bstStyles.stream()
                                                  .map(vm -> vm.getBstStyle())
                                                  .filter(style -> !style.isInternalStyle())
                                                  .map(BstStyle::getPath)
                                                  .toList();
        openOfficePreferences.setExternalBstStyles(externalBstStyles);

        // save bst citation format
        openOfficePreferences.setBstCitationFormat(bstCitationFormat.get());

        openOfficePreferences.setCurrentStyle(getSelectedStyle());
    }

    private Optional<Path> selectStyleFile(StandardFileType fileType) {
        String description = Localization.lang("%0 file", fileType.getName());
        FileDialogConfiguration fileDialogConfiguration = new FileDialogConfiguration.Builder()
                .addExtensionFilter(description, fileType)
                .withDefaultExtension(description, fileType)
                .withInitialDirectory(filePreferences.getWorkingDirectory())
                .build();
        return dialogService.showFileOpenDialog(fileDialogConfiguration).map(Path::toAbsolutePath);
    }

    private <T> void deleteStyle(OOStyle style, BooleanSupplier removeFromLoader, ObservableList<T> rows,
                                 ObjectProperty<@Nullable T> selection, Function<T, OOStyle> toStyle) {
        if (style.isInternalStyle() || rows.stream().noneMatch(row -> style.equals(toStyle.apply(row)))) {
            return;
        }

        boolean deleteConfirmed = dialogService.showConfirmationDialogAndWait(
                Localization.lang("Delete style"),
                Localization.lang("Are you sure you want to delete the style '%0'?", style.getName()),
                Localization.lang("Delete"),
                Localization.lang("Cancel"));
        if (!deleteConfirmed) {
            return;
        }

        if (!removeFromLoader.getAsBoolean()) {
            dialogService.showErrorDialogAndWait(
                    Localization.lang("Cannot delete style"),
                    Localization.lang("Could not delete style. It might be an internal style that cannot be removed."));
            return;
        }

        Optional<T> replacement = rows.stream().filter(row -> !style.equals(toStyle.apply(row))).findFirst();
        @Nullable T replacementRow = replacement.orElse(null);
        // Change selection before removing rows so table listeners cannot restore the deleted row.
        Optional.ofNullable(selection.get())
                .filter(row -> style.equals(toStyle.apply(row)))
                .ifPresent(_ -> selection.set(replacementRow));
        rows.removeIf(row -> style.equals(toStyle.apply(row)));

        OOStyle currentStyle = openOfficePreferences.getCurrentStyle();
        if (style.getClass().equals(currentStyle.getClass()) && style.getPath().equals(currentStyle.getPath())) {
            openOfficePreferences.setCurrentStyle(replacement.map(toStyle).orElseGet(CSLStyleLoader::getDefaultStyle));
        }
    }

    // endregion

    // region - csl-specific methods

    public ObservableList<CitationStylePreviewLayout> getAvailableCslLayouts() {
        return filteredAvailableCslLayouts;
    }

    public ObjectProperty<@Nullable CitationStylePreviewLayout> selectedCslLayoutProperty() {
        return selectedCslLayoutProperty;
    }

    public void setAvailableCslLayoutsFilter(String searchTerm) {
        filteredAvailableCslLayouts.setPredicate(layout -> matchStyleSearch(layout.getDisplayName(), searchTerm));
    }

    public static boolean matchStyleSearch(String styleName, String searchTerm) {
        // [impl->req~ux.text-filtering.case-insensitive-separators~1]
        List<String> searchTerms = STYLE_SEARCH_SEPARATORS.splitAsStream(searchTerm.toLowerCase(Locale.ROOT))
                                                          .filter(term -> !term.isBlank())
                                                          .toList();

        if (searchTerms.isEmpty()) {
            return true;
        }

        String searchableStyleName = STYLE_SEARCH_SEPARATORS.matcher(styleName.toLowerCase(Locale.ROOT))
                                                            .replaceAll(" ")
                                                            .trim();
        return searchTerms.stream().allMatch(searchableStyleName::contains);
    }

    /// Handles importing a custom CSL style file
    public void addCslStyleFile() {
        selectStyleFile(StandardFileType.CITATION_STYLE).map(Path::toString).ifPresent(stylePath -> {
            // Create a citation style
            Optional<CitationStyle> citationStyleToAddOptional = CSLStyleUtils.createCitationStyleFromFile(stylePath);

            // Check if citation style is valid
            if (citationStyleToAddOptional.isEmpty()) {
                dialogService.showErrorDialogAndWait(
                        Localization.lang("Invalid style selected"),
                        Localization.lang("You must select a valid CSL style file.")
                );
                return;
            }

            CitationStyle citationStyleToAdd = citationStyleToAddOptional.get();

            // Check if citation style is duplicate in the list
            if (isDuplicate(citationStyleToAdd)) {
                dialogService.showInformationDialogAndWait(
                        Localization.lang("Style already available"),
                        Localization.lang("The selected CSL style is already available in the list.")
                );
                return;
            }

            // Citation style is good to add
            cslStyleLoader.addExternalStyle(citationStyleToAdd);

            List<CitationStyle> allStyles = CSLStyleLoader.getStyles();
            List<CitationStylePreviewLayout> updatedLayouts = allStyles.stream()
                                                                       .map(style -> new CitationStylePreviewLayout(style, bibEntryTypesManager))
                                                                       .toList();

            Optional<CitationStylePreviewLayout> newLayoutOptional = updatedLayouts.stream()
                                                                                   .filter(layout -> layout.getFilePath().equals(stylePath))
                                                                                   .findFirst();

            if (newLayoutOptional.isPresent()) {
                CitationStylePreviewLayout newLayout = newLayoutOptional.get();
                selectedCslLayoutProperty.set(newLayout);
                availableCslLayouts.setAll(updatedLayouts);

                openOfficePreferences.setCurrentStyle(citationStyleToAdd);

                dialogService.showInformationDialogAndWait(
                        Localization.lang("Style added"),
                        Localization.lang("The CSL style has been added successfully.")
                );
            } else {
                dialogService.showErrorDialogAndWait(
                        Localization.lang("Style not found"),
                        Localization.lang("The CSL style was added but could not be found in the list.")
                );
            }
        });
    }

    public void deleteCslStyle(CitationStyle style) {
        deleteStyle(style, () -> cslStyleLoader.removeStyle(style), availableCslLayouts,
                selectedCslLayoutProperty, CitationStylePreviewLayout::citationStyle);
    }

    private boolean isDuplicate(CitationStyle styleToAdd) {
        return CSLStyleLoader.getStyles().stream()
                             .anyMatch(existingStyle -> hasSameStyleName(existingStyle, styleToAdd));
    }

    private boolean hasSameStyleName(CitationStyle existingStyle, CitationStyle styleToAdd) {
        return existingStyle.getTitle().equals(styleToAdd.getTitle())
                || (!styleToAdd.getShortTitle().isBlank() && existingStyle.getShortTitle().equals(styleToAdd.getShortTitle()));
    }
    // endregion

    // region - bst-specific methods

    public ListProperty<BstStyleSelectViewModel> bstStylesProperty() {
        return bstStyles;
    }

    public ObjectProperty<@Nullable BstStyleSelectViewModel> selectedBstStyleProperty() {
        return selectedBstStyle;
    }

    public ObjectProperty<BstCitationFormat> bstCitationFormatProperty() {
        return bstCitationFormat;
    }

    public List<BstStyleSelectViewModel> loadBstStyles() {
        return bstStyleLoader.getStyles().stream()
                             .map(BstStyleSelectViewModel::new)
                             .toList();
    }

    public void addBstStyleFile() {
        selectStyleFile(StandardFileType.BST).ifPresent(stylePath -> {
            if (!stylePath.getFileName().toString().toLowerCase().endsWith(".bst") || !Files.exists(stylePath)) {
                dialogService.showErrorDialogAndWait(
                        Localization.lang("Invalid style selected"),
                        Localization.lang("You must select a valid .bst style file.")
                );
                return;
            }

            BstStyle bstStyleToAdd;
            try {
                bstStyleToAdd = BstStyle.loadExternal(stylePath);
            } catch (IOException _) {
                dialogService.showErrorDialogAndWait(
                        Localization.lang("Invalid style selected"),
                        Localization.lang("You must select a valid .bst style file.")
                );
                return;
            }

            if (isDuplicate(bstStyleToAdd)) {
                dialogService.showErrorDialogAndWait(
                        Localization.lang("Style already available"),
                        Localization.lang("A style with the same filename already exists. If it is a different style, please rename and import.")
                );
                return;
            }

            bstStyleLoader.addExternalStyle(bstStyleToAdd);
            BstStyleSelectViewModel vm = new BstStyleSelectViewModel(bstStyleToAdd);
            bstStyles.add(vm);
            selectedBstStyle.setValue(vm);
            openOfficePreferences.setCurrentStyle(bstStyleToAdd);
        });
    }

    public void deleteBstStyle(BstStyle style) {
        deleteStyle(style, () -> bstStyleLoader.removeStyle(style), bstStyles,
                selectedBstStyle, BstStyleSelectViewModel::getBstStyle);
    }

    private boolean isDuplicate(BstStyle fileToAdd) {
        return bstStyleLoader.getStyles().stream()
                             .anyMatch(existingStyle -> hasSameStyleName(existingStyle, fileToAdd));
    }

    private boolean hasSameStyleName(BstStyle existingStyle, BstStyle styleToAdd) {
        return existingStyle.getName().equals(styleToAdd.getName());
    }

    // endregion

    // region - jstyle-specific methods

    public JStyleSelectViewModel fromJStyle(JStyle style) {
        return new JStyleSelectViewModel(style);
    }

    public JStyle toJStyle(JStyleSelectViewModel item) {
        return item.getJStyle();
    }

    public ListProperty<JStyleSelectViewModel> jStylesProperty() {
        return jStyles;
    }

    public ObjectProperty<@Nullable JStyleSelectViewModel> selectedJStyleProperty() {
        return selectedJStyle;
    }

    public List<JStyleSelectViewModel> loadJStyles() {
        return jStyleLoader.getStyles().stream().map(this::fromJStyle).toList();
    }

    private JStyleSelectViewModel getJStyleOrDefault(String stylePath) {
        return jStyles.stream()
                      .filter(style -> style.getStylePath().equals(stylePath))
                      .findFirst()
                      .orElse(jStyles.getFirst());
    }

    public void addJStyleFile() {
        selectStyleFile(StandardFileType.JSTYLE).ifPresent(stylePath -> {
            if (jStyleLoader.addStyleIfValid(stylePath)) {
                jStyles.setAll(loadJStyles());
                selectedJStyle.setValue(getJStyleOrDefault(stylePath.toString()));
            } else {
                dialogService.showErrorDialogAndWait(Localization.lang("Invalid style selected"), Localization.lang("You must select a valid style file. Your style is probably missing a line for the type \"default\"."));
            }
        });
    }

    public void editJStyle() {
        JStyle jStyle = selectedJStyle.getValue().getJStyle();
        Optional<ExternalFileType> type = ExternalFileTypes.getExternalFileTypeByExt("jstyle", externalApplicationsPreferences);
        try {
            NativeDesktop.openExternalFileAnyFormat(new BibDatabaseContext(), externalApplicationsPreferences, filePreferences, jStyle.getPath(), type);
        } catch (IOException e) {
            dialogService.showErrorDialogAndWait(e);
        }
    }

    public void deleteJStyle(JStyle style) {
        deleteStyle(style, () -> jStyleLoader.removeStyle(style), jStyles,
                selectedJStyle, JStyleSelectViewModel::getJStyle);
    }

    // endregion
}
