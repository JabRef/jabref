package org.jabref.gui.maintable;

import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import javax.swing.Icon;
import javax.swing.JLabel;
import javax.swing.JPopupMenu;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

import org.jabref.Globals;
import org.jabref.JabRefExecutorService;
import org.jabref.JabRefGUI;
import org.jabref.gui.BasePanel;
import org.jabref.gui.BasePanelMode;
import org.jabref.gui.GUIGlobals;
import org.jabref.gui.IconTheme;
import org.jabref.gui.PreviewPanel;
import org.jabref.gui.actions.CopyDoiUrlAction;
import org.jabref.gui.desktop.JabRefDesktop;
import org.jabref.gui.entryeditor.EntryEditor;
import org.jabref.gui.externalfiletype.ExternalFileMenuItem;
import org.jabref.gui.externalfiletype.ExternalFileType;
import org.jabref.gui.externalfiletype.UnknownExternalFileType;
import org.jabref.gui.filelist.FileListEntry;
import org.jabref.gui.filelist.FileListTableModel;
import org.jabref.gui.menus.RightClickMenu;
import org.jabref.gui.specialfields.SpecialFieldMenuAction;
import org.jabref.gui.specialfields.SpecialFieldValueViewModel;
import org.jabref.gui.specialfields.SpecialFieldViewModel;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.util.OS;
import org.jabref.logic.util.io.FileUtil;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.FieldName;
import org.jabref.model.entry.specialfields.SpecialField;
import org.jabref.model.entry.specialfields.SpecialFieldValue;

import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.event.ListEvent;
import ca.odell.glazedlists.event.ListEventListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * List event, mouse, key and focus listener for the main table that makes up the
 * most part of the BasePanel for a single BIB database.
 */
public class MainTableSelectionListener implements ListEventListener<BibEntry>, MouseListener, KeyListener, FocusListener {
    private static final Logger LOGGER = LoggerFactory.getLogger(MainTableSelectionListener.class);

    private final MainTable table;
    private final BasePanel panel;

    private final EventList<BibEntry> tableRows;
    // Register the last character pressed to quick jump in the table. Together
    // with storing the last row number jumped to, this is used to let multiple
    // key strokes cycle between all entries starting with the same letter:
    private final int[] lastPressed = new int[20];
    private PreviewPanel preview;
    private boolean workingOnPreview;
    private boolean enabled = true;
    private int lastPressedCount;

    private long lastPressedTime;

    public MainTableSelectionListener(BasePanel panel, MainTable table) {
        this.table = table;
        this.panel = panel;
        this.tableRows = table.getTableModel().getTableRows();
        preview = panel.getPreviewPanel();
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    @Override
    public void listChanged(ListEvent<BibEntry> e) {
        if (!enabled) {
            return;
        }

        EventList<BibEntry> selected = e.getSourceList();
        if (selected.isEmpty()) {
            return;
        }

        final BibEntry newSelected = selected.get(0);
        if ((panel.getMode() == BasePanelMode.SHOWING_EDITOR || panel.getMode() == BasePanelMode.WILL_SHOW_EDITOR)
                && panel.getEntryEditor() != null && newSelected == panel.getEntryEditor().getEntry()) {
            // entry already selected and currently editing it, do not steal the focus from the selected textfield
            return;
        }

        if (newSelected != null) {
            final BasePanelMode mode = panel.getMode(); // What is the panel already showing?
            if ((mode == BasePanelMode.WILL_SHOW_EDITOR) || (mode == BasePanelMode.SHOWING_EDITOR)) {
                panel.showAndEdit(newSelected);
                SwingUtilities.invokeLater(() -> table.ensureVisible(table.getSelectedRow()));
            } else if (panel.getMode() == BasePanelMode.SHOWING_NOTHING || panel.getMode() == BasePanelMode.SHOWING_PREVIEW) {
                // Either nothing or a preview was shown. Update the preview.
                updatePreview(newSelected);
            }
        }
    }

    private void updatePreview(final BibEntry toShow) {
        updatePreview(toShow, 0);
    }

    private void updatePreview(final BibEntry toShow, int repeats) {
        if (workingOnPreview) {
            if (repeats > 0) {
                return; // We've already waited once. Give up on this selection.
            }
            Timer t = new Timer(50, actionEvent -> updatePreview(toShow, 1));
            t.setRepeats(false);
            t.start();
            return;
        }
        EventList<BibEntry> list = table.getSelected();
        // Check if the entry to preview is still selected:
        if ((list.size() != 1) || (list.get(0) != toShow)) {
            return;
        }
        workingOnPreview = true;
        SwingUtilities.invokeLater(() -> {
            panel.showPreview(toShow);
            workingOnPreview = false;
        });
    }

    public void editSignalled() {
        if (table.getSelected().size() == 1) {
            editSignalled(table.getSelected().get(0));
        }
    }

    public void editSignalled(BibEntry entry) {
        panel.showAndEdit(entry);
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        // First find the column and row on which the user has clicked.
        final int col = table.columnAtPoint(e.getPoint());
        final int row = table.rowAtPoint(e.getPoint());

        // get the MainTableColumn which is currently visible at col
        int modelIndex = table.getColumnModel().getColumn(col).getModelIndex();
        MainTableColumn modelColumn = table.getMainTableColumn(modelIndex);

        // Check if the user has right-clicked. If so, open the right-click menu.
        if (e.isPopupTrigger() || (e.getButton() == MouseEvent.BUTTON3)) {
            if ((modelColumn == null) || !modelColumn.isIconColumn()) {
                // show normal right click menu
                processPopupTrigger(e, row);
            } else {
                // show right click menu for icon columns
                showIconRightClickMenu(e, row, modelColumn);
            }
        }
    }

    @Override
    public void mousePressed(MouseEvent e) {
        // all handling is done in "mouseReleased"
    }

    @Override
    public void mouseClicked(MouseEvent e) {

        // First find the column on which the user has clicked.
        final int row = table.rowAtPoint(e.getPoint());

        // A double click on an entry should open the entry's editor.
        if (e.getClickCount() == 2) {
            BibEntry toShow = tableRows.get(row);
            editSignalled(toShow);
            return;
        }

        final int col = table.columnAtPoint(e.getPoint());
        // get the MainTableColumn which is currently visible at col
        int modelIndex = table.getColumnModel().getColumn(col).getModelIndex();
        MainTableColumn modelColumn = table.getMainTableColumn(modelIndex);

        // Workaround for Windows. Right-click is not popup trigger on mousePressed, but
        // on mouseReleased. Therefore we need to avoid taking action at this point, because
        // action will be taken when the button is released:
        if (OS.WINDOWS && (modelColumn.isIconColumn()) && (e.getButton() != MouseEvent.BUTTON1)) {
            return;
        }

        // Check if the clicked colum is a specialfield column
        if (modelColumn.isIconColumn() && (SpecialField.isSpecialField(modelColumn.getColumnName()))) {
            // handle specialfield
            handleSpecialFieldLeftClick(e, modelColumn.getColumnName());
        } else if (modelColumn.isIconColumn()) { // left click on icon field

            Object value = table.getValueAt(row, col);
            if (value == null) {
                return; // No icon here, so we do nothing.
            }

            final BibEntry entry = tableRows.get(row);

            final List<String> fieldNames = modelColumn.getBibtexFields();

            // Open it now. We do this in a thread, so the program won't freeze during the wait.
            JabRefExecutorService.INSTANCE.execute(() -> {
                panel.output(Localization.lang("External viewer called") + '.');
                // check for all field names whether a link is present
                // (is relevant for combinations such as "url/doi")
                for (String fieldName : fieldNames) {
                    // Check if field is present, if not skip this field
                    if (entry.hasField(fieldName)) {
                        String link = entry.getField(fieldName).get();

                        // See if this is a simple file link field, or if it is a file-list
                        // field that can specify a list of links:
                        if (fieldName.equals(FieldName.FILE)) {

                            // We use a FileListTableModel to parse the field content:
                            FileListTableModel fileList = new FileListTableModel();
                            fileList.setContent(link);

                            FileListEntry flEntry = null;
                            // If there are one or more links of the correct type, open the first one:
                            if (modelColumn.isFileFilter()) {
                                for (int i = 0; i < fileList.getRowCount(); i++) {
                                    if (fileList.getEntry(i).getType().toString().equals(modelColumn.getColumnName())) {
                                        flEntry = fileList.getEntry(i);
                                        break;
                                    }
                                }
                            } else if (fileList.getRowCount() > 0) {
                                //If there are no file types specified open the first file
                                flEntry = fileList.getEntry(0);
                            }
                            if (flEntry != null) {
                                ExternalFileMenuItem item = new ExternalFileMenuItem(panel.frame(), entry, "",
                                        flEntry.getLink(), flEntry.getType().map(ExternalFileType::getIcon).orElse(null),
                                        panel.getBibDatabaseContext(), flEntry.getType());
                                item.doClick();
                            }
                        } else {
                            try {
                                JabRefDesktop.openExternalViewer(panel.getBibDatabaseContext(), link, fieldName);
                            } catch (IOException ex) {
                                panel.output(Localization.lang("Unable to open link."));
                                LOGGER.info("Unable to open link", ex);
                            }
                        }
                        break; // only open the first link
                    }
                }
            });
        } else if (modelColumn.getBibtexFields().contains(FieldName.CROSSREF)) { // Clicking on crossref column
            tableRows.get(row).getField(FieldName.CROSSREF)
                    .ifPresent(crossref -> panel.getDatabase().getEntryByKey(crossref).ifPresent(entry -> panel.highlightEntry(entry)));
        }
        panel.frame().updateEnabledState();
    }

    /**
     * Method to handle a single left click on one the special fields (e.g., ranking, quality, ...)
     * Shows either a popup to select/clear a value or simply toggles the functionality to set/unset the special field
     *
     * @param e MouseEvent used to determine the position of the popups
     * @param columnName the name of the specialfield column
     */
    private void handleSpecialFieldLeftClick(MouseEvent e, String columnName) {
        if ((e.getClickCount() == 1)) {
            SpecialField.getSpecialFieldInstanceFromFieldName(columnName).ifPresent(field -> {
                // special field found
                if (field.isSingleValueField()) {
                    // directly execute toggle action instead of showing a menu with one action
                    new SpecialFieldViewModel(field).getSpecialFieldAction(field.getValues().get(0), panel.frame()).action();
                } else {
                    JPopupMenu menu = new JPopupMenu();
                    for (SpecialFieldValue val : field.getValues()) {
                        menu.add(new SpecialFieldMenuAction(new SpecialFieldValueViewModel(val), panel.frame()));
                    }
                    menu.show(table, e.getX(), e.getY());
                }
            });
        }
    }

    /**
     * Process general right-click events on the table. Show the table context menu at
     * the position where the user right-clicked.
     * @param e The mouse event defining the popup trigger.
     * @param row The row where the event occurred.
     */
    private void processPopupTrigger(MouseEvent e, int row) {
        int selRow = table.getSelectedRow();
        if ((selRow == -1) || !table.isRowSelected(table.rowAtPoint(e.getPoint()))) {
            table.setRowSelectionInterval(row, row);
        }
        RightClickMenu rightClickMenu = new RightClickMenu(JabRefGUI.getMainFrame(), panel);
        rightClickMenu.show(table, e.getX(), e.getY());
    }

    /**
     * Process popup trigger events occurring on an icon cell in the table. Show a menu where the user can choose which
     * external resource to open for the entry. If no relevant external resources exist, let the normal popup trigger
     * handler do its thing instead.
     *
     * @param e The mouse event defining this popup trigger.
     * @param row The row where the event occurred.
     * @param column the MainTableColumn associated with this table cell.
     */
    private void showIconRightClickMenu(MouseEvent e, int row, MainTableColumn column) {
        BibEntry entry = tableRows.get(row);
        JPopupMenu menu = new JPopupMenu();
        boolean showDefaultPopup = true;

        // See if this is a simple file link field, or if it is a file-list
        // field that can specify a list of links:
        if (!column.getBibtexFields().isEmpty()) {
            for (String field : column.getBibtexFields()) {
                if (FieldName.FILE.equals(field)) {
                    // We use a FileListTableModel to parse the field content:
                    FileListTableModel fileList = new FileListTableModel();
                    entry.getField(field).ifPresent(fileList::setContent);
                    for (int i = 0; i < fileList.getRowCount(); i++) {
                        FileListEntry flEntry = fileList.getEntry(i);
                        if (column.isFileFilter()
                                && (!flEntry.getType().get().getName().equalsIgnoreCase(column.getColumnName()))) {
                            continue;
                        }
                        String description = flEntry.getDescription();
                        if ((description == null) || (description.trim().isEmpty())) {
                            description = flEntry.getLink();
                        }

                        Optional<ExternalFileType> fileType = flEntry.getType();

                        // file type might be unknown
                        if (!fileType.isPresent()) {
                            String fileExtension = FileUtil.getFileExtension(flEntry.getLink()).orElse("");
                            fileType = Optional.of(new UnknownExternalFileType(fileExtension.toUpperCase(), fileExtension));
                        }
                        menu.add(new ExternalFileMenuItem(panel.frame(), entry, description, flEntry.getLink(),
                                fileType.get().getIcon(), panel.getBibDatabaseContext(), fileType));
                        showDefaultPopup = false;
                    }
                } else {
                    if (SpecialField.isSpecialField(column.getColumnName())) {
                        // full pop should be shown as left click already shows short popup
                        showDefaultPopup = true;
                    } else {
                        Optional<String> content = entry.getField(field);
                        if (content.isPresent()) {
                            Icon icon;
                            JLabel iconLabel = GUIGlobals.getTableIcon(field);
                            if (iconLabel == null) {
                                icon = IconTheme.JabRefIcon.FILE.getIcon();
                            } else {
                                icon = iconLabel.getIcon();
                            }
                            menu.add(new ExternalFileMenuItem(panel.frame(), entry, content.get(), content.get(), icon,
                                    panel.getBibDatabaseContext(), field));
                            if (field.equals(FieldName.DOI)) {
                                menu.add(new CopyDoiUrlAction(content.get()));
                            }
                            showDefaultPopup = false;
                        }
                    }
                }
            }
            if (showDefaultPopup) {
                processPopupTrigger(e, row);
            } else {
                menu.show(table, e.getX(), e.getY());
            }
        }
    }

    public void entryEditorClosing(EntryEditor editor) {
        if (Globals.prefs.getPreviewPreferences().isPreviewPanelEnabled()) {
            panel.showPreview(editor.getEntry());
        } else {
            panel.hideBottomComponent();
            panel.adjustSplitter();
        }
        table.requestFocus();
    }

    @Override
    public void mouseEntered(MouseEvent e) {
        // Do nothing
    }

    @Override
    public void mouseExited(MouseEvent e) {
        // Do nothing
    }

    /**
     * Receive key event on the main table. If the key is a letter or a digit,
     * we should select the first entry in the table which starts with the given
     * letter in the column by which the table is sorted.
     * @param e The KeyEvent
     */
    @Override
    public void keyTyped(KeyEvent e) {
        if ((!e.isActionKey()) && Character.isLetterOrDigit(e.getKeyChar())
                && (e.getModifiers() == 0)) {
            long time = System.currentTimeMillis();
            final long QUICK_JUMP_TIMEOUT = 2000;
            if ((time - lastPressedTime) > QUICK_JUMP_TIMEOUT) {
                lastPressedCount = 0; // Reset last pressed character
            }
            // Update timestamp:
            lastPressedTime = time;
            // Add the new char to the search array:
            int c = e.getKeyChar();
            if (lastPressedCount < lastPressed.length) {
                lastPressed[lastPressedCount] = c;
                lastPressedCount++;
            }

            int sortingColumn = table.getSortingColumn(0);
            if (sortingColumn == -1) {
                return; // No sorting? TODO: look up by author, etc.?
            }
            // TODO: the following lookup should be done by a faster algorithm,
            // such as binary search. But the table may not be sorted properly,
            // due to marked entries, search etc., which rules out the binary search.

            for (int i = 0; i < table.getRowCount(); i++) {
                Object o = table.getValueAt(i, sortingColumn);
                if (o == null) {
                    continue;
                }
                String s = o.toString().toLowerCase(Locale.ROOT);
                if (s.length() >= lastPressedCount) {
                    for (int j = 0; j < lastPressedCount; j++) {
                        if (s.charAt(j) != lastPressed[j]) {
                            break; // Escape the loop immediately when we find a mismatch
                        } else if (j == (lastPressedCount - 1)) {
                            // We found a match:
                            table.setRowSelectionInterval(i, i);
                            table.ensureVisible(i);
                            return;
                        }
                    }
                }
            }

        } else if (e.getKeyChar() == KeyEvent.VK_ESCAPE) {
            lastPressedCount = 0;
        }
        panel.frame().updateEnabledState();
    }

    @Override
    public void keyReleased(KeyEvent e) {
        // Do nothing
    }

    @Override
    public void keyPressed(KeyEvent e) {
        // Do nothing
    }

    @Override
    public void focusGained(FocusEvent e) {
        // Do nothing
    }

    @Override
    public void focusLost(FocusEvent e) {
        lastPressedCount = 0; // Reset quick jump when focus is lost.
    }

    public PreviewPanel getPreview() {
        return preview;
    }
}
