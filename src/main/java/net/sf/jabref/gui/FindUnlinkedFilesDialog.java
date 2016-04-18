/*  Copyright (C) 2003-2015 JabRef contributors.
    This program is free software; you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation; either version 2 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License along
    with this program; if not, write to the Free Software Foundation, Inc.,
    51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
*/
package net.sf.jabref.gui;

import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Vector;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JRootPane;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.JTree;
import javax.swing.KeyStroke;
import javax.swing.WindowConstants;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.filechooser.FileSystemView;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;

import net.sf.jabref.Globals;
import net.sf.jabref.JabRefExecutorService;
import net.sf.jabref.JabRefGUI;
import net.sf.jabref.JabRefPreferences;
import net.sf.jabref.gui.desktop.JabRefDesktop;
import net.sf.jabref.importer.EntryFromFileCreator;
import net.sf.jabref.importer.EntryFromFileCreatorManager;
import net.sf.jabref.importer.UnlinkedFilesCrawler;
import net.sf.jabref.importer.UnlinkedPDFFileFilter;
import net.sf.jabref.logic.l10n.Localization;
import net.sf.jabref.model.EntryTypes;
import net.sf.jabref.model.database.BibDatabase;
import net.sf.jabref.model.entry.EntryType;

import com.jgoodies.forms.builder.ButtonBarBuilder;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * GUI Dialog for the feature "Find unlinked files".
 *
 * @author Nosh&Dan
 * @version 25.11.2008 | 23:13:29
 *
 */
public class FindUnlinkedFilesDialog extends JDialog {

    private static final Log LOGGER = LogFactory.getLog(FindUnlinkedFilesDialog.class);

    /**
     * Keys to be used for referencing this Action.
     */
    public static final String ACTION_COMMAND = "findUnlinkedFiles";
    public static final String ACTION_MENU_TITLE = Localization.menuTitle("Find unlinked files...");

    public static final String ACTION_SHORT_DESCRIPTION = Localization
            .lang("Searches for unlinked PDF files on the file system");
    private static final String GLOBAL_PREFS_WORKING_DIRECTORY_KEY = "findUnlinkedFilesWD";

    private static final String GLOBAL_PREFS_DIALOG_SIZE_KEY = "findUnlinkedFilesDialogSize";
    private JabRefFrame frame;
    private BibDatabase database;
    private EntryFromFileCreatorManager creatorManager;

    private UnlinkedFilesCrawler crawler;
    private Path lastSelectedDirectory;

    private TreeModel treeModel;
    /* PANELS */
    private JPanel panelDirectory;
    private JPanel panelSearchArea;
    private JPanel panelFiles;
    private JPanel panelOptions;
    private JPanel panelButtons;
    private JPanel panelEntryTypesSelection;

    private JPanel panelImportArea;
    private JButton buttonBrowse;
    private JButton buttonScan;
    private JButton buttonApply;

    private JButton buttonClose;
    /* Options for the TreeView */
    private JButton buttonOptionSelectAll;
    private JButton buttonOptionUnselectAll;
    private JButton buttonOptionExpandAll;
    private JButton buttonOptionCollapseAll;

    private JCheckBox checkboxCreateKeywords;
    private JTextField textfieldDirectoryPath;
    private JLabel labelDirectoryDescription;
    private JLabel labelFileTypesDescription;
    private JLabel labelFilesDescription;
    private JLabel labelEntryTypeDescription;
    private JLabel labelSearchingDirectoryInfo;

    private JLabel labelImportingInfo;
    private JTree tree;
    private JScrollPane scrollpaneTree;
    private JComboBox<FileFilter> comboBoxFileTypeSelection;

    private JComboBox<BibtexEntryTypeWrapper> comboBoxEntryTypeSelection;
    private JProgressBar progressBarSearching;
    private JProgressBar progressBarImporting;
    private JFileChooser fileChooser;

    private MouseListener treeMouseListener;
    private Action actionSelectAll;
    private Action actionUnselectAll;
    private Action actionExpandTree;

    private Action actionCollapseTree;

    private ComponentListener dialogPositionListener;
    private final AtomicBoolean threadState = new AtomicBoolean();

    private boolean checkBoxWhyIsThereNoGetSelectedStupidSwing;


    /**
     * For Unit-testing only. <i>Don't remove!</i> <br>
     * Used via reflection in {@link net.sf.jabref.importer.DatabaseFileLookup} to construct this
     * class.
     */
    @SuppressWarnings("unused")
    private FindUnlinkedFilesDialog() {
        //intended
    }

    public FindUnlinkedFilesDialog(Frame owner, JabRefFrame frame, BasePanel panel) {
        super(owner, Localization.lang("Find unlinked files"), true);
        this.frame = frame;

        restoreSizeOfDialog();

        database = panel.getDatabase();
        creatorManager = new EntryFromFileCreatorManager();
        crawler = new UnlinkedFilesCrawler(database);

        lastSelectedDirectory = loadLastSelectedDirectory();

        initialize();
        buttonApply.setEnabled(false);
    }

    /**
     * Close dialog when pressing escape
     */
    @Override
    protected JRootPane createRootPane() {
        ActionListener actionListener = actionEvent -> setVisible(false);
        JRootPane rPane = new JRootPane();
        KeyStroke stroke = KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0);
        rPane.registerKeyboardAction(actionListener, stroke, JComponent.WHEN_IN_FOCUSED_WINDOW);

        return rPane;
    }

    /**
     * Stores the current size of this dialog persistently.
     */
    private void storeSizeOfDialog() {
        Dimension dim = getSize();
        String store = dim.width + ";" + dim.height;
        Globals.prefs.put(FindUnlinkedFilesDialog.GLOBAL_PREFS_DIALOG_SIZE_KEY, store);
    }

    /**
     * Restores the location and size of this dialog from the persistent storage.
     */
    private void restoreSizeOfDialog() {

        String store = Globals.prefs.get(FindUnlinkedFilesDialog.GLOBAL_PREFS_DIALOG_SIZE_KEY);

        Dimension dimension = null;

        if (store != null) {
            try {
                String[] dim = store.split(";");
                dimension = new Dimension(Integer.valueOf(dim[0]), Integer.valueOf(dim[1]));
            } catch (NumberFormatException ignoredEx) {
                LOGGER.debug("RestoreSizeDialog Exception ", ignoredEx);
            }
        }
        if (dimension != null) {
            setPreferredSize(dimension);
        }
    }

    /**
     * Initializes the components, the layout, the data structure and the
     * actions in this dialog.
     */
    private void initialize() {

        initializeActions();
        initComponents();
        createTree();
        createFileTypesCombobox();
        createEntryTypesCombobox();
        initLayout();
        setupActions();
        pack();
    }

    /**
     * Initializes action objects. <br>
     * Does not assign actions to components yet!
     */
    private void initializeActions() {

        actionSelectAll = new AbstractAction(Localization.lang("Select all")) {

            @Override
            public void actionPerformed(ActionEvent e) {
                CheckableTreeNode rootNode = (CheckableTreeNode) tree.getModel().getRoot();
                rootNode.setSelected(true);
                tree.invalidate();
                tree.repaint();
            }
        };

        actionUnselectAll = new AbstractAction(Localization.lang("Unselect all")) {

            @Override
            public void actionPerformed(ActionEvent e) {
                CheckableTreeNode rootNode = (CheckableTreeNode) tree.getModel().getRoot();
                rootNode.setSelected(false);
                tree.invalidate();
                tree.repaint();
            }
        };

        actionExpandTree = new AbstractAction(Localization.lang("Expand all")) {

            @Override
            public void actionPerformed(ActionEvent e) {
                CheckableTreeNode rootNode = (CheckableTreeNode) tree.getModel().getRoot();
                expandTree(tree, new TreePath(rootNode), true);
            }
        };

        actionCollapseTree = new AbstractAction(Localization.lang("Collapse all")) {

            @Override
            public void actionPerformed(ActionEvent e) {
                CheckableTreeNode rootNode = (CheckableTreeNode) tree.getModel().getRoot();
                expandTree(tree, new TreePath(rootNode), false);
            }
        };

        dialogPositionListener = new ComponentAdapter() {

            /* (non-Javadoc)
             * @see java.awt.event.ComponentAdapter#componentResized(java.awt.event.ComponentEvent)
             */
            @Override
            public void componentResized(ComponentEvent e) {
                storeSizeOfDialog();
            }

            /* (non-Javadoc)
             * @see java.awt.event.ComponentAdapter#componentMoved(java.awt.event.ComponentEvent)
             */
            @Override
            public void componentMoved(ComponentEvent e) {
                storeSizeOfDialog();
            }
        };

    }

    /**
     * Stores the working directory path for this view in the global
     * preferences.
     *
     * @param lastSelectedDir
     *            directory that is used as the working directory in this view.
     */
    private void storeLastSelectedDirectory(Path lastSelectedDir) {
        lastSelectedDirectory = lastSelectedDir;
        if (lastSelectedDirectory != null) {
            Globals.prefs.put(FindUnlinkedFilesDialog.GLOBAL_PREFS_WORKING_DIRECTORY_KEY,
                    lastSelectedDirectory.toAbsolutePath().toString());
        }
    }

    /**
     * Loads the working directory path which is persistantly stored for this
     * view and returns it as a {@link File}-Object. <br>
     * <br>
     * If there is no working directory path stored, the general working
     * directory will be consulted.
     *
     * @return The persistently stored working directory path for this view.
     */
    private Path loadLastSelectedDirectory() {
        String workingDirectory = Globals.prefs.get(FindUnlinkedFilesDialog.GLOBAL_PREFS_WORKING_DIRECTORY_KEY);
        if (workingDirectory == null) {
            workingDirectory = Globals.prefs.get(JabRefPreferences.WORKING_DIRECTORY);
        }
        lastSelectedDirectory = Paths.get(workingDirectory);

        return lastSelectedDirectory;
    }

    /**
     * Opens a {@link JFileChooser} and receives the user input as a
     * {@link File} object, which this method returns. <br>
     * <br>
     * The "Open file" dialog will start at the path that is set in the
     * "directory" textfield, or at the last stored path for this dialog, if the
     * textfield is empty. <br>
     * <br>
     * If the user cancels the "Open file" dialog, this method returns null. <br>
     * <br>
     * If the user has selected a valid directory in the "Open file" dialog,
     * this path will be stored persistently for this dialog, so that it can be
     * preset at the next time this dialog is opened.
     *
     * @return The selected directory from the user, or <code>null</code>, if
     *         the user has aborted the selection.
     */
    private Path chooseDirectory() {

        if (fileChooser == null) {
            fileChooser = new JFileChooser();
            fileChooser.setAutoscrolls(true);
            fileChooser.setDialogTitle(Localization.lang("Select directory"));
            fileChooser.setApproveButtonText(Localization.lang("Choose Directory"));
            fileChooser.setApproveButtonToolTipText(
                    Localization.lang("Use the selected directory to start with the search."));
            fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        }

        String path = textfieldDirectoryPath.getText();
        if (path.isEmpty()) {
            fileChooser.setCurrentDirectory(lastSelectedDirectory.toFile());
        } else {
            fileChooser.setCurrentDirectory(Paths.get(path).toFile());
        }

        int result = fileChooser.showOpenDialog(frame);
        if (result == JFileChooser.CANCEL_OPTION) {
            return null;
        }
        Path selectedDirectory = fileChooser.getSelectedFile().toPath();
        String filepath = "";
        if (selectedDirectory != null) {
            filepath = selectedDirectory.toAbsolutePath().toString();
        }
        textfieldDirectoryPath.setText(filepath);

        return selectedDirectory;
    }

    /**
     * Disables or enables all visible Elements in this Dialog. <br>
     * <br>
     * This also removes the {@link MouseListener} from the Tree-View to prevent
     * it from receiving mouse events when in disabled-state.
     *
     * @param enable
     *            <code>true</code> when the elements shall get enabled,
     *            <code>false</code> when they shall get disabled.
     */
    private void disOrEnableDialog(boolean enable) {

        if (enable) {
            tree.addMouseListener(treeMouseListener);
        } else {
            tree.removeMouseListener(treeMouseListener);
        }
        disOrEnableAllElements(FindUnlinkedFilesDialog.this, enable);

    }

    /**
     * Recursively disables or enables all swing and awt components in this
     * dialog, starting with but not including the container
     * <code>startContainer</code>.
     *
     * @param startContainer
     *            The GUI Element to start with.
     * @param enable
     *            <code>true</code>, if all elements will get enabled,
     *            <code>false</code> if all elements will get disabled.
     */
    private void disOrEnableAllElements(Container startContainer, boolean enable) {
        Component[] children = startContainer.getComponents();
        for (Component child : children) {
            if (child instanceof Container) {
                disOrEnableAllElements((Container) child, enable);
            }
            child.setEnabled(enable);
        }
    }

    /**
     * Expands or collapses the specified tree according to the
     * <code>expand</code>-parameter.
     */
    private void expandTree(JTree currentTree, TreePath parent, boolean expand) {
        TreeNode node = (TreeNode) parent.getLastPathComponent();
        if (node.getChildCount() >= 0) {
            for (Enumeration<TreeNode> e = node.children(); e.hasMoreElements();) {
                TreePath path = parent.pathByAddingChild(e.nextElement());
                expandTree(currentTree, path, expand);
            }
        }
        if (expand) {
            currentTree.expandPath(parent);
        } else {
            currentTree.collapsePath(parent);
        }
    }

    /**
     * Starts the search of unlinked files according to the current dialog
     * state. <br>
     * <br>
     * This state is made of: <br>
     * <li>The value of the "directory"-input-textfield and <li>The file type
     * selection. <br>
     * The search will process in a seperate thread and the progress bar behind
     * the "search" button will be displayed. <br>
     * <br>
     * When the search has completed, the
     * {@link #searchFinishedHandler(CheckableTreeNode)} handler method is
     * invoked.
     */
    private void startSearch() {

        Path directory = Paths.get(textfieldDirectoryPath.getText());
        if (Files.notExists(directory)) {
            directory = Paths.get(System.getProperty("user.dir"));
        }
        if (!Files.isDirectory(directory)) {
            directory = directory.getParent();
        }

        //this addtional statement is needed because for the lamdba the variable must be effetively final
        Path dir = directory;

        storeLastSelectedDirectory(directory);

        progressBarSearching.setMinimumSize(
                new Dimension(buttonScan.getSize().width, progressBarSearching.getMinimumSize().height));
        progressBarSearching.setVisible(true);
        progressBarSearching.setString("");

        labelSearchingDirectoryInfo.setVisible(true);
        buttonScan.setVisible(false);

        disOrEnableDialog(false);
        labelSearchingDirectoryInfo.setEnabled(true);

        final FileFilter selectedFileFilter = (FileFilter) comboBoxFileTypeSelection.getSelectedItem();

        threadState.set(true);
        JabRefExecutorService.INSTANCE.execute(() -> {
            UnlinkedPDFFileFilter unlinkedPDFFileFilter = new UnlinkedPDFFileFilter(selectedFileFilter, database);
            CheckableTreeNode rootNode = crawler.searchDirectory(dir.toFile(), unlinkedPDFFileFilter, threadState,
                    new ChangeListener() {

                        int counter;


                        @Override
                        public void stateChanged(ChangeEvent e) {
                            counter++;
                            progressBarSearching.setString(counter + " files found");
                        }
                    });
            searchFinishedHandler(rootNode);
        });

    }

    /**
     * This will start the import of all file of all selected nodes in this
     * dialogs tree view. <br>
     * <br>
     * The import itself will run in a seperate thread, whilst this dialog will
     * be showing a progress bar, until the thread has finished its work. <br>
     * <br>
     * When the import has finished, the {@link #importFinishedHandler(java.util.List)} is
     * invoked.
     */
    private void startImport() {

        if (treeModel == null) {
            return;
        }
        setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);

        CheckableTreeNode root = (CheckableTreeNode) treeModel.getRoot();

        final List<File> fileList = getFileListFromNode(root);

        if ((fileList == null) || fileList.isEmpty()) {
            return;
        }

        progressBarImporting.setVisible(true);
        labelImportingInfo.setVisible(true);
        buttonApply.setVisible(false);
        buttonClose.setVisible(false);
        disOrEnableDialog(false);

        labelImportingInfo.setEnabled(true);

        progressBarImporting.setMinimum(0);
        progressBarImporting.setMaximum(fileList.size());
        progressBarImporting.setValue(0);
        progressBarImporting.setString("");

        final EntryType entryType = ((BibtexEntryTypeWrapper) comboBoxEntryTypeSelection.getSelectedItem())
                .getEntryType();

        threadState.set(true);
        JabRefExecutorService.INSTANCE.execute(() -> {
            List<String> errors = new LinkedList<>();
            creatorManager.addEntriesFromFiles(fileList, database, frame.getCurrentBasePanel(), entryType,
                    checkBoxWhyIsThereNoGetSelectedStupidSwing, new ChangeListener() {

                        int counter;


                        @Override
                        public void stateChanged(ChangeEvent e) {
                            counter++;
                            progressBarImporting.setValue(counter);
                            progressBarImporting.setString(counter + " of " + progressBarImporting.getMaximum());
                        }
                    }, errors);
            importFinishedHandler(errors);
        });
    }

    /**
     *
     * @param errors
     */
    private void importFinishedHandler(List<String> errors) {

        if ((errors != null) && !errors.isEmpty()) {

            JOptionPane.showMessageDialog(this,
                    "The import finished with warnings:\n" + "There " + (errors.size() > 1 ? "were " : "was ")
                            + errors.size() + (errors.size() > 1 ? " files" : " file")
                            + (errors.size() > 1 ? " which" : " that") + " could not be imported.",
                    Localization.lang("Warning"), JOptionPane.WARNING_MESSAGE);
        }

        progressBarImporting.setVisible(false);
        labelImportingInfo.setVisible(false);
        buttonApply.setVisible(true);
        buttonClose.setVisible(true);
        disOrEnableDialog(true);
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        frame.getCurrentBasePanel().markBaseChanged();
    }

    /**
     * Will be called from the Thread in which the "unlinked files search" is
     * processed. As the result of the search, the root node of the determined
     * file structure is passed.
     *
     * @param rootNode
     *            The root of the file structure as the result of the search.
     */
    private void searchFinishedHandler(CheckableTreeNode rootNode) {
        treeModel = new DefaultTreeModel(rootNode);
        tree.setModel(treeModel);
        tree.setRootVisible(rootNode.getChildCount() > 0);

        tree.invalidate();
        tree.repaint();

        progressBarSearching.setVisible(false);
        labelSearchingDirectoryInfo.setVisible(false);
        buttonScan.setVisible(true);
        actionSelectAll.actionPerformed(null);

        disOrEnableDialog(true);
        buttonApply.setEnabled(true);
    }

    /**
     * Sets up the actions for the components.
     */
    private void setupActions() {

        /**
         * Stores the selected Directory.
         */
        buttonBrowse.addActionListener(e -> {
            Path selectedDirectory = chooseDirectory();
            storeLastSelectedDirectory(selectedDirectory);
        });

        buttonScan.addActionListener(e -> startSearch());

        /**
         * Action for the button "Import...". <br>
         * <br>
         * Actions on this button will start the import of all file of all
         * selected nodes in this dialogs tree view. <br>
         */
        ActionListener actionListenerImportEntrys = e -> startImport();
        buttonApply.addActionListener(actionListenerImportEntrys);
        buttonClose.addActionListener(e -> dispose());
    }

    /**
     * Creates a list of {@link File}s for all leaf nodes in the tree structure
     * <code>node</code>, which have been marked as <i>selected</i>. <br>
     * <br>
     * <code>Selected</code> nodes correspond to those entries in the tree,
     * whose checkbox is <code>checked</code>.
     *
     * SIDE EFFECT: The checked nodes are removed from the tree.
     *
     * @param node
     *            The root node representing a tree structure.
     * @return A list of files of all checked leaf nodes.
     */
    private List<File> getFileListFromNode(CheckableTreeNode node) {
        List<File> filesList = new ArrayList<>();
        Enumeration<CheckableTreeNode> children = node.depthFirstEnumeration();
        List<CheckableTreeNode> nodesToRemove = new ArrayList<>();
        for (CheckableTreeNode child : Collections.list(children)) {
            if (child.isLeaf() && child.isSelected()) {
                File nodeFile = ((FileNodeWrapper) child.getUserObject()).file;
                if ((nodeFile != null) && nodeFile.isFile()) {
                    filesList.add(nodeFile);
                    nodesToRemove.add(child);
                }
            }
        }

        // remove imported files from tree
        DefaultTreeModel model = (DefaultTreeModel) tree.getModel();
        for (CheckableTreeNode nodeToRemove : nodesToRemove) {
            DefaultMutableTreeNode parent = (DefaultMutableTreeNode) nodeToRemove.getParent();
            model.removeNodeFromParent(nodeToRemove);

            // remove empty parent node
            while ((parent != null) && parent.isLeaf()) {
                DefaultMutableTreeNode pp = (DefaultMutableTreeNode) parent.getParent();
                if (pp != null) {
                    model.removeNodeFromParent(parent);
                }
                parent = pp;
            }
            // TODO: update counter / see: getTreeCellRendererComponent for label generation
        }
        tree.invalidate();
        tree.repaint();

        return filesList;
    }

    /**
     * Initializes the visible components in this dialog.
     */
    private void initComponents() {

        this.addComponentListener(dialogPositionListener);
        /* Interrupts the searchThread by setting the State-Array to 0 */
        this.addWindowListener(new WindowAdapter() {

            @Override
            public void windowClosing(WindowEvent e) {
                threadState.set(false);
            }
        });

        panelDirectory = new JPanel();
        panelSearchArea = new JPanel();
        panelFiles = new JPanel();
        panelOptions = new JPanel();
        panelEntryTypesSelection = new JPanel();
        panelButtons = new JPanel();
        panelImportArea = new JPanel();

        buttonBrowse = new JButton(Localization.lang("Browse"));
        buttonBrowse.setMnemonic('B');
        buttonBrowse.setToolTipText(Localization.lang("Opens the file browser."));
        buttonScan = new JButton(Localization.lang("Scan directory"));
        buttonScan.setMnemonic('S');
        buttonScan.setToolTipText(Localization.lang("Searches the selected directory for unlinked files."));
        buttonApply = new JButton(Localization.lang("Apply"));
        buttonApply.setMnemonic('I');
        buttonApply.setToolTipText(Localization.lang("Starts the import of BibTeX entries."));
        buttonClose = new JButton(Localization.lang("Close"));
        buttonClose.setToolTipText(Localization.lang("Leave this dialog."));
        buttonClose.setMnemonic('C');

        /* Options for the TreeView */
        buttonOptionSelectAll = new JButton();
        buttonOptionSelectAll.setMnemonic('A');
        buttonOptionSelectAll.setAction(actionSelectAll);
        buttonOptionUnselectAll = new JButton();
        buttonOptionUnselectAll.setMnemonic('U');
        buttonOptionUnselectAll.setAction(actionUnselectAll);
        buttonOptionExpandAll = new JButton();
        buttonOptionExpandAll.setMnemonic('E');
        buttonOptionExpandAll.setAction(actionExpandTree);
        buttonOptionCollapseAll = new JButton();
        buttonOptionCollapseAll.setMnemonic('L');
        buttonOptionCollapseAll.setAction(actionCollapseTree);

        checkboxCreateKeywords = new JCheckBox(Localization.lang("Create directory based keywords"));
        checkboxCreateKeywords
                .setToolTipText(Localization.lang("Creates keywords in created entrys with directory pathnames"));
        checkboxCreateKeywords.setSelected(checkBoxWhyIsThereNoGetSelectedStupidSwing);
        checkboxCreateKeywords.addItemListener(
                e -> checkBoxWhyIsThereNoGetSelectedStupidSwing = !checkBoxWhyIsThereNoGetSelectedStupidSwing);

        textfieldDirectoryPath = new JTextField();
        textfieldDirectoryPath
                .setText(lastSelectedDirectory == null ? "" : lastSelectedDirectory.toAbsolutePath().toString());

        labelDirectoryDescription = new JLabel(Localization.lang("Select a directory where the search shall start."));
        labelFileTypesDescription = new JLabel(Localization.lang("Select file type:"));
        labelFilesDescription = new JLabel(Localization.lang("These files are not linked in the active database."));
        labelEntryTypeDescription = new JLabel(Localization.lang("Entry type to be created:"));
        labelSearchingDirectoryInfo = new JLabel(Localization.lang("Searching file system..."));
        labelSearchingDirectoryInfo.setHorizontalAlignment(JTextField.CENTER);
        labelSearchingDirectoryInfo.setVisible(false);
        labelImportingInfo = new JLabel(Localization.lang("Importing into Database..."));
        labelImportingInfo.setHorizontalAlignment(JTextField.CENTER);
        labelImportingInfo.setVisible(false);

        tree = new JTree();
        scrollpaneTree = new JScrollPane(tree);
        scrollpaneTree.setWheelScrollingEnabled(true);

        progressBarSearching = new JProgressBar();
        progressBarSearching.setIndeterminate(true);
        progressBarSearching.setVisible(false);
        progressBarSearching.setStringPainted(true);

        progressBarImporting = new JProgressBar();
        progressBarImporting.setIndeterminate(false);
        progressBarImporting.setVisible(false);
        progressBarImporting.setStringPainted(true);

    }

    /**
     * Initializes the layout for the visible components in this menu. A
     * {@link GridBagLayout} is used.
     */
    private void initLayout() {

        GridBagLayout gbl = new GridBagLayout();

        panelDirectory.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(),
                Localization.lang("Select directory")));
        panelFiles.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(),
                Localization.lang("Select files")));
        panelEntryTypesSelection.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(),
                Localization.lang("BibTeX entry creation")));

        Insets basicInsets = new Insets(6, 6, 6, 6);
        Insets smallInsets = new Insets(3, 2, 3, 1);
        Insets noInsets = new Insets(0, 0, 0, 0);

        // 		x, y, w, h, wx,wy,ix,iy
        FindUnlinkedFilesDialog.addComponent(gbl, panelSearchArea, buttonScan, GridBagConstraints.HORIZONTAL,
                GridBagConstraints.EAST, noInsets, 0, 1, 1, 1, 1, 1, 40, 10);
        FindUnlinkedFilesDialog.addComponent(gbl, panelSearchArea, labelSearchingDirectoryInfo,
                GridBagConstraints.HORIZONTAL, GridBagConstraints.EAST, noInsets, 0, 2, 1, 1, 0, 0, 0, 0);
        FindUnlinkedFilesDialog.addComponent(gbl, panelSearchArea, progressBarSearching, GridBagConstraints.HORIZONTAL,
                GridBagConstraints.EAST, noInsets, 0, 3, 1, 1, 0, 0, 0, 0);

        FindUnlinkedFilesDialog.addComponent(gbl, panelDirectory, labelDirectoryDescription, null,
                GridBagConstraints.WEST, new Insets(6, 6, 0, 6), 0, 0, 3, 1, 0, 0, 0, 0);
        FindUnlinkedFilesDialog.addComponent(gbl, panelDirectory, textfieldDirectoryPath, GridBagConstraints.HORIZONTAL,
                null, basicInsets, 0, 1, 2, 1, 1, 1, 0, 0);
        FindUnlinkedFilesDialog.addComponent(gbl, panelDirectory, buttonBrowse, GridBagConstraints.HORIZONTAL,
                GridBagConstraints.EAST, basicInsets, 2, 1, 1, 1, 0, 0, 0, 0);
        FindUnlinkedFilesDialog.addComponent(gbl, panelDirectory, labelFileTypesDescription, GridBagConstraints.NONE,
                GridBagConstraints.WEST, new Insets(18, 6, 18, 3), 0, 3, 1, 1, 0, 0, 0, 0);
        FindUnlinkedFilesDialog.addComponent(gbl, panelDirectory, comboBoxFileTypeSelection,
                GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST, new Insets(18, 3, 18, 6), 1, 3, 1, 1, 1, 0, 0,
                0);
        FindUnlinkedFilesDialog.addComponent(gbl, panelDirectory, panelSearchArea, GridBagConstraints.HORIZONTAL,
                GridBagConstraints.EAST, new Insets(18, 6, 18, 6), 2, 3, 1, 1, 0, 0, 0, 0);

        FindUnlinkedFilesDialog.addComponent(gbl, panelFiles, labelFilesDescription, GridBagConstraints.HORIZONTAL,
                GridBagConstraints.WEST, new Insets(6, 6, 0, 6), 0, 0, 1, 1, 0, 0, 0, 0);
        FindUnlinkedFilesDialog.addComponent(gbl, panelFiles, scrollpaneTree, GridBagConstraints.BOTH,
                GridBagConstraints.CENTER, basicInsets, 0, 1, 1, 1, 1, 1, 0, 0);
        FindUnlinkedFilesDialog.addComponent(gbl, panelFiles, panelOptions, GridBagConstraints.NONE,
                GridBagConstraints.NORTHEAST, basicInsets, 1, 1, 1, 1, 0, 0, 0, 0);
        FindUnlinkedFilesDialog.addComponent(gbl, panelOptions, buttonOptionSelectAll, GridBagConstraints.HORIZONTAL,
                GridBagConstraints.NORTH, noInsets, 0, 0, 1, 1, 1, 0, 0, 0);
        FindUnlinkedFilesDialog.addComponent(gbl, panelOptions, buttonOptionUnselectAll, GridBagConstraints.HORIZONTAL,
                GridBagConstraints.NORTH, noInsets, 0, 1, 1, 1, 0, 0, 0, 0);
        FindUnlinkedFilesDialog.addComponent(gbl, panelOptions, buttonOptionExpandAll, GridBagConstraints.HORIZONTAL,
                GridBagConstraints.NORTH, new Insets(6, 0, 0, 0), 0, 2, 1, 1, 0, 0, 0, 0);
        FindUnlinkedFilesDialog.addComponent(gbl, panelOptions, buttonOptionCollapseAll, GridBagConstraints.HORIZONTAL,
                GridBagConstraints.NORTH, noInsets, 0, 3, 1, 1, 0, 0, 0, 0);

        FindUnlinkedFilesDialog.addComponent(gbl, panelEntryTypesSelection, labelEntryTypeDescription,
                GridBagConstraints.NONE, GridBagConstraints.WEST, basicInsets, 0, 0, 1, 1, 0, 0, 0, 0);
        FindUnlinkedFilesDialog.addComponent(gbl, panelEntryTypesSelection, comboBoxEntryTypeSelection,
                GridBagConstraints.NONE, GridBagConstraints.WEST, basicInsets, 1, 0, 1, 1, 1, 0, 0, 0);
        FindUnlinkedFilesDialog.addComponent(gbl, panelEntryTypesSelection, checkboxCreateKeywords,
                GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST, basicInsets, 0, 1, 2, 1, 0, 0, 0, 0);
        FindUnlinkedFilesDialog.addComponent(gbl, panelImportArea, labelImportingInfo, GridBagConstraints.HORIZONTAL,
                GridBagConstraints.CENTER, new Insets(6, 6, 0, 6), 0, 1, 1, 1, 1, 0, 0, 0);
        FindUnlinkedFilesDialog.addComponent(gbl, panelImportArea, progressBarImporting, GridBagConstraints.HORIZONTAL,
                GridBagConstraints.CENTER, new Insets(0, 6, 6, 6), 0, 2, 1, 1, 1, 0, 0, 0);
        FindUnlinkedFilesDialog.addComponent(gbl, panelButtons, panelImportArea, GridBagConstraints.NONE,
                GridBagConstraints.EAST, smallInsets, 1, 0, 1, 1, 0, 0, 0, 0);

        FindUnlinkedFilesDialog.addComponent(gbl, getContentPane(), panelDirectory, GridBagConstraints.HORIZONTAL,
                GridBagConstraints.CENTER, basicInsets, 0, 0, 1, 1, 0, 0, 0, 0);
        FindUnlinkedFilesDialog.addComponent(gbl, getContentPane(), panelFiles, GridBagConstraints.BOTH,
                GridBagConstraints.NORTHWEST, new Insets(12, 6, 2, 2), 0, 1, 1, 1, 1, 1, 0, 0);
        FindUnlinkedFilesDialog.addComponent(gbl, getContentPane(), panelEntryTypesSelection,
                GridBagConstraints.HORIZONTAL, GridBagConstraints.SOUTHWEST, new Insets(12, 6, 2, 2), 0, 2, 1, 1, 0, 0,
                0, 0);
        FindUnlinkedFilesDialog.addComponent(gbl, getContentPane(), panelButtons, GridBagConstraints.HORIZONTAL,
                GridBagConstraints.CENTER, new Insets(10, 6, 10, 6), 0, 3, 1, 1, 0, 0, 0, 0);

        ButtonBarBuilder bb = new ButtonBarBuilder();
        bb.addGlue();
        bb.addButton(buttonApply);
        bb.addButton(buttonClose);
        bb.addGlue();

        bb.getPanel().setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        panelImportArea.add(bb.getPanel(), GridBagConstraints.NONE);
        pack();

    }

    /**
     * Adds a component to a container, using the specified gridbag-layout and
     * the supplied parameters. <br>
     * <br>
     * This method is simply used to ged rid of thousands of lines of code,
     * which inevitably rise when layouts such as the gridbag-layout is being
     * used.
     *
     * @param layout
     *            The layout to be used.
     * @param container
     *            The {@link Container}, to which the component will be added.
     * @param component
     *            An AWT {@link Component}, that will be added to the container.
     * @param fill
     *            A constant describing the fill behaviour (see
     *            {@link GridBagConstraints}). Can be <code>null</code>, if no
     *            filling wants to be specified.
     * @param anchor
     *            A constant describing the anchor of the element in its parent
     *            container (see {@link GridBagConstraints}). Can be
     *            <code>null</code>, if no specification is needed.
     * @param gridX
     *            The relative grid-X coordinate.
     * @param gridY
     *            The relative grid-Y coordinate.
     * @param width
     *            The relative width of the component.
     * @param height
     *            The relative height of the component.
     * @param weightX
     *            A value for the horizontal weight.
     * @param weightY
     *            A value for the vertical weight.
     * @param insets
     *            Insets of the component. Can be <code>null</code>.
     */
    private static void addComponent(GridBagLayout layout, Container container, Component component, Integer fill,
            Integer anchor, Insets insets, int gridX, int gridY, int width, int height, double weightX, double weightY,
            int ipadX, int ipadY) {
        container.setLayout(layout);
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.gridx = gridX;
        constraints.gridy = gridY;
        constraints.gridwidth = width;
        constraints.gridheight = height;
        constraints.weightx = weightX;
        constraints.weighty = weightY;
        constraints.ipadx = ipadX;
        constraints.ipady = ipadY;
        if (fill != null) {
            constraints.fill = fill;
        }
        if (insets != null) {
            constraints.insets = insets;
        }
        if (anchor != null) {
            constraints.anchor = anchor;
        }
        layout.setConstraints(component, constraints);
        container.add(component);
    }

    /**
     * Creates the tree view, that holds the data structure. <br>
     * <br>
     * Initially, the root node is <b>not</b> visible, so that the tree appears empty at the beginning.
     */
    private void createTree() {

        /**
         * Mouse listener to listen for mouse events on the tree. <br>
         * This will mark the selected tree entry as "selected" or "unselected",
         * which will cause this nodes checkbox to appear as either "checked" or
         * "unchecked".
         */
        treeMouseListener = new MouseAdapter() {

            @Override
            public void mousePressed(MouseEvent e) {
                int x = e.getX();
                int y = e.getY();

                int row = tree.getRowForLocation(x, y);

                TreePath path = tree.getPathForRow(row);
                if (path != null) {
                    CheckableTreeNode node = (CheckableTreeNode) path.getLastPathComponent();
                    if (e.getClickCount() == 2) {
                        Object userObject = node.getUserObject();
                        if ((userObject instanceof FileNodeWrapper) && node.isLeaf()) {
                            FileNodeWrapper fnw = (FileNodeWrapper) userObject;
                            try {
                                JabRefDesktop.openExternalViewer(
                                        JabRefGUI.getMainFrame().getCurrentBasePanel().getBibDatabaseContext(),
                                        fnw.file.getAbsolutePath(), "pdf");
                            } catch (IOException e1) {
                                LOGGER.info("Error opening file", e1);
                            }
                        }
                    } else {
                        node.check();
                        tree.invalidate();
                        tree.repaint();
                    }
                }
            }

        };

        CheckableTreeNode startNode = new CheckableTreeNode("ROOT");
        DefaultTreeModel model = new DefaultTreeModel(startNode);

        tree.setModel(model);
        tree.setRootVisible(false);

        DefaultTreeCellRenderer renderer = new CheckboxTreeCellRenderer();
        tree.setCellRenderer(renderer);

        tree.addMouseListener(treeMouseListener);

    }

    /**
     * Initialises the combobox that contains the available file types which
     * bibtex entries can be created of.
     */
    private void createFileTypesCombobox() {

        List<FileFilter> fileFilterList = creatorManager.getFileFilterList();

        Vector<FileFilter> vector = new Vector<>();
        for (FileFilter fileFilter : fileFilterList) {
            vector.add(fileFilter);
        }
        comboBoxFileTypeSelection = new JComboBox<>(vector);

        comboBoxFileTypeSelection.setRenderer(new DefaultListCellRenderer() {

            /* (non-Javadoc)
             * @see javax.swing.DefaultListCellRenderer#getListCellRendererComponent(javax.swing.JList, java.lang.Object, int, boolean, boolean)
             */
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected,
                    boolean cellHasFocus) {
                JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected,
                        cellHasFocus);
                if (value instanceof EntryFromFileCreator) {
                    EntryFromFileCreator creator = (EntryFromFileCreator) value;
                    if (creator.getExternalFileType() != null) {
                        label.setIcon(creator.getExternalFileType().getIcon());
                    }
                }
                return label;
            }
        });

    }

    /**
     * Creates the ComboBox-View for the Listbox that holds the Bibtex entry
     * types.
     */
    private void createEntryTypesCombobox() {

        Iterator<EntryType> iterator = EntryTypes
                .getAllValues(frame.getCurrentBasePanel().getBibDatabaseContext().getMode()).iterator();
        Vector<BibtexEntryTypeWrapper> list = new Vector<>();
        list.add(new BibtexEntryTypeWrapper(null));
        while (iterator.hasNext()) {
            list.add(new BibtexEntryTypeWrapper(iterator.next()));
        }
        comboBoxEntryTypeSelection = new JComboBox<>(list);
    }


    /**
     * Wrapper for displaying the Type {@link BibtexEntryType} in a Combobox.
     *
     * @author Nosh&Dan
     * @version 12.11.2008 | 01:02:30
     *
     */
    private static class BibtexEntryTypeWrapper {

        private final EntryType entryType;


        BibtexEntryTypeWrapper(EntryType bibtexType) {
            this.entryType = bibtexType;
        }

        @Override
        public String toString() {
            if (entryType == null) {
                return Localization.lang("<No selection>");
            }
            return entryType.getName();
        }

        public EntryType getEntryType() {
            return entryType;
        }
    }

    public static class CheckableTreeNode extends DefaultMutableTreeNode {

        private boolean isSelected;
        private final JCheckBox checkbox;


        public CheckableTreeNode(Object userObject) {
            super(userObject);
            checkbox = new JCheckBox();
        }

        /**
         * @return the checkbox
         */
        public JCheckBox getCheckbox() {
            return checkbox;
        }

        public void check() {
            setSelected(!isSelected);
        }

        public void setSelected(boolean bSelected) {
            isSelected = bSelected;
            Enumeration<CheckableTreeNode> tmpChildren = this.children();
            for (CheckableTreeNode child : Collections.list(tmpChildren)) {
                child.setSelected(bSelected);
            }

        }

        public boolean isSelected() {
            return isSelected;
        }

    }

    private static class CheckboxTreeCellRenderer extends DefaultTreeCellRenderer {

        private final FileSystemView fsv = FileSystemView.getFileSystemView();


        @Override
        public Component getTreeCellRendererComponent(final JTree tree, Object value, boolean sel, boolean expanded,
                boolean leaf, int row, boolean hasFocus) {

            Component nodeComponent = super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row,
                    hasFocus);
            CheckableTreeNode node = (CheckableTreeNode) value;

            FileNodeWrapper userObject = (FileNodeWrapper) node.getUserObject();

            JPanel newPanel = new JPanel();

            JCheckBox checkbox = node.getCheckbox();
            checkbox.setSelected(node.isSelected());

            try {
                setIcon(fsv.getSystemIcon(userObject.file));
            } catch (Exception ignored) {
                // Ignored
            }

            newPanel.setBackground(nodeComponent.getBackground());
            checkbox.setBackground(nodeComponent.getBackground());

            GridBagLayout gbl = new GridBagLayout();
            FindUnlinkedFilesDialog.addComponent(gbl, newPanel, checkbox, null, null, null, 0, 0, 1, 1, 0, 0, 0, 0);
            FindUnlinkedFilesDialog.addComponent(gbl, newPanel, nodeComponent, GridBagConstraints.HORIZONTAL, null,
                    new Insets(1, 2, 0, 0), 1, 0, 1, 1, 1, 0, 0, 0);

            if (userObject.fileCount > 0) {
                JLabel label = new JLabel(
                        "(" + userObject.fileCount + " file" + (userObject.fileCount > 1 ? "s" : "") + ")");
                FindUnlinkedFilesDialog.addComponent(gbl, newPanel, label, null, null, new Insets(1, 2, 0, 0), 2, 0, 1,
                        1, 0, 0, 0, 0);
            }
            return newPanel;
        }

    }

    public static class FileNodeWrapper {

        public final File file;
        public final int fileCount;


        public FileNodeWrapper(File aFile) {
            this(aFile, 0);
        }

        /**
         * @param aDirectory
         * @param fileCount
         */
        public FileNodeWrapper(File aDirectory, int fileCount) {
            this.file = aDirectory;
            this.fileCount = fileCount;
        }

        /*
         * (non-Javadoc)
         *
         * @see java.lang.Object#toString()
         */
        @Override
        public String toString() {
            return file.getName();
        }
    }

}
