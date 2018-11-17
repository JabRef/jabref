package org.jabref.gui.openoffice;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import javafx.beans.property.ListProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.collections.FXCollections;
import javafx.scene.Node;
import javafx.scene.layout.FlowPane;
import javafx.scene.text.Text;

import org.jabref.gui.DialogService;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.openoffice.CitationEntry;
import org.jabref.model.strings.StringUtil;

import com.sun.star.beans.IllegalTypeException;
import com.sun.star.beans.NotRemoveableException;
import com.sun.star.beans.PropertyExistException;
import com.sun.star.beans.UnknownPropertyException;
import com.sun.star.container.NoSuchElementException;
import com.sun.star.container.XNameAccess;
import com.sun.star.lang.IllegalArgumentException;
import com.sun.star.lang.WrappedTargetException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ManageCitationsDialogViewModel {

    private static final String HTML_BOLD_END_TAG = "</b>";
    private static final String HTML_BOLD_START_TAG = "<b>";

    private static final Logger LOGGER = LoggerFactory.getLogger(ManageCitationsDialogViewModel.class);

    private final ListProperty<ManageCitationsItemViewModel> citations = new SimpleListProperty<>(FXCollections.observableArrayList());
    private final OOBibBase ooBase;
    private final DialogService dialogService;

    public ManageCitationsDialogViewModel(OOBibBase ooBase, DialogService dialogService) throws NoSuchElementException, WrappedTargetException, UnknownPropertyException {
        this.ooBase = ooBase;
        this.dialogService = dialogService;

        XNameAccess nameAccess = ooBase.getReferenceMarks();
        List<String> names = ooBase.getJabRefReferenceMarks(nameAccess);
        for (String name : names) {

            CitationEntry entry = new CitationEntry(name,
                                                    ooBase.getCitationContext(nameAccess, name, 30, 30, true),
                                                    ooBase.getCustomProperty(name));

            getText(ooBase.getCitationContext(nameAccess, name, 30, 30, true));

            ManageCitationsItemViewModel itemViewModelEntry = ManageCitationsItemViewModel.fromCitationEntry(entry);
            citations.add(itemViewModelEntry);
        }

    }

    //TODO: Call store settings after edit commit
    //update reference then and call refresh/sync in the background
    public void storeSettings() {
        List<CitationEntry> ciationEntries = citations.stream().map(ManageCitationsItemViewModel::toCitationEntry).collect(Collectors.toList());
        try {
            for (CitationEntry entry : ciationEntries) {
                Optional<String> pageInfo = entry.getPageInfo();
                if (pageInfo.isPresent()) {
                    ooBase.setCustomProperty(entry.getRefMarkName(), pageInfo.get());
                }
            }
        } catch (UnknownPropertyException | NotRemoveableException | PropertyExistException | IllegalTypeException |
                 IllegalArgumentException ex) {
            LOGGER.warn("Problem modifying citation", ex);
            dialogService.showErrorDialogAndWait(Localization.lang("Problem modifying citation"), ex);
        }
    }

    public ListProperty<ManageCitationsItemViewModel> citationsProperty() {
        return citations;
    }

    public Node getText(String citationContext) {

        String inBetween = StringUtil.substringBetween(citationContext, HTML_BOLD_START_TAG, HTML_BOLD_END_TAG);
        String start = citationContext.substring(0, citationContext.indexOf(HTML_BOLD_START_TAG));
        String end = citationContext.substring(citationContext.lastIndexOf(HTML_BOLD_END_TAG) + HTML_BOLD_END_TAG.length(), citationContext.length());

        Text startText = new Text(start);
        Text inBetweenText = new Text(inBetween);
        inBetweenText.setStyle("-fx-font-weight: bold");
        Text endText = new Text(end);

        FlowPane flow = new FlowPane(startText, inBetweenText, endText);
        return flow;
    }
}
