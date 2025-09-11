package org.jabref.gui.cleanup;

import java.util.Objects;
import java.util.Optional;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.layout.VBox;

import org.jabref.gui.commonfxcontrols.FieldFormatterCleanupsPanel;
import org.jabref.logic.cleanup.CleanupPreferences;
import org.jabref.logic.cleanup.FieldFormatterCleanups;

import com.airhacks.afterburner.views.ViewLoader;

public class CleanupSingleFieldPanel extends VBox implements CleanupPanel {

    @FXML private FieldFormatterCleanupsPanel formatterCleanupsPanel;

    public CleanupSingleFieldPanel(CleanupPreferences cleanupPreferences) {
        Objects.requireNonNull(cleanupPreferences, "cleanupPreferences must not be null");

        ViewLoader.view(this)
                  .root(this)
                  .load();

        init(cleanupPreferences);
    }

    private void init(CleanupPreferences cleanupPreferences) {
        formatterCleanupsPanel.cleanupsDisableProperty().setValue(!cleanupPreferences.getFieldFormatterCleanups().isEnabled());
        formatterCleanupsPanel.cleanupsProperty().setValue(FXCollections.observableArrayList(
                cleanupPreferences.getFieldFormatterCleanups().getConfiguredActions()
        ));
    }

    @Override
    public Optional<CleanupPreferences> getCleanupPreferences() {
        FieldFormatterCleanups fieldFormatterCleanups = new FieldFormatterCleanups(
                !formatterCleanupsPanel.cleanupsDisableProperty().getValue(),
                formatterCleanupsPanel.cleanupsProperty()
        );
        return Optional.of(new CleanupPreferences(fieldFormatterCleanups));
    }
}
