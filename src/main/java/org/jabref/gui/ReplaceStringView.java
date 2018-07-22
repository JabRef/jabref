package org.jabref.gui;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.DialogPane;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleGroup;
import javafx.stage.Stage;

import org.jabref.gui.undo.NamedCompound;
import org.jabref.gui.undo.UndoableFieldChange;
import org.jabref.gui.util.BaseDialog;
import org.jabref.gui.util.IconValidationDecorator;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.database.BibDatabase;
import org.jabref.model.entry.BibEntry;

import com.airhacks.afterburner.views.ViewLoader;
import de.saxsys.mvvmfx.utils.validation.visualization.ControlsFxVisualizer;

public class ReplaceStringView extends BaseDialog<Void>
{

    @FXML public ToggleGroup RadioGroup;
    @FXML public Button CancelButton;
    @FXML public Button ReplaceButton;
    @FXML private TextField LimitFieldInput;
    @FXML private TextField FindField;
    @FXML private TextField ReplaceField;
    @FXML private CheckBox checkLimit;
    @FXML private DialogPane pane;

    private boolean AllFieldReplace;
    private boolean selOnly;
    private String findString;
    private String replaceString;
    private String[] fieldStrings;
    private BibDatabase database;
    private boolean exitSignal;
    private BasePanel panel;
    private Stage st;

    private final ControlsFxVisualizer visualizer = new ControlsFxVisualizer();

    public ReplaceStringView(BibDatabase bibDatabase, BasePanel basePanel)  {
        this.setTitle(Localization.lang("Replace String"));

        database = bibDatabase;
        AllFieldReplace = true;
        exitSignal = false;
        selOnly = false;
        panel = basePanel;

        ViewLoader.view(this)
                  .load()
                  .setAsDialogPane(this);

        st = (Stage) this.pane.getScene().getWindow();
        st.setOnCloseRequest(event -> st.close());
    }

    @FXML
    public void initialize() {
        visualizer.setDecoration(new IconValidationDecorator());
        LimitFieldInput.setEditable(true);
        FindField.setEditable(true);
        ReplaceField.setEditable(true);
        checkLimit.setSelected(false);
    }

    /**
     * FXML Message handler
    */
    @FXML
    public void ButtonReplace() {
        findString = FindField.getText();
        replaceString = ReplaceField.getText();
        fieldStrings = LimitFieldInput.getText().toLowerCase().split(";");
        if("".equals(findString))
        {
            exitSignal = true;
            st.close();
            return;
        }
        replace();
        exitSignal = true;
        st.close();
    }

    @FXML
    public void ButtonCancel() {
        exitSignal = true;
        st.close();
    }

    @FXML
    public void RadioAll() {
        AllFieldReplace = true;
    }

    @FXML
    public void RadioLimit() {
        AllFieldReplace = false;
    }

    @FXML
    public void selectOnly() {
        selOnly = !selOnly;
    }

    public boolean isExit() {
        return this.exitSignal;
    }

    /**
     *  General replacement, same as Action:Replace_All in BasePanel.java
     *  Rep check: BasePanel == null
     *  @return : replace count
     */
    public int replace() {
        final NamedCompound ce = new NamedCompound(Localization.lang("Replace string"));
        int counter = 0;
        if(this.panel == null)
            return 0;
        if(this.selOnly) {
            for(BibEntry bibEntry: this.panel.getSelectedEntries())
            {
                counter += replaceItem(bibEntry, ce);
            }
        }
        else {
            for(BibEntry bibEntry: this.panel.getDatabase().getEntries())
            {
                counter += replaceItem(bibEntry, ce);
            }
        }
        return counter;
    }

    /**
     * Does the actual operation on a Bibtex entry based on the
     * settings specified in this same dialog. Returns the number of
     * occurences replaced.
     * Copied and Adapted from org.jabref.gui.ReplaceStringDialog.java
     */
    public int replaceItem(BibEntry be, NamedCompound ce) {
        int counter = 0;
        if (this.AllFieldReplace) {
            for (String s : be.getFieldNames()) {
                counter += replaceField(be, s, ce);
            }
        } else {
            for (String fld : fieldStrings) {
                counter += replaceField(be, fld, ce);
            }
        }
        return counter;
    }

    private int replaceField(BibEntry be, String fieldname, NamedCompound ce) {
        if (!be.hasField(fieldname)) {
            return 0;
        }
        String txt = be.getField(fieldname).get();
        StringBuilder sb = new StringBuilder();
        int ind;
        int piv = 0;
        int counter = 0;
        int len1 = this.findString.length();
        while ((ind = txt.indexOf(this.findString, piv)) >= 0) {
            counter++;
            sb.append(txt, piv, ind); // Text leading up to s1
            sb.append(this.replaceString); // Insert s2
            piv = ind + len1;
        }
        sb.append(txt.substring(piv));
        String newStr = sb.toString();
        be.setField(fieldname, newStr);
        ce.addEdit(new UndoableFieldChange(be, fieldname, txt, newStr));
        return counter;
    }

}
